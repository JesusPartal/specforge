package com.jesuspartal.specforge.domain.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "specs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Spec {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long Id;

    @Column(nullable = false)
    private String repoUrl;

    @Column(nullable = false)
    private String title;

    private String version;

    @Column(columnDefinition = "TEXT")
    private String rawContent;

    @Column(nullable = false)
    private LocalDateTime fetchedAt;
}
