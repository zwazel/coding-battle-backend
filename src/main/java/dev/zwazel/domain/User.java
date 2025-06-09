package dev.zwazel.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "users", uniqueConstraints = @UniqueConstraint(columnNames = "username"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    private static final BCryptPasswordEncoder ENC = new BCryptPasswordEncoder();

    /**
     * Primary-key, auto-generated
     */
    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(length = 36, nullable = false, updatable = false)
    private UUID id;

    /**
     * public login/identifier
     */
    @Column(nullable = false, length = 40)
    private String username;

    /**
     * BCrypt-hashed password
     */
    @Column(nullable = false, length = 60)         // BCrypt hash is 60 chars
    private String password;

    /* ------------------------------------------------------------------ */
    /* static helpers                                                     */
    /* ------------------------------------------------------------------ */
    /**
     * simple role mapping (many-to-many)
     */
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id"))
    private Set<Role> roles;

    /**
     * Factory that auto-hashes a clear-text password.
     */
    public static User ofPlainPassword(String username, String rawPassword, Set<Role> roles) {
        return User.builder()
                .username(username)
                .password(ENC.encode(rawPassword))
                .roles(roles)
                .build();
    }

    /**
     * Validate raw password against stored hash
     */
    public boolean matches(String rawPassword) {
        return ENC.matches(rawPassword, this.password);
    }
}
