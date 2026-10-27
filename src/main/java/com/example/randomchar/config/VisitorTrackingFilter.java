package com.example.randomchar.config;

import com.example.randomchar.Entity.PageView;
import com.example.randomchar.Repository.PageViewRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDate;

@Component
public class VisitorTrackingFilter extends OncePerRequestFilter {

    private final PageViewRepository pageViewRepository;

    public VisitorTrackingFilter(PageViewRepository pageViewRepository) {
        this.pageViewRepository = pageViewRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();

        // Chỉ đếm khi người dùng vào trang chủ hoặc index.html
        if (path.equals("/") || path.equals("/index.html")) {
            String ip = getClientIP(request);
            LocalDate today = LocalDate.now();

            // Nếu IP chưa được đếm trong ngày hôm nay thì lưu vào DB
            if (!pageViewRepository.existsByIpAddressAndViewDate(ip, today)) {
                pageViewRepository.save(PageView.builder()
                        .ipAddress(ip)
                        .viewDate(today)
                        .build());
            }
        }

        filterChain.doFilter(request, response);
    }

    private String getClientIP(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0];
    }
}