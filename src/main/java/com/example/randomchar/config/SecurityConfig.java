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
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${spring.security.user.name}")
    private String adminUsername;

    @Value("${spring.security.user.password}")
    private String adminPassword;

    private final UserRepository userRepository;
    private final RateLimitFilter rateLimitFilter; // Inject Filter vào đây

    // Sửa Constructor để nhận RateLimitFilter
    public SecurityConfig(UserRepository userRepository, RateLimitFilter rateLimitFilter) {
        this.userRepository = userRepository;
        this.rateLimitFilter = rateLimitFilter;
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
                // Thêm vào SecurityConfig.java
                .rememberMe(remember -> remember
                        .key("randomCharSecretKey2024") // Khóa bí mật
                        .tokenValiditySeconds(7 * 24 * 60 * 60) // 7 ngày
                        .userDetailsService(databaseUserDetailsService()) // Chỉ định rõ dùng DB cho Remember Me
                )
                // Sử dụng injected bean thay vì new
                .addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class)
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