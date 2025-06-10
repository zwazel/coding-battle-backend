package dev.zwazel.domain;

import dev.zwazel.model.language.Language;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.util.UUID;

@Entity
@Table(name = "bots",
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "name_lower"}))  // unique per owner
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Bot {

    @Id
    @GeneratedValue @UuidGenerator
    @Column(length = 36, nullable = false, updatable = false)
    private UUID id;

    /** Bot name – unique for each user */
    @Column(nullable = false, length = 40, updatable = false)
    private String name;

    /** Bot lowercase name – used for case-insensitive lookups */
    @Column(name = "name_lower", nullable = false, length = 40, updatable = false)
    private String nameLower;

    /** Absolute or project-relative path to raw source file / folder */
    @Column(name = "source_path", length = 255, updatable = false)
    private String sourcePath;

    /** Path to compiled WASM artifact */
    @Column(name = "wasm_path", length = 255)
    private String wasmPath;

    /** Owning user (many-to-one) */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", updatable = false)          // FK column
    private User owner;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, updatable = false)
    private Language language;

    @PrePersist
    @PreUpdate
    private void updateNameLower() {
        if (name != null) {
            nameLower = name.toLowerCase();
        }
    }
}
