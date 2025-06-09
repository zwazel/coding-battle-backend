package dev.zwazel.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.util.UUID;

@Entity
@Table(name = "roles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Role {
    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(length = 36, nullable = false, updatable = false)
    private UUID id;

    @Column(nullable = false, unique = true, length = 30)
    private String name;
}