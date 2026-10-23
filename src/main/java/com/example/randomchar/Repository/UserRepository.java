package com.example.randomchar.Repository;

import com.example.randomchar.Entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    boolean existsByUsername(String username);
    // Tìm và xóa các user chưa kích hoạt được tạo trước thời điểm 'time'
    @Modifying
    @Transactional
    @Query("DELETE FROM User u WHERE u.enabled = false AND u.createdAt < :time")
    void deleteUnverifiedUsersOlderThan(LocalDateTime time);
}