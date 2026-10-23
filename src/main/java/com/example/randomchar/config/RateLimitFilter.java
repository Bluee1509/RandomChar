package com.example.randomchar.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    // Lưu trữ "xô" token cho từng IP (mỗi IP sẽ có 3 xô riêng biệt)
    private final Map<String, Bucket> cache = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();

        // 1. Bỏ qua các request tải tài nguyên tĩnh (HTML, CSS, JS, ảnh)
        if (!path.startsWith("/api/")) {
            filterChain.doFilter(request, response);
            return;
        }

        String ip = request.getRemoteAddr();
        Bucket bucket;

        // 2. Phân loại API để lấy đúng "xô" token tương ứng
        if (path.startsWith("/api/auth/register")) {
            // Nhóm 1: Đăng ký (Nghiêm ngặt - 3 lần/phút để chống spam mail)
            bucket = cache.computeIfAbsent(ip + "_register", k -> createStrictBucket());
        } else if (path.startsWith("/api/actors/random")) {
            // Nhóm 2: Quay số (Vừa phải - 10 lần/10 giây để chống auto-roll)
            bucket = cache.computeIfAbsent(ip + "_roll", k -> createModerateBucket());
        } else {
            // Nhóm 3: Đọc dữ liệu, load trang (Thoải mái - 30 lần/10 giây)
            bucket = cache.computeIfAbsent(ip + "_general", k -> createRelaxedBucket());
        }

        // 3. Kiểm tra xem còn token trong xô không
        if (bucket.tryConsume(1)) {
            filterChain.doFilter(request, response);
        } else {
            // Nếu hết token, trả về lỗi 429 Too Many Requests
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            // Thông báo lỗi thân thiện bằng tiếng Việt
            response.getWriter().write("{\"error\": \"Bạn thao tác quá nhanh, vui lòng thử lại sau vài giây!\"}");
        }
    }

    // ==========================================
    // CÁC HÀM TẠO "XÔ" TOKEN THEO TỪNG MỨC ĐỘ
    // ==========================================

    // Mức 1: Nghiêm ngặt (3 requests / 1 phút)
    private Bucket createStrictBucket() {
        Refill refill = Refill.greedy(3, Duration.ofMinutes(1));
        Bandwidth limit = Bandwidth.classic(3, refill);
        return Bucket.builder().addLimit(limit).build();
    }

    // Mức 2: Vừa phải (10 requests / 10 giây)
    private Bucket createModerateBucket() {
        Refill refill = Refill.greedy(10, Duration.ofSeconds(10));
        Bandwidth limit = Bandwidth.classic(10, refill);
        return Bucket.builder().addLimit(limit).build();
    }

    // Mức 3: Thoải mái (30 requests / 10 giây)
    private Bucket createRelaxedBucket() {
        Refill refill = Refill.greedy(30, Duration.ofSeconds(10));
        Bandwidth limit = Bandwidth.classic(30, refill);
        return Bucket.builder().addLimit(limit).build();
    }
}