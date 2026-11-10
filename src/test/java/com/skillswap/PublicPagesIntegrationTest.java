package com.skillswap;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.skillswap.service.PublicCatalogService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PublicPagesIntegrationTest {
    @Autowired private MockMvc mvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private PublicCatalogService catalog;

    @Test
    @WithAnonymousUser
    void homepageRendersDatabaseBackedCatalogAndHonestEmptyCommunityCounts() throws Exception {
        mvc.perform(get("/"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Exchange skills.")))
            .andExpect(content().string(containsString("Programming")))
            .andExpect(content().string(containsString("Counts update from the SkillSwap database.")))
            .andExpect(content().string(containsString("No public mentor profiles yet.")))
            .andExpect(header().string("Content-Security-Policy", containsString("frame-ancestors 'none'")));
        org.junit.jupiter.api.Assertions.assertEquals(10,
            jdbc.queryForObject("SELECT COUNT(*) FROM skill_categories WHERE active = 1", Integer.class));
    }

    @Test
    @WithAnonymousUser
    void catalogSearchUsesTheRealSkillRows() throws Exception {
        mvc.perform(get("/skills").param("q", "Java"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Java")));
        org.junit.jupiter.api.Assertions.assertTrue(catalog.searchSkills("Java", "").stream()
            .anyMatch(skill -> skill.name().equals("Java")));
    }

    @Test
    @WithAnonymousUser
    void categoryFilterShowsOnlyMatchingSkills() throws Exception {
        var programming = catalog.searchSkills("", "programming");
        org.junit.jupiter.api.Assertions.assertFalse(programming.isEmpty());
        org.junit.jupiter.api.Assertions.assertTrue(programming.stream()
            .allMatch(skill -> skill.categorySlug().equals("programming")));
        mvc.perform(get("/skills").param("category", "programming"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Data Structures and Algorithms")));
    }

    @Test
    @WithAnonymousUser
    void accountAndPolicyPagesRenderForVisitors() throws Exception {
        mvc.perform(get("/register"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("name=\"_csrf\"")));
        mvc.perform(get("/login"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("name=\"_csrf\"")));
        for (String path : java.util.List.of("/about", "/contact", "/privacy", "/terms", "/forgot-password")) {
            mvc.perform(get(path)).andExpect(status().isOk());
        }
        mvc.perform(get("/reset-password"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("This reset link is unavailable.")));
    }

    @Test
    @WithAnonymousUser
    void localFrontendAssetsArePublicAndServed() throws Exception {
        mvc.perform(get("/js/password-toggle.js"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("aria-pressed")));
        mvc.perform(get("/icons/lucide.svg")).andExpect(status().isOk());
        mvc.perform(get("/images/mark.svg")).andExpect(status().isOk());
    }

    @Test
    @WithAnonymousUser
    void dashboardIsNotPublic() throws Exception {
        mvc.perform(get("/dashboard"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrlPattern("**/login"));
    }
}
