package com.example.randomchar.Controller;

import com.example.randomchar.Entity.Actor;
import com.example.randomchar.Repository.ActorRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

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
        return actorRepository.findAll(org.springframework.data.domain.Sort.by("id").ascending());
    }

    @GetMapping("/random")
    public ResponseEntity<Actor> getRandomActor() {
        return actorRepository.findRandomActor()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    @PostMapping
    public ResponseEntity<Actor> createActor(@RequestBody Actor actor) {
        Actor savedActor = actorRepository.save(actor);
        return new ResponseEntity<>(savedActor, HttpStatus.CREATED);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Actor> patchActor(@PathVariable Long id, @RequestBody Map<String, Object> updates) {
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
    public ResponseEntity<Void> deleteActor(@PathVariable Long id) {
        if (actorRepository.existsById(id)) {
            actorRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}