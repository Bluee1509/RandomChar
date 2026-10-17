package com.example.randomchar.Entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.time.Year;
import java.util.UUID;

@Entity
@Table(name = "actors")
@Getter
@Setter
@NoArgsConstructor
public class Actor {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false, columnDefinition = "uuid")
    private UUID id;

    private String name;

    private Integer birthYear; // Nhập và lưu năm sinh

    private String imageUrl;

    @Column(length = 1000)
    private String famousCode; // Danh sách code nổi bật

    private String webUrl;     // Link trang web cá nhân/profile

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    // Tự động tính tuổi theo năm hiện tại khi chuyển thành JSON (không tạo cột trong DB)
    @Transient
    public Integer getAge() {
        if (birthYear == null || birthYear <= 0) return null;
        return Year.now().getValue() - birthYear;
    }
}