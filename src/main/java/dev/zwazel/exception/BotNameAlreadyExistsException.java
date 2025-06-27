package dev.zwazel.exception;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@EqualsAndHashCode(callSuper = true)
@ResponseStatus(HttpStatus.BAD_REQUEST)
@Data
public class BotNameAlreadyExistsException extends RuntimeException {
    private final String botName;

    public BotNameAlreadyExistsException(String botName) {
        super("Bot name already exists: " + botName);
        this.botName = botName;
    }
}
