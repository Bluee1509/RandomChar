package com.example.randomchar.config;

import com.example.randomchar.Entity.User;
import com.example.randomchar.Repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${spring.security.user.name}")
    private String adminUsername;

    @Value("${spring.security.user.password}")
    private String adminPassword;

    public DataInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
        Optional<User> adminOpt = userRepository.findByUsername(adminUsername);

        if (adminOpt.isEmpty()) {
            // Nếu Admin chưa tồn tại -> Tạo mới
            User admin = User.builder()
                    .username(adminUsername)
                    .password(passwordEncoder.encode(adminPassword)) // Phải mã hóa BCrypt
                    .email(adminUsername + "@randomchar.com") // Email fake vì trong DB bắt buộc có email
                    .enabled(true) // Kích hoạt tài khoản
                    .build();
            userRepository.save(admin);
            System.out.println("✅ Đã tạo tài khoản Admin mới trong Database: " + adminUsername);
        } else {
            // Nếu Admin đã tồn tại -> Kiểm tra xem mật khẩu trên Render có bị đổi không
            User existingAdmin = adminOpt.get();
            if (!passwordEncoder.matches(adminPassword, existingAdmin.getPassword())) {
                existingAdmin.setPassword(passwordEncoder.encode(adminPassword));
                userRepository.save(existingAdmin);
                System.out.println("🔄 Đã cập nhật lại mật khẩu Admin từ Environment Variable!");
            } else {
                System.out.println("ℹ️ Tài khoản Admin đã tồn tại trong Database.");
            }
        }
    }
}