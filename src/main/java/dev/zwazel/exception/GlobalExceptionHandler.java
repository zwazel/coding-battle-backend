package dev.zwazel.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    @ExceptionHandler({UsernameAlreadyExistsException.class, BotNameAlreadyExistsException.class})
    public ResponseEntity<String> handleDuplicateEntryExceptions(UsernameAlreadyExistsException ex) {
        log.error(ex.getMessage());
        return ResponseEntity.badRequest().body(ex.getMessage());
    }

    @ExceptionHandler({BotNotFoundException.class, RoleNotFoundException.class, UserNotFoundException.class})
    public ResponseEntity<String> handleNotFoundExceptions(BotNotFoundException ex) {
        log.error(ex.getMessage());
        return ResponseEntity.status(404).body(ex.getMessage());
    }
}
