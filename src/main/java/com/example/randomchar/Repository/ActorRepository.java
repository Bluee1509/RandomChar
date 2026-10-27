package com.example.randomchar.Repository;

import com.example.randomchar.Entity.Actor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ActorRepository extends JpaRepository<Actor, UUID> {
    // Truy vấn ngẫu nhiên 1 diễn viên từ PostgreSQL
    @Query(value = "SELECT * FROM actors ORDER BY RANDOM() LIMIT 1", nativeQuery = true)
    Optional<Actor> findRandomActor();

    // Tìm kiếm và lọc
    @Query("SELECT a FROM Actor a WHERE " +
            "(:search IS NULL OR LOWER(a.name) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
            "(:hasCode IS NULL OR (:hasCode = true AND a.famousCode IS NOT NULL AND a.famousCode <> '') OR (:hasCode = false AND (a.famousCode IS NULL OR a.famousCode = '')))")
    Page<Actor> searchAndFilter(@Param("search") String search, @Param("hasCode") Boolean hasCode, Pageable pageable);
}