package com.example.randomchar.config;

import com.example.randomchar.Repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${spring.security.user.name}")
    private String adminUsername;

    @Value("${spring.security.user.password}")
    private String adminPassword;

    @Value("${app.cookie.secure:false}")
    private boolean isSecureCookie;

    private final UserRepository userRepository;
    private final RateLimitFilter rateLimitFilter;
    private final VisitorTrackingFilter visitorTrackingFilter;
    private final JwtUtils jwtUtils;
    private final JwtAuthenticationFilter jwtAuthenticationFilter; // Inject Filter JWT

    public SecurityConfig(UserRepository userRepository, RateLimitFilter rateLimitFilter,
                          VisitorTrackingFilter visitorTrackingFilter, JwtUtils jwtUtils,
                          JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.userRepository = userRepository;
        this.rateLimitFilter = rateLimitFilter;
        this.visitorTrackingFilter = visitorTrackingFilter;
        this.jwtUtils = jwtUtils;
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService databaseUserDetailsService() {
        return username -> userRepository.findByUsername(username)
                .map(user -> User.builder()
                        .username(user.getUsername())
                        .password(user.getPassword())
                        .disabled(!user.isEnabled())
                        .roles("USER")
                        .build())
                .orElseThrow(() -> new org.springframework.security.core.userdetails.UsernameNotFoundException("User not found"));
    }

    @Bean
    public UserDetailsService adminUserDetailsService() {
        var admin = User.builder()
                .username(adminUsername)
                .password(passwordEncoder().encode(adminPassword))
                .roles("ADMIN")
                .build();
        return new InMemoryUserDetailsManager(admin);
    }

    @Bean
    public AuthenticationManager authenticationManager() {
        DaoAuthenticationProvider dbProvider = new DaoAuthenticationProvider(databaseUserDetailsService());
        dbProvider.setPasswordEncoder(passwordEncoder());

        DaoAuthenticationProvider adminProvider = new DaoAuthenticationProvider(adminUserDetailsService());
        adminProvider.setPasswordEncoder(passwordEncoder());

        return new ProviderManager(dbProvider, adminProvider);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, AuthenticationManager authenticationManager) throws Exception {
        http
                .authenticationManager(authenticationManager)
                .csrf(csrf -> csrf.disable())
                // TẮT SESSION: Chuyển sang trạng thái STATELESS (Không lưu session trên server)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // Thêm các bộ lọc
                .addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(visitorTrackingFilter, RateLimitFilter.class)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class) // Thêm JWT Filter

                .exceptionHandling(exception -> exception
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.sendRedirect("/index.html?error=forbidden");
                        })
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/index.html", "/login.html", "/error", "/css/**", "/js/**", "/images/**").permitAll()
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/actors/**").permitAll()
                        .requestMatchers("/api/history/**").permitAll()
                        .requestMatchers("/api/favorites/**").authenticated()
                        .requestMatchers("/dashboard.html").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/actors/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/actors/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/actors/**").hasRole("ADMIN")
                        .anyRequest().permitAll()
                )
                .formLogin(form -> form
                        .loginPage("/login.html")
                        .loginProcessingUrl("/login")
                        .successHandler((request, response, authentication) -> {
                            // 1. Tạo JWT
                            String token = jwtUtils.generateJwtToken(authentication);

                            // 2. Lưu JWT vào Cookie (HttpOnly để chống XSS)
                            ResponseCookie jwtCookie = ResponseCookie.from("jwt_token", token)
                                    .httpOnly(true)
                                    .secure(isSecureCookie) // SỬA LỖI: Dùng biến isSecureCookie
                                    .path("/")
                                    .maxAge(7 * 24 * 60 * 60) // 7 ngày
                                    .sameSite("Lax")
                                    .build();
                            response.addHeader("Set-Cookie", jwtCookie.toString());

                            // 3. Điều hướng
                            boolean isAdmin = authentication.getAuthorities().stream()
                                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
                            if (isAdmin) {
                                response.sendRedirect("/dashboard.html");
                            } else {
                                response.sendRedirect("/index.html");
                            }
                        })
                        .failureUrl("/login.html?error=true")
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutSuccessHandler((request, response, authentication) -> {
                            // Xóa Cookie khi Đăng xuất
                            ResponseCookie cleanCookie = ResponseCookie.from("jwt_token", "")
                                    .httpOnly(true)
                                    .secure(isSecureCookie) // SỬA LỖI: Dùng biến isSecureCookie
                                    .path("/")
                                    .maxAge(0)
                                    .build();
                            response.addHeader("Set-Cookie", cleanCookie.toString());

                            String redirect = request.getParameter("redirect");
                            if ("login".equals(redirect)) {
                                response.sendRedirect("/login.html");
                            } else {
                                response.sendRedirect("/index.html");
                            }
                        })
                        .permitAll()
                );

        return http.build();
    }
}