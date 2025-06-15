package dev.zwazel.api.hal.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.hateoas.RepresentationModel;

import java.util.UUID;

@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
public class UserModel extends RepresentationModel<UserModel> {
    private UUID id;
    private String username;
}
