package dev.zwazel.exception;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@EqualsAndHashCode(callSuper = true)
@ResponseStatus(HttpStatus.NOT_FOUND)
@Data
public class RoleNotFoundException extends RuntimeException {
    private final String roleName;

    public RoleNotFoundException(String roleName) {
        super("Role not found: " + roleName);
        this.roleName = roleName;
    }
}
