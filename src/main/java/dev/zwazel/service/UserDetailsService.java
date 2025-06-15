package dev.zwazel.service;

import dev.zwazel.domain.User;
import dev.zwazel.repository.UserRepository;
import dev.zwazel.security.CustomUserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserDetailsService implements ReactiveUserDetailsService {

    private final UserRepository userRepo;

    @Override
    public Mono<UserDetails> findByUsername(String username) {

        /* Query repo → map to principal → propagate error if absent */
        return userRepo.findByUsernameIgnoreCase(username)
                .switchIfEmpty(Mono.error(
                        new UsernameNotFoundException("User not found: " + username)))
                .map(this::toPrincipal);
    }

    /*─────────────────────────────────────────────────────
     *  Entity → Security principal
     *────────────────────────────────────────────────────*/
    private CustomUserPrincipal toPrincipal(User u) {
        return new CustomUserPrincipal(
                u.getId(),
                u.getUsername(),
                u.getPassword(),
                u.getRoles().stream()
                        .map(r -> new SimpleGrantedAuthority("ROLE_" + r.getName()))
                        .toList()
        );
    }
}
