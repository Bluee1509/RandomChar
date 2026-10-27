package com.example.randomchar.Entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "page_views")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PageView {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "ip_address", nullable = false)
    private String ipAddress;

    @Column(name = "view_date", nullable = false)
    private LocalDate viewDate;
}