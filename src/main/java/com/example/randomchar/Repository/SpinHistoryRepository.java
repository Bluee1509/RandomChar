package com.example.randomchar.Repository;

import com.example.randomchar.Entity.SpinHistory;
import com.example.randomchar.Entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface SpinHistoryRepository extends JpaRepository<SpinHistory, UUID> {
    List<SpinHistory> findTop10ByUserOrderBySpinTimeDesc(User user);

    // Lấy top 5 diễn viên được quay nhiều nhất
    @Query("SELECT s.actorName as name, COUNT(s) as count FROM SpinHistory s GROUP BY s.actorName ORDER BY COUNT(s) DESC")
    List<Map<String, Object>> getTopSpinnedActors(org.springframework.data.domain.Pageable pageable);

    // Đếm số lượt quay theo ngày (VD: lấy 7 ngày qua)
    @Query(value = "SELECT CAST(spin_time AS DATE) as date, COUNT(*) as count FROM spin_histories WHERE spin_time >= :startDate GROUP BY CAST(spin_time AS DATE) ORDER BY CAST(spin_time AS DATE) ASC", nativeQuery = true)
    List<Map<String, Object>> getSpinStatsByDate(@Param("startDate") OffsetDateTime startDate);
}