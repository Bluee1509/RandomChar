package com.example.randomchar.Repository;

import com.example.randomchar.Entity.PageView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface PageViewRepository extends JpaRepository<PageView, UUID> {
    // Kiểm tra xem IP này đã truy cập trong ngày hôm nay chưa
    boolean existsByIpAddressAndViewDate(String ipAddress, LocalDate viewDate);

    // Lấy thống kê truy cập theo ngày
    @Query("SELECT p.viewDate as date, COUNT(p) as count FROM PageView p WHERE p.viewDate >= :startDate GROUP BY p.viewDate ORDER BY p.viewDate ASC")
    List<Map<String, Object>> getVisitorStatsByDate(@Param("startDate") LocalDate startDate);
}