package dev.zwazel.api.hal.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.hateoas.RepresentationModel;

import java.util.List;
import java.util.UUID;

@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
public class LoginResponseModel extends RepresentationModel<LoginResponseModel> {
    private UUID id;
    private String username;
    private List<String> roles;
}
