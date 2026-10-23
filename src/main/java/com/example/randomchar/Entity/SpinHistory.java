package com.example.randomchar.Entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.ZonedDateTime;
import java.util.UUID;

@Entity
@Table(name = "spin_histories")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SpinHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private String actorName;
    private String actorImageUrl;
    @Builder.Default
    private ZonedDateTime spinTime = ZonedDateTime.now();
}