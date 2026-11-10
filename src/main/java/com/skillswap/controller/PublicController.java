package com.skillswap.controller;

import com.skillswap.service.PublicCatalogService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class PublicController {
    private final PublicCatalogService catalog;

    public PublicController(PublicCatalogService catalog) {
        this.catalog = catalog;
    }

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("categories", catalog.categories());
        model.addAttribute("popularSkills", catalog.popularSkills(8));
        model.addAttribute("metrics", catalog.metrics());
        model.addAttribute("testimonials", catalog.testimonials(3));
        return "home";
    }

    @GetMapping("/skills")
    public String skills(@RequestParam(required = false, defaultValue = "") String q,
                         @RequestParam(required = false, defaultValue = "") String category,
                         Model model) {
        model.addAttribute("skills", catalog.searchSkills(q, category));
        model.addAttribute("categories", catalog.categories());
        model.addAttribute("query", q);
        model.addAttribute("selectedCategory", category);
        return "skills/index";
    }
}
