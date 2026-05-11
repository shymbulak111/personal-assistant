package kz.projem.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import kz.projem.dto.request.LoginRequest;
import kz.projem.dto.request.RegisterRequest;
import kz.projem.dto.response.AuthResponse;
import kz.projem.service.UserService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@Tag("unit")
class AuthControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean UserService userService;

    @Test
    @WithAnonymousUser
    void register_withValidData_returns201() throws Exception {
        RegisterRequest req = new RegisterRequest();
        req.setEmail("newuser@test.com");
        req.setPassword("Password123");
        req.setFirstName("Test");

        AuthResponse resp = AuthResponse.builder()
                .accessToken("token123")
                .email("newuser@test.com")
                .build();

        when(userService.register(any())).thenReturn(resp);

        mockMvc.perform(post("/api/v1/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").value("token123"))
                .andExpect(jsonPath("$.email").value("newuser@test.com"));
    }

    @Test
    @WithAnonymousUser
    void register_withInvalidEmail_returns400() throws Exception {
        RegisterRequest req = new RegisterRequest();
        req.setEmail("not-an-email");
        req.setPassword("Password123");

        mockMvc.perform(post("/api/v1/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithAnonymousUser
    void login_withValidCredentials_returnsToken() throws Exception {
        LoginRequest req = new LoginRequest();
        req.setEmail("user@test.com");
        req.setPassword("Password123");

        AuthResponse resp = AuthResponse.builder()
                .accessToken("jwt-token")
                .email("user@test.com")
                .build();

        when(userService.login(any())).thenReturn(resp);

        mockMvc.perform(post("/api/v1/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists());
    }
}
