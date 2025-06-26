package dev.zwazel.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(
        name = "users",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_username", columnNames = "username")
        }
)
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
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

    @Column(nullable = false, length = 256)
    private String username;               // as entered

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

    @OneToMany(mappedBy = "owner",
            cascade = CascadeType.ALL,
            orphanRemoval = true)
    @Singular
    private Set<Bot> bots = new HashSet<>();

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

    public void addBot(Bot bot) {
        bots.add(bot);
        bot.setOwner(this);
    }

    public void removeBot(Bot bot) {
        bots.remove(bot);
        bot.setOwner(null);
    }
}
