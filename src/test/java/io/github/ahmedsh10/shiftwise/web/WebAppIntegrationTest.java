package io.github.ahmedsh10.shiftwise.web;

import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** The supervisor web app is served without a token; its data calls still need one. */
class WebAppIntegrationTest extends IntegrationTestBase {

    @Test
    void rootServesTheWebApp() throws Exception {
        mvc.perform(get("/")).andExpect(status().isOk()).andExpect(forwardedUrl("index.html"));
        mvc.perform(get("/index.html")).andExpect(status().isOk())
                .andExpect(content().string(containsString("Try it as a supervisor")));
    }

    @Test
    void appAssetsArePublic() throws Exception {
        mvc.perform(get("/app/app.js")).andExpect(status().isOk());
        mvc.perform(get("/app/app.css")).andExpect(status().isOk());
    }

    @Test
    void dataStillRequiresAToken() throws Exception {
        mvc.perform(get("/api/coverage").param("date", "2027-02-01")).andExpect(status().isUnauthorized());
    }
}
