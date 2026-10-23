package com.example.randomchar.config;

import com.example.randomchar.Repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    // 1. Lấy tài khoản Admin từ Environment Variables
    @Value("${spring.security.user.name}")
    private String adminUsername;

    @Value("${spring.security.user.password}")
    private String adminPassword;

    private final UserRepository userRepository;

    public SecurityConfig(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // 2. Khai báo PasswordEncoder
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // 3. Lấy User từ Database (dành cho người dùng đăng ký)
    @Bean
    public UserDetailsService databaseUserDetailsService() {
        return username -> userRepository.findByUsername(username)
                .map(user -> User.builder()
                        .username(user.getUsername())
                        .password(user.getPassword())
                        .disabled(!user.isEnabled()) // Chặn nếu chưa xác thực OTP
                        .roles("USER")
                        .build())
                .orElseThrow(() -> new org.springframework.security.core.userdetails.UsernameNotFoundException("User not found"));
    }

    // 4. Lấy Admin từ RAM (từ biến môi trường)
    @Bean
    public UserDetailsService adminUserDetailsService() {
        var admin = User.builder()
                .username(adminUsername)
                .password(passwordEncoder().encode(adminPassword))
                .roles("ADMIN")
                .build();
        return new InMemoryUserDetailsManager(admin);
    }

    // 5. Gộp 2 nguồn User lại
    @Bean
    public AuthenticationManager authenticationManager() {
        DaoAuthenticationProvider dbProvider = new DaoAuthenticationProvider(databaseUserDetailsService());
        dbProvider.setPasswordEncoder(passwordEncoder());

        DaoAuthenticationProvider adminProvider = new DaoAuthenticationProvider(adminUserDetailsService());
        adminProvider.setPasswordEncoder(passwordEncoder());

        return new ProviderManager(dbProvider, adminProvider);
    }

    // 6. Cấu hình Phân quyền (Authorization)
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, AuthenticationManager authenticationManager) throws Exception {
        http
                .authenticationManager(authenticationManager)
                .csrf(csrf -> csrf.disable())

                // XỬ LÝ LỖI 403: Nếu User cố vào trang Admin -> Đá về trang chủ kèm thông báo
                .exceptionHandling(exception -> exception
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.sendRedirect("/index.html?error=forbidden");
                        })
                )

                .authorizeHttpRequests(auth -> auth
                        // Cho phép truy cập công khai các trang tĩnh và API cơ bản
                        .requestMatchers("/", "/index.html", "/login.html", "/error", "/css/**", "/js/**", "/images/**").permitAll()
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/actors/**").permitAll()

                        // CHỈ ADMIN MỚI ĐƯỢC VÀO DASHBOARD VÀ THÊM/SỬA/XÓA
                        .requestMatchers("/dashboard.html").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/actors/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/actors/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/actors/**").hasRole("ADMIN")

                        .anyRequest().permitAll()
                )
                // Cấu hình trang Login tùy chỉnh
                .formLogin(form -> form
                        .loginPage("/login.html")
                        .loginProcessingUrl("/login")
                        .successHandler((request, response, authentication) -> {
                            boolean isAdmin = authentication.getAuthorities().stream()
                                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

                            if (isAdmin) {
                                response.sendRedirect("/dashboard.html"); // Admin vào Dashboard
                            } else {
                                response.sendRedirect("/index.html"); // User thường ra Trang chủ
                            }
                        })
                        .failureUrl("/login.html?error=true")
                        .permitAll()
                )
                // Cấu hình Đăng xuất
                .logout(logout -> logout
                        .logoutSuccessHandler((request, response, authentication) -> {
                            String redirect = request.getParameter("redirect");
                            if ("login".equals(redirect)) {
                                response.sendRedirect("/login.html"); // Từ Dashboard -> Về Login
                            } else {
                                response.sendRedirect("/index.html"); // Từ Trang chủ -> Về Index
                            }
                        })
                        .permitAll()
                );

        return http.build();
    }
}