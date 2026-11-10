package com.skillswap.controller;

import com.skillswap.entity.UserEntity;
import com.skillswap.service.DashboardService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {
    private final DashboardService dashboard;

    public DashboardController(DashboardService dashboard) { this.dashboard = dashboard; }

    @GetMapping("/dashboard")
    public String overview(@AuthenticationPrincipal UserEntity user, Model model) {
        model.addAttribute("user", user);
        model.addAttribute("dashboard", dashboard.forStudent(user.getId()));
        return "dashboard/overview";
    }
}
