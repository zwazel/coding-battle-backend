package dev.zwazel.exception;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.UUID;

@EqualsAndHashCode(callSuper = true)
@ResponseStatus(HttpStatus.NOT_FOUND)
@Data
public class BotNotFoundException extends RuntimeException {
    private final UUID botId;
    private final UUID ownerId;
    private final String botName;
    private final String ownerName;

    public BotNotFoundException(String botName, UUID ownerId) {
        super("Bot not found with name: " + botName + " for owner ID: " + ownerId);
        this.botId = null;
        this.ownerId = ownerId;
        this.botName = botName;
        this.ownerName = null;
    }

    public BotNotFoundException(String botName, String ownerName) {
        super("Bot not found with name: " + botName + " for owner name: " + ownerName);
        this.botId = null;
        this.ownerId = null;
        this.botName = botName;
        this.ownerName = ownerName;
    }

    public BotNotFoundException(UUID botId) {
        super("Bot not found with ID: " + botId);
        this.botId = botId;
        this.ownerId = null;
        this.botName = null;
        this.ownerName = null;
    }
}
