package dev.zwazel.api.security;

import dev.zwazel.domain.User;
import dev.zwazel.exception.UserNotFoundException;
import dev.zwazel.repository.UserRepository;
import dev.zwazel.security.AuthController;
import dev.zwazel.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Value("${roles.user}")
    private String userRoleName;

    @Test
    void login() throws Exception {
        // given
        var username = "testuser-" + java.util.UUID.randomUUID();
        ;
        var password = "password";
        userService.register(new AuthController.LoginRegisterRequest(username, password, null));

        // when
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\": \"" + username + "\", \"password\": \"" + password + "\"}"))
                // then
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists());
    }

    @Test
    void register() throws Exception {
        // given
        var username = "testuser-" + java.util.UUID.randomUUID();
        var password = "password";

        // when
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\": \"" + username + "\", \"password\": \"" + password + "\"}"))
                // then
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists());
        // verify user is created
        User user = userRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new UserNotFoundException(username));
        assert user != null;
        assert user.getUsername().equals(username);
        assert user.getPassword() != null && !user.getPassword().isEmpty();
        assert user.getRoles() != null && !user.getRoles().isEmpty();
        assert user.getRoles().stream().anyMatch(role -> role.getName().equalsIgnoreCase(userRoleName));
    }

    @Test
    void registerWithExistingUsername() throws Exception {
        // given
        var username = "testuser-" + java.util.UUID.randomUUID();
        var password = "password";
        userService.register(new AuthController.LoginRegisterRequest(username, password, null));

        // when
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\": \"" + username + "\", \"password\": \"" + password + "\"}"))
                // then
                .andExpect(status().isBadRequest())
                // get message from response body
                .andExpect(result -> {
                            String response = result.getResponse().getContentAsString();
                            assert response.equals("Username already exists: " + username);
                        }
                );
    }
}
