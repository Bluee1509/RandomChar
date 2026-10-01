package com.example.randomchar.Controller;

import com.example.randomchar.Entity.Actor;
import com.example.randomchar.Repository.ActorRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
        return actorRepository.findAll();
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

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteActor(@PathVariable Long id) {
        if (actorRepository.existsById(id)) {
            actorRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}
