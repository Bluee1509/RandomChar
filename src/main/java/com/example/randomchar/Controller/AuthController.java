package com.example.randomchar.Controller;

import com.example.randomchar.Entity.User;
import com.example.randomchar.Repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

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
    private final JavaMailSender mailSender;

    // Tiêm tên Admin hệ thống từ file cấu hình (Environment Variable) vào đây
    @Value("${spring.security.user.name}")
    private String systemAdminUsername;

    public AuthController(UserRepository userRepository, PasswordEncoder passwordEncoder, JavaMailSender mailSender) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.mailSender = mailSender;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> request) {
        String username = request.get("username");
        String password = request.get("password");
        String email = request.get("email");

        // ========================================================
        // BƯỚC 0: BẢO MẬT - CHẶN CÁC TÊN TÀI KHOẢN HỆ THỐNG
        // ========================================================
        // 1. Chặn nếu trùng với Admin hệ thống (lấy từ Env)
        if (systemAdminUsername.equalsIgnoreCase(username)) {
            return ResponseEntity.badRequest().body(Map.of("message", "Tên đăng nhập này là của hệ thống, vui lòng chọn tên khác!"));
        }

        // 2. Chặn các từ khóa nhạy cảm khác (Hardcode)
        List<String> reservedWords = List.of("root", "administrator", "system", "support", "mod");
        if (reservedWords.contains(username.toLowerCase())) {
            return ResponseEntity.badRequest().body(Map.of("message", "Tên đăng nhập này không được phép sử dụng!"));
        }

        // ========================================================
        // BƯỚC 1: KIỂM TRA DATABASE
        // ========================================================
        Optional<User> existingUserOpt = userRepository.findByUsername(username);
        if (existingUserOpt.isPresent()) {
            User existingUser = existingUserOpt.get();
            if (existingUser.isEnabled()) {
                return ResponseEntity.badRequest().body(Map.of("message", "Tên đăng nhập đã được sử dụng và kích hoạt!"));
            } else {
                // Nếu chưa kích hoạt, xóa đi để cho phép đăng ký lại
                userRepository.delete(existingUser);
            }
        }

        Optional<User> existingEmailOpt = userRepository.findByEmail(email);
        if (existingEmailOpt.isPresent()) {
            User existingUser = existingEmailOpt.get();
            if (existingUser.isEnabled()) {
                return ResponseEntity.badRequest().body(Map.of("message", "Email đã được sử dụng!"));
            } else {
                // Nếu chưa kích hoạt, xóa đi
                userRepository.delete(existingUser);
            }
        }

        // ========================================================
        // BƯỚC 2: TẠO USER VÀ GỬI OTP
        // ========================================================
        String otp = String.format("%06d", new Random().nextInt(999999));

        User user = User.builder()
                .username(username)
                .password(passwordEncoder.encode(password))
                .email(email)
                .otpCode(otp)
                // SET THỜI GIAN HẾT HẠN LÀ 5 PHÚT
                .otpExpiryTime(LocalDateTime.now().plusMinutes(5))
                .enabled(false)
                .build();

        userRepository.save(user);

        // Gửi Email
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(email);
            message.setSubject("Xác thực tài khoản RANDOM CHAR");
            message.setText("Chào " + username + ",\n\nMã xác thực (OTP) của bạn là: " + otp + "\n\nMã này có hiệu lực trong 5 phút.\n\nVui lòng nhập mã này để hoàn tất đăng ký.");
            mailSender.send(message);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of("message", "Không thể gửi email. Vui lòng kiểm tra lại cấu hình SMTP!"));
        }

        return ResponseEntity.ok(Map.of("message", "Đăng ký thành công! Vui lòng kiểm tra email để lấy mã OTP."));
    }

    @PostMapping("/verify")
    public ResponseEntity<?> verifyOtp(@RequestBody Map<String, String> request) {
        String username = request.get("username");
        String otp = request.get("otp");

        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) return ResponseEntity.badRequest().body(Map.of("message", "User không tồn tại!"));

        // 1. KIỂM TRA THỜI GIAN HẾT HẠN TRƯỚC
        if (user.getOtpExpiryTime() != null && user.getOtpExpiryTime().isBefore(LocalDateTime.now())) {
            return ResponseEntity.badRequest().body(Map.of("message", "Mã OTP đã hết hạn (quá 5 phút). Vui lòng bấm Đăng ký lại để nhận mã mới!"));
        }

        // 2. KIỂM TRA MÃ OTP CÓ ĐÚNG KHÔNG
        if (user.getOtpCode() != null && user.getOtpCode().equals(otp)) {
            user.setEnabled(true);
            user.setOtpCode(null); // Xóa mã OTP sau khi dùng thành công
            user.setOtpExpiryTime(null); // Xóa thời gian hết hạn
            userRepository.save(user);
            return ResponseEntity.ok(Map.of("message", "Xác thực thành công! Bạn có thể đăng nhập."));
        }

        return ResponseEntity.badRequest().body(Map.of("message", "Mã OTP không chính xác!"));
    }

    // API Lấy thông tin User hiện tại (Dùng cho hiển thị tên trên Header)
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(org.springframework.security.core.Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || authentication.getName().equals("anonymousUser")) {
            return ResponseEntity.status(401).body(Map.of("message", "Chưa đăng nhập"));
        }
        return ResponseEntity.ok(Map.of("username", authentication.getName()));
    }
}