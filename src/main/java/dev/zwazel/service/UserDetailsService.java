package dev.zwazel.service;

import dev.zwazel.domain.User;
import dev.zwazel.repository.UserRepository;
import dev.zwazel.security.CustomUserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserDetailsService implements org.springframework.security.core.userdetails.UserDetailsService {
    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.info("Loading user by username: {}", username);
        User u = userRepository.findByUsernameIgnoreCase(username).orElseThrow(() -> {
            log.warn("User not found with username: {}", username);
            return new UsernameNotFoundException("User not found with username: " + username);
        });

        log.info("Found user: {}", u.getUsername());

        return new CustomUserPrincipal(
                u.getId(),
                u.getUsername(),
                u.getPassword(),
                u.getRoles().stream()
                        .map(role -> new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_" + role.getName()))
                        .toList()
        );
    }
}
