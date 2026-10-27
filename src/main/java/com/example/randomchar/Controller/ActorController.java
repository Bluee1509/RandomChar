package com.example.randomchar.Controller;

import com.example.randomchar.Entity.Actor;
import com.example.randomchar.Repository.ActorRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/actors")
@CrossOrigin(origins = "*")
public class ActorController {
    private final ActorRepository actorRepository;

    public ActorController(ActorRepository actorRepository) {
        this.actorRepository = actorRepository;
    }

    @GetMapping
    public List<Actor> getAllActors() {
        // Sắp xếp theo thời gian tạo (mới nhất ở trên)
        System.out.println("------ ĐANG LẤY DỮ LIỆU TỪ DATABASE ------");
        return actorRepository.findAll(org.springframework.data.domain.Sort.by("createdAt").ascending());
    }
    // API MỚI: Hỗ trợ phân trang cho Dashboard
    @GetMapping("/paginated")
    public org.springframework.data.domain.Page<Actor> getActorsPaginated(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean hasCode) {

        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(
                page, size, org.springframework.data.domain.Sort.by("createdAt").descending()
        );

        // Xử lý chuỗi rỗng
        String searchKeyword = (search == null || search.trim().isEmpty()) ? null : search;

        return actorRepository.searchAndFilter(searchKeyword, hasCode, pageable);
    }

    @GetMapping("/random")
    public ResponseEntity<Actor> getRandomActor() {
        return actorRepository.findRandomActor()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    @PostMapping
    @CacheEvict(value = "actors", allEntries = true)
    public ResponseEntity<Actor> createActor(@RequestBody Actor actor) {
        Actor savedActor = actorRepository.save(actor);
        return new ResponseEntity<>(savedActor, HttpStatus.CREATED);
    }

    @PatchMapping("/{id}")
    @CacheEvict(value = "actors", allEntries = true)
    public ResponseEntity<Actor> patchActor(@PathVariable UUID id, @RequestBody Map<String, Object> updates) {
        return actorRepository.findById(id).map(actor -> {
            if (updates.containsKey("name") && updates.get("name") != null) {
                actor.setName(updates.get("name").toString().trim());
            }
            if (updates.containsKey("birthYear") && updates.get("birthYear") != null) {
                actor.setBirthYear(Integer.parseInt(updates.get("birthYear").toString()));
            }
            if (updates.containsKey("imageUrl") && updates.get("imageUrl") != null) {
                actor.setImageUrl(updates.get("imageUrl").toString().trim());
            }
            if (updates.containsKey("famousCode")) {
                actor.setFamousCode(updates.get("famousCode") != null ? updates.get("famousCode").toString().trim() : null);
            }
            if (updates.containsKey("webUrl")) {
                actor.setWebUrl(updates.get("webUrl") != null ? updates.get("webUrl").toString().trim() : null);
            }

            return ResponseEntity.ok(actorRepository.save(actor));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @CacheEvict(value = "actors", allEntries = true)
    public ResponseEntity<Void> deleteActor(@PathVariable UUID id) {
        if (actorRepository.existsById(id)) {
            actorRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}