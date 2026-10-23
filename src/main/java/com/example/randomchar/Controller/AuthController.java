package com.example.randomchar.Controller;

import com.example.randomchar.Entity.User;
import com.example.randomchar.Repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RestClient brevoClient;
    private final String brevoSenderEmail;
    private final String brevoSenderName;

    @Value("${spring.security.user.name}")
    private String systemAdminUsername;

    public AuthController(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${brevo.api.key}") String brevoApiKey,
            @Value("${brevo.sender.email}") String brevoSenderEmail,
            @Value("${brevo.sender.name}") String brevoSenderName) {

        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.brevoSenderEmail = brevoSenderEmail;
        this.brevoSenderName = brevoSenderName;

        // Client gọi Brevo API v3
        this.brevoClient = RestClient.builder()
                .baseUrl("https://api.brevo.com/v3")
                .defaultHeader("api-key", brevoApiKey)
                .defaultHeader("Accept", MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .build();

    }

    // ========================================================
    // ĐĂNG KÝ
    // ========================================================
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> request) {
        String username = request.get("username");
        String password = request.get("password");
        String email = request.get("email");

        // BƯỚC 0: CHẶN TÊN HỆ THỐNG
        if (systemAdminUsername.equalsIgnoreCase(username)) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Tên đăng nhập này là của hệ thống, vui lòng chọn tên khác!"));
        }
        List<String> reservedWords = List.of("root", "administrator", "system", "support", "mod");
        if (reservedWords.contains(username.toLowerCase())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Tên đăng nhập này không được phép sử dụng!"));
        }

        // BƯỚC 1: KIỂM TRA DATABASE
        Optional<User> existingUserOpt = userRepository.findByUsername(username);
        if (existingUserOpt.isPresent()) {
            User existingUser = existingUserOpt.get();
            if (existingUser.isEnabled()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "Tên đăng nhập đã được sử dụng và kích hoạt!"));
            } else {
                userRepository.delete(existingUser);
            }
        }
        Optional<User> existingEmailOpt = userRepository.findByEmail(email);
        if (existingEmailOpt.isPresent()) {
            User existingUser = existingEmailOpt.get();
            if (existingUser.isEnabled()) {
                return ResponseEntity.badRequest().body(Map.of("message", "Email đã được sử dụng!"));
            } else {
                userRepository.delete(existingUser);
            }
        }

        // BƯỚC 2: TẠO USER + OTP
        String otp = String.format("%06d", new Random().nextInt(999999));

        User user = User.builder()
                .username(username)
                .password(passwordEncoder.encode(password))
                .email(email)
                .otpCode(otp)
                .otpExpiryTime(LocalDateTime.now().plusMinutes(5))
                .enabled(false)
                .build();
        userRepository.save(user);

        // BƯỚC 3: GỬI EMAIL QUA BREVO HTTP API
        try {
            sendOtpEmail(email, username, otp);
            System.out.println("✅ Email OTP đã được gửi thành công qua Brevo API.");
        } catch (RestClientResponseException ex) {
            // Brevo trả 4xx/5xx -> log body để debug
            System.err.println("❌ Brevo API error: " + ex.getStatusCode()
                    + " - " + ex.getResponseBodyAsString());
            userRepository.delete(user); // rollback user để user có thể đăng ký lại
            return ResponseEntity.internalServerError()
                    .body(Map.of("message", "Không thể gửi email xác thực. Vui lòng thử lại sau!"));
        } catch (Exception ex) {
            ex.printStackTrace();
            userRepository.delete(user);
            return ResponseEntity.internalServerError()
                    .body(Map.of("message", "Không thể gửi email xác thực. Vui lòng thử lại sau!"));
        }

        return ResponseEntity.ok(Map.of("message",
                "Đăng ký thành công! Vui lòng kiểm tra email để lấy mã OTP."));
    }

    // ========================================================
    // HÀM GỬI EMAIL QUA BREVO API
    // ========================================================
    private void sendOtpEmail(String toEmail, String username, String otp) {

        String htmlContent = "<html><body style=\"font-family:Arial,sans-serif;color:#222;\">"
                + "<h2 style=\"color:#ff3366;\">Xác thực tài khoản RANDOM CHAR</h2>"
                + "<p>Chào <b>" + username + "</b>,</p>"
                + "<p>Mã xác thực (OTP) của bạn là:</p>"
                + "<p style=\"font-size:32px;font-weight:bold;letter-spacing:8px;"
                + "color:#ff3366;background:#f5f5f5;padding:12px 20px;display:inline-block;"
                + "border-radius:6px;\">" + otp + "</p>"
                + "<p>Mã này có hiệu lực trong <b>5 phút</b>.</p>"
                + "<p>Vui lòng nhập mã này để hoàn tất đăng ký.</p>"
                + "<hr><p style=\"font-size:12px;color:#888;\">Nếu bạn không thực hiện đăng ký, "
                + "hãy bỏ qua email này.</p>"
                + "</body></html>";

        Map<String, Object> payload = Map.of(
                "sender", Map.of(
                        "name", brevoSenderName,   // đã set ở constructor
                        "email", brevoSenderEmail   // phải là sender đã verify trên Brevo
                ),
                "to", List.of(Map.of(
                        "email", toEmail,
                        "name", username
                )),
                "subject", "Xác thực tài khoản RANDOM CHAR",
                "htmlContent", htmlContent
        );

        brevoClient.post()
                .uri("/smtp/email")
                .body(payload)
                .retrieve()
                .toBodilessEntity();   // ném exception nếu 4xx/5xx
    }

    // ========================================================
    // CÁC HÀM CÒN LẠI GIỮ NGUYÊN
    // ========================================================
    @PostMapping("/verify")
    public ResponseEntity<?> verifyOtp(@RequestBody Map<String, String> request) {
        String username = request.get("username");
        String otp = request.get("otp");

        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null)
            return ResponseEntity.badRequest().body(Map.of("message", "User không tồn tại!"));

        if (user.getOtpExpiryTime() == null || user.getOtpExpiryTime().isBefore(LocalDateTime.now())) {
            return ResponseEntity.badRequest().body(Map.of("message",
                    "Mã OTP đã hết hạn (quá 5 phút). Vui lòng bấm Đăng ký lại để nhận mã mới!"));
        }

        if (user.getOtpCode() != null && user.getOtpCode().equals(otp)) {
            user.setEnabled(true);
            user.setOtpCode(null);
            user.setOtpExpiryTime(null);
            userRepository.save(user);
            return ResponseEntity.ok(Map.of("message", "Xác thực thành công! Bạn có thể đăng nhập."));
        }
        return ResponseEntity.badRequest().body(Map.of("message", "Mã OTP không chính xác!"));
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(org.springframework.security.core.Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || authentication.getName().equals("anonymousUser")) {
            return ResponseEntity.status(401).body(Map.of("message", "Chưa đăng nhập"));
        }
        return ResponseEntity.ok(Map.of("username", authentication.getName()));
    }
}