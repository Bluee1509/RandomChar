package com.example.randomchar.Controller;

import com.example.randomchar.Entity.Actor;
import com.example.randomchar.Entity.SpinHistory;
import com.example.randomchar.Entity.User;
import com.example.randomchar.Repository.ActorRepository;
import com.example.randomchar.Repository.SpinHistoryRepository;
import com.example.randomchar.Repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/history")
@CrossOrigin(origins = "*")
public class HistoryController {
    private final SpinHistoryRepository historyRepository;
    private final UserRepository userRepository;
    private final ActorRepository actorRepository; // Thêm ActorRepository

    // Cập nhật Constructor để inject ActorRepository
    public HistoryController(SpinHistoryRepository historyRepository, UserRepository userRepository, ActorRepository actorRepository) {
        this.historyRepository = historyRepository;
        this.userRepository = userRepository;
        this.actorRepository = actorRepository;
    }

    @PostMapping
    public ResponseEntity<?> saveHistory(@RequestBody Map<String, String> request, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || authentication.getName().equals("anonymousUser")) {
            return ResponseEntity.status(401).body(Map.of("message", "Chưa đăng nhập"));
        }

        User user = userRepository.findByUsername(authentication.getName()).orElse(null);
        if (user == null) return ResponseEntity.badRequest().body(Map.of("message", "User không tồn tại"));

        // Lấy actorId từ request Frontend gửi lên
        String actorIdStr = request.get("actorId");
        if (actorIdStr == null || actorIdStr.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Thiếu ID diễn viên"));
        }

        try {
            // Tìm diễn viên trong Database
            UUID actorId = UUID.fromString(actorIdStr);
            Actor actor = actorRepository.findById(actorId).orElse(null);

            if (actor == null) {
                return ResponseEntity.badRequest().body(Map.of("message", "Diễn viên không tồn tại"));
            }

            // Tạo lịch sử bằng cách lấy thông tin mới nhất từ DB (thay vì lấy từ Frontend)
            SpinHistory history = SpinHistory.builder()
                    .user(user)
                    .actorName(actor.getName())
                    .actorImageUrl(actor.getImageUrl())
                    .build();

            historyRepository.save(history);
            return ResponseEntity.ok(Map.of("message", "Đã lưu lịch sử"));

        } catch (IllegalArgumentException e) {
            // Xử lý trường hợp actorId không phải là UUID hợp lệ
            return ResponseEntity.badRequest().body(Map.of("message", "ID diễn viên không hợp lệ"));
        }
    }

    @GetMapping
    public ResponseEntity<?> getHistory(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || authentication.getName().equals("anonymousUser")) {
            return ResponseEntity.status(401).body(Map.of("message", "Chưa đăng nhập"));
        }

        User user = userRepository.findByUsername(authentication.getName()).orElse(null);
        if (user == null) return ResponseEntity.badRequest().body(Map.of("message", "User không tồn tại"));

        List<SpinHistory> histories = historyRepository.findTop10ByUserOrderBySpinTimeDesc(user);
        return ResponseEntity.ok(histories);
    }
}