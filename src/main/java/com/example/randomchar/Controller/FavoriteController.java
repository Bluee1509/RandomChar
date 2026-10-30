package com.example.randomchar.Controller;

import com.example.randomchar.Entity.Actor;
import com.example.randomchar.Entity.Favorite;
import com.example.randomchar.Entity.User;
import com.example.randomchar.Repository.ActorRepository;
import com.example.randomchar.Repository.FavoriteRepository;
import com.example.randomchar.Repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/favorites")
@CrossOrigin(origins = "*")
public class FavoriteController {

    private final FavoriteRepository favoriteRepository;
    private final UserRepository userRepository;
    private final ActorRepository actorRepository;

    public FavoriteController(FavoriteRepository favoriteRepository, UserRepository userRepository, ActorRepository actorRepository) {
        this.favoriteRepository = favoriteRepository;
        this.userRepository = userRepository;
        this.actorRepository = actorRepository;
    }

    // Lấy danh sách ID diễn viên yêu thích của user
    @GetMapping
    public ResponseEntity<?> getFavorites(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || authentication.getName().equals("anonymousUser")) {
            return ResponseEntity.status(401).body(Map.of("message", "Chưa đăng nhập"));
        }

        User user = userRepository.findByUsername(authentication.getName()).orElse(null);
        if (user == null) return ResponseEntity.badRequest().body(Map.of("message", "User không tồn tại"));

        List<UUID> favoriteActorIds = favoriteRepository.findByUser(user)
                .stream().map(f -> f.getActor().getId()).collect(Collectors.toList());

        return ResponseEntity.ok(favoriteActorIds);
    }

    // Thêm vào yêu thích
    @PostMapping("/{actorId}")
    public ResponseEntity<?> addFavorite(@PathVariable UUID actorId, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }

        User user = userRepository.findByUsername(authentication.getName()).orElse(null);
        Actor actor = actorRepository.findById(actorId).orElse(null);

        if (user == null || actor == null) return ResponseEntity.badRequest().build();

        if (!favoriteRepository.existsByUserAndActor(user, actor)) {
            favoriteRepository.save(Favorite.builder().user(user).actor(actor).build());
        }
        return ResponseEntity.ok(Map.of("message", "Đã thêm vào yêu thích"));
    }

    // Xoá khỏi yêu thích
    @DeleteMapping("/{actorId}")
    public ResponseEntity<?> removeFavorite(@PathVariable UUID actorId, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }

        User user = userRepository.findByUsername(authentication.getName()).orElse(null);
        Actor actor = actorRepository.findById(actorId).orElse(null);

        if (user == null || actor == null) return ResponseEntity.badRequest().build();

        favoriteRepository.deleteByUserAndActor(user, actor);
        return ResponseEntity.ok(Map.of("message", "Đã xoá khỏi yêu thích"));
    }
}