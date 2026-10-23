package com.example.randomchar.Repository;

import com.example.randomchar.Entity.SpinHistory;
import com.example.randomchar.Entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface SpinHistoryRepository extends JpaRepository<SpinHistory, UUID> {
    // Lấy 10 lịch sử mới nhất của 1 user, sắp xếp giảm dần theo thời gian
    List<SpinHistory> findTop10ByUserOrderBySpinTimeDesc(User user);
}