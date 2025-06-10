package dev.zwazel.DTO;

import lombok.NonNull;

import java.util.UUID;

/**
 * A Data Transfer Object (DTO) for public user information.
 * No Private or sensitive data is included in this DTO, like passwords.
 */
public record PublicUserDTO(@NonNull UUID id, String username) {
}
