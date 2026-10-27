package com.example.randomchar.Controller;

import com.example.randomchar.Entity.User;
import com.example.randomchar.Repository.ActorRepository;
import com.example.randomchar.Repository.SpinHistoryRepository;
import com.example.randomchar.Repository.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.example.randomchar.Repository.PageViewRepository;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
public class AdminDashboardController {

    private final ActorRepository actorRepository;
    private final UserRepository userRepository;
    private final SpinHistoryRepository spinHistoryRepository;
    private final PageViewRepository pageViewRepository;

    public AdminDashboardController(ActorRepository actorRepository, UserRepository userRepository,
                                    SpinHistoryRepository spinHistoryRepository, PageViewRepository pageViewRepository) {
        this.actorRepository = actorRepository;
        this.userRepository = userRepository;
        this.spinHistoryRepository = spinHistoryRepository;
        this.pageViewRepository = pageViewRepository;
    }

    @GetMapping("/stats")
    public ResponseEntity<?> getStats() {
        long totalActors = actorRepository.count();
        long totalUsers = userRepository.count();
        long totalSpins = spinHistoryRepository.count();

        Map<String, Long> stats = new HashMap<>();
        stats.put("actors", totalActors);
        stats.put("users", totalUsers);
        stats.put("spins", totalSpins);
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/top-actors")
    public ResponseEntity<?> getTopActors() {
        return ResponseEntity.ok(spinHistoryRepository.getTopSpinnedActors(PageRequest.of(0, 5)));
    }

    @GetMapping("/chart-data")
    public ResponseEntity<?> getChartData() {
        // Lấy dữ liệu 7 ngày qua
        OffsetDateTime startDate = OffsetDateTime.now().minusDays(7);
        return ResponseEntity.ok(spinHistoryRepository.getSpinStatsByDate(startDate));
    }

    @GetMapping("/users")
    public ResponseEntity<?> getAllUsers() {
        return ResponseEntity.ok(userRepository.findAll());
    }

    @PatchMapping("/users/{id}/toggle-status")
    public ResponseEntity<?> toggleUserStatus(@PathVariable UUID id) {
        return userRepository.findById(id).map(user -> {
            boolean newStatus = !user.isEnabled();
            user.setEnabled(newStatus);

            if (!newStatus) {
                // Nếu đang chuyển sang Khóa (false) -> Đánh dấu là BANNED
                user.setOtpCode("BANNED");
            } else {
                // Nếu Mở khóa (true) -> Xóa dấu BANNED
                user.setOtpCode(null);
            }

            userRepository.save(user);
            return ResponseEntity.ok(Map.of("message", "Đã cập nhật trạng thái user", "enabled", user.isEnabled()));
        }).orElse(ResponseEntity.notFound().build());
    }
    @GetMapping("/visitor-chart-data")
    public ResponseEntity<?> getVisitorChartData() {
        LocalDate startDate = LocalDate.now().minusDays(30);
        return ResponseEntity.ok(pageViewRepository.getVisitorStatsByDate(startDate));
    }

    @GetMapping("/ping")
    public ResponseEntity<?> ping() {
        return ResponseEntity.ok("pong");
    }
}