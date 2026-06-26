package com.jesuspartal.specforge.domain.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "specs")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Spec {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String repoUrl;

    @Column(nullable = false)
    private String title;

    private String version;

    @Column(columnDefinition = "TEXT")
    private String rawContent;

    @Column(nullable = false)
    private LocalDateTime fetchedAt;

    @Column(nullable = false)
    private String ownerLogin;
}
