package com.example.randomchar.config; // Đổi lại đúng package của dự án bạn

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        // 1. Cho phép truy cập trang chủ, tài nguyên tĩnh và trang login
                        .requestMatchers("/", "/index.html", "/login.html", "/css/**", "/js/**", "/images/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/actors/**").permitAll()

                        // 2. Dashboard và các hành động Thêm/Xóa bắt buộc phải đăng nhập
                        .requestMatchers("/dashboard.html").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/actors/**").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/actors/**").authenticated()

                        .anyRequest().permitAll()
                )
                // 3. Cấu hình trang login tùy chỉnh
                .formLogin(form -> form
                        .loginPage("/login.html")              // Đường dẫn đến trang giao diện đăng nhập tự tạo
                        .loginProcessingUrl("/login")          // URL xử lý xác thực form
                        .defaultSuccessUrl("/dashboard.html", true) // Đăng nhập thành công thì tự chuyển vào Dashboard
                        .failureUrl("/login.html?error=true")  // Đăng nhập sai thì ở lại trang login và báo lỗi
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutSuccessUrl("/index.html")       // Đăng xuất xong thì quay về trang chủ
                        .permitAll()
                );

        return http.build();
    }
}