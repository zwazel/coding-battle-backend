package dev.zwazel.api.controller;

import dev.zwazel.domain.User;
import dev.zwazel.security.AuthController;
import dev.zwazel.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Slf4j
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserService userService;

    @Test
    void getUserShouldBeAuthenticated() throws Exception {
        log.info("Testing getUser endpoint with authentication...");

        // given
        var username = "testuser2";
        var password = "password";
        User user = userService.register(new AuthController.LoginRegisterRequest(username, password, 0L));
        log.info("Registered user: {}", user);

        // when
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\": \"" + username + "\", \"password\": \"" + password + "\", \"ttlSeconds\": 0}"))
                .andExpect(status().isOk())
                .andReturn();

        log.info("Login successful, received response: {}", result.getResponse().getContentAsString());

        String response = result.getResponse().getContentAsString();
        String token = response.substring(response.indexOf("token\":\"") + 9, response.indexOf("\",\"expiresInSeconds") - 1);

        log.info("Extracted token: {}", token);

        mockMvc.perform(get("/users/" + user.getId())
                        .header("Authorization", "Bearer " + token))
                // then
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(username));

        log.info("getUser endpoint test completed successfully.");
    }
}
