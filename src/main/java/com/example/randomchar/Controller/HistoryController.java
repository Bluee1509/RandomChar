package com.example.randomchar.Controller;

import com.example.randomchar.Entity.SpinHistory;
import com.example.randomchar.Entity.User;
import com.example.randomchar.Repository.SpinHistoryRepository;
import com.example.randomchar.Repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/history")
@CrossOrigin(origins = "*")
public class HistoryController {
    private final SpinHistoryRepository historyRepository;
    private final UserRepository userRepository;

    public HistoryController(SpinHistoryRepository historyRepository, UserRepository userRepository) {
        this.historyRepository = historyRepository;
        this.userRepository = userRepository;
    }

    @PostMapping
    public ResponseEntity<?> saveHistory(@RequestBody Map<String, String> request, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || authentication.getName().equals("anonymousUser")) {
            return ResponseEntity.status(401).body(Map.of("message", "Chưa đăng nhập"));
        }

        User user = userRepository.findByUsername(authentication.getName()).orElse(null);
        if (user == null) return ResponseEntity.badRequest().body(Map.of("message", "User không tồn tại"));

        SpinHistory history = SpinHistory.builder()
                .user(user)
                .actorName(request.get("actorName"))
                .actorImageUrl(request.get("actorImageUrl"))
                .build();

        historyRepository.save(history);
        return ResponseEntity.ok(Map.of("message", "Đã lưu lịch sử"));
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