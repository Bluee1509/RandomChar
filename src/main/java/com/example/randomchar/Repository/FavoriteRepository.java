package com.example.randomchar.Repository;

import com.example.randomchar.Entity.Actor;
import com.example.randomchar.Entity.Favorite;
import com.example.randomchar.Entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying; // Thêm import này
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional; // Thêm import này

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FavoriteRepository extends JpaRepository<Favorite, UUID> {
    List<Favorite> findByUser(User user);
    Optional<Favorite> findByUserAndActor(User user, Actor actor);
    boolean existsByUserAndActor(User user, Actor actor);

    // THÊM 2 ANNOTATION NÀY VÀO
    @Transactional
    @Modifying
    void deleteByUserAndActor(User user, Actor actor);
}