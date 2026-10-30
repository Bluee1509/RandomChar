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
        // SỬA LẠI: Chỉ trim(), giữ nguyên chữ hoa/thường từ biến môi trường
        String normalizedAdminUsername = adminUsername.trim();

        Optional<User> adminOpt = userRepository.findByUsername(normalizedAdminUsername);

        if (adminOpt.isEmpty()) {
            User admin = User.builder()
                    .username(normalizedAdminUsername)
                    .password(passwordEncoder.encode(adminPassword))
                    .email(normalizedAdminUsername + "@randomchar.com")
                    .enabled(true)
                    .build();
            userRepository.save(admin);
            System.out.println("✅ Đã tạo tài khoản Admin trong Database: " + normalizedAdminUsername);
        } else {
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