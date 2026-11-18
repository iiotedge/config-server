package com.iotmining.config_server.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * This service previously had no SecurityConfig at all - proves the fix,
 * not just that the app boots. Uses the "native" (filesystem) backend
 * (see application-test.yml) rather than the real git one, so this also
 * proves an authenticated caller gets a real, correctly-resolved config
 * document back - not just a 200 with an empty body.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({"test", "native"})
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void configEndpoint_requiresAuth() throws Exception {
        mockMvc.perform(get("/sample-app/default"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void configEndpoint_servesTheRealDocument_withValidCredentials() throws Exception {
        mockMvc.perform(get("/sample-app/default")
                        .with(SecurityMockMvcRequestPostProcessors.httpBasic("test-admin", "test-password")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.propertySources[0].source.greeting").value("hello-from-config-server"));
    }

    @Test
    void configEndpoint_rejectsWrongCredentials() throws Exception {
        mockMvc.perform(get("/sample-app/default")
                        .with(SecurityMockMvcRequestPostProcessors.httpBasic("test-admin", "wrong-password")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void actuatorHealth_isPublic() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }
}
