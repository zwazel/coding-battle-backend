package dev.zwazel.exception;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@EqualsAndHashCode(callSuper = true)
@ResponseStatus(HttpStatus.BAD_REQUEST)
@Data
public class UsernameAlreadyExistsException extends RuntimeException {
    private final String userName;

    public UsernameAlreadyExistsException(String username) {
        super("Username already exists: " + username);
        this.userName = username;
    }
}
