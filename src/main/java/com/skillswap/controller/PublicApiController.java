package com.skillswap.controller;

import com.skillswap.dto.CategorySummary;
import com.skillswap.dto.PublicMetrics;
import com.skillswap.dto.SkillSummary;
import com.skillswap.service.PublicCatalogService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Public, read-only data for the separately hosted frontend. */
@RestController
public class PublicApiController {
    private final PublicCatalogService catalog;

    public PublicApiController(PublicCatalogService catalog) {
        this.catalog = catalog;
    }

    @GetMapping("/api/public/home")
    public HomeResponse home() {
        return new HomeResponse(catalog.metrics(), catalog.popularSkills(8), catalog.categories());
    }

    public record HomeResponse(PublicMetrics metrics, List<SkillSummary> popularSkills,
                               List<CategorySummary> categories) { }
}
