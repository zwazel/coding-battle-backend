package dev.zwazel.exception;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.UUID;

@EqualsAndHashCode(callSuper = true)
@ResponseStatus(HttpStatus.NOT_FOUND)
@Data
public class UserNotFoundException extends RuntimeException {
    private final UserNotFoundType type;
    private final UUID uuid;
    private final String username;

    public UserNotFoundException(String username) {
        super("User not found with username: " + username);
        this.type = UserNotFoundType.USERNAME;
        this.uuid = null;
        this.username = username;
    }

    public UserNotFoundException(UUID userId) {
        super("User not found with ID: " + userId);
        this.type = UserNotFoundType.USER_ID;
        this.uuid = userId;
        this.username = null;
    }

    enum UserNotFoundType {
        USERNAME,
        USER_ID
    }
}
