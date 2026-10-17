package com.example.randomchar.Repository;

import com.example.randomchar.Entity.Actor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ActorRepository extends JpaRepository<Actor, UUID> { // Đã đổi sang UUID
    // Truy vấn ngẫu nhiên 1 diễn viên từ PostgreSQL
    @Query(value = "SELECT * FROM actors ORDER BY RANDOM() LIMIT 1", nativeQuery = true)
    Optional<Actor> findRandomActor();
}