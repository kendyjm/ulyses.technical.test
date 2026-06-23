package com.septeo.ulyses.technical.test;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "spring.sql.init.mode=never")
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void getEndpoint_withoutAuth_isPublic() throws Exception {
        mockMvc.perform(get("/api/brands"))
                .andExpect(status().isOk());
    }

    @Test
    void postEndpoint_withoutAuth_returns401() throws Exception {
        mockMvc.perform(post("/api/brands")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"X\",\"description\":\"Y\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void putEndpoint_withoutAuth_returns401() throws Exception {
        mockMvc.perform(put("/api/brands/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"X\",\"description\":\"Y\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deleteEndpoint_withoutAuth_returns401() throws Exception {
        mockMvc.perform(delete("/api/brands/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    void postEndpoint_withAuth_isNotRejected() throws Exception {
        // Security boundary only: an authenticated principal must not be rejected.
        // Asserting "not 401/403" (per spec Task 3) decouples this from whether the
        // create itself succeeds (validation / unique brand.name / persistence).
        mockMvc.perform(post("/api/brands")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"TestBrand\",\"description\":\"Desc\"}"))
                .andExpect(status().is(not(401)))
                .andExpect(status().is(not(403)));
    }

    // valid credentials are actually processed (exercises the in-memory user end-to-end,
    // which @WithMockUser bypasses).
    @Test
    void postEndpoint_withValidCredentials_isProcessed() throws Exception {
        mockMvc.perform(post("/api/brands")
                .with(httpBasic("user", "password"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"ValidCredBrand\",\"description\":\"Desc\"}"))
                .andExpect(status().is2xxSuccessful());
    }

    // wrong password is rejected with 401.
    @Test
    void postEndpoint_withWrongPassword_returns401() throws Exception {
        mockMvc.perform(post("/api/brands")
                .with(httpBasic("user", "wrongpassword"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"X\",\"description\":\"Y\"}"))
                .andExpect(status().isUnauthorized());
    }

    // unknown user is rejected with 401.
    @Test
    void postEndpoint_withUnknownUser_returns401() throws Exception {
        mockMvc.perform(post("/api/brands")
                .with(httpBasic("nobody", "password"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"X\",\"description\":\"Y\"}"))
                .andExpect(status().isUnauthorized());
    }

    // the H2 console path is permitted by Security (not rejected with 401/403).
    // MockMvc does not route to the H2 servlet, so we assert only the security boundary,
    // not console rendering (the iframe/rendering check stays manual).
    @Test
    void h2Console_withoutAuth_isPermittedBySecurity() throws Exception {
        mockMvc.perform(get("/h2-console"))
                .andExpect(status().is(not(401)))
                .andExpect(status().is(not(403)));
    }
}
