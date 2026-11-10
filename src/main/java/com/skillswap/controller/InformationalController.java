package com.skillswap.controller;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class InformationalController {
    private final String supportEmail;

    public InformationalController(@Value("${skillswap.support-email:}") String supportEmail) {
        this.supportEmail = supportEmail == null ? "" : supportEmail.strip();
    }

    @GetMapping("/about")
    public String about(Model model) {
        model.addAttribute("title", "About SkillSwap");
        model.addAttribute("eyebrow", "A fairer way to learn");
        model.addAttribute("heading", "Learning is better when it goes both ways.");
        model.addAttribute("intro", "SkillSwap is being built to help students trade knowledge, practice, and time — without charging one another.");
        model.addAttribute("sections", List.of(
            new InfoSection("A skill exchange, not a marketplace", "Students list what they can teach and what they hope to learn. Recommendations are designed around those real overlaps, not paid placement."),
            new InfoSection("Learn with your peers", "A useful exchange can be small: a focused conversation, a practice session, or a new perspective from someone on the same learning journey."),
            new InfoSection("Credits stay in the app", "Skill Credits are intended as an in-app learning reward. They are not money, have no cash value, and cannot be sold or withdrawn.")));
        return "info/standard";
    }

    @GetMapping("/contact")
    public String contact(Model model) {
        model.addAttribute("title", "Contact");
        model.addAttribute("eyebrow", "Get in touch");
        model.addAttribute("heading", "Contact SkillSwap.");
        model.addAttribute("intro", "Questions about the platform or a concern to report? Use the support contact configured by the SkillSwap operator.");
        model.addAttribute("sections", List.of());
        model.addAttribute("supportEmail", supportEmail);
        return "info/standard";
    }

    @GetMapping("/privacy")
    public String privacy(Model model) {
        model.addAttribute("title", "Privacy policy");
        model.addAttribute("eyebrow", "Your information");
        model.addAttribute("heading", "Privacy matters.");
        model.addAttribute("intro", "This early version collects the account details you submit and the minimum technical data needed to run the service. Review this policy before using a production deployment.");
        model.addAttribute("sections", List.of(
            new InfoSection("Information you provide", "Account registration stores your name, email address, optional college and department, optional semester, a password hash, and the time you accepted the terms. The original password is not stored."),
            new InfoSection("Why it is used", "The current account flow uses your email address to send a verification link and uses your details to create your account. Future features may add skills, availability, sessions, and messages; they will be described when enabled."),
            new InfoSection("Cookies and security", "The service uses an HttpOnly session cookie for sign-in. The Secure cookie flag is enabled for HTTPS deployments and is disabled only for local development over HTTP. Verification tokens are stored as SHA-256 hashes."),
            new InfoSection("Your choices", "Do not enter sensitive personal information. Account deletion, data export, retention schedules, and a production support process must be confirmed by the operator before public launch."),
            new InfoSection("Contact", supportEmail.isBlank() ? "A support address has not been configured for this deployment." : "For privacy questions, contact " + supportEmail + ".")));
        return "info/standard";
    }

    @GetMapping("/terms")
    public String terms(Model model) {
        model.addAttribute("title", "Terms of use");
        model.addAttribute("eyebrow", "Community guidelines");
        model.addAttribute("heading", "Use SkillSwap with care.");
        model.addAttribute("intro", "These early terms explain the intended rules for this student learning community. They need review by the operator and appropriate legal counsel before a public production launch.");
        model.addAttribute("sections", List.of(
            new InfoSection("Respect other students", "Use accurate account details, be respectful, keep agreed session times, and do not harass, impersonate, or mislead other members."),
            new InfoSection("Keep exchanges safe", "Do not share passwords, verification links, recovery codes, or sensitive personal data. Arrange in-person meetings responsibly and follow your institution's safety guidance."),
            new InfoSection("No money or cash-out", "Skill Credits are in-app rewards only. They are not currency, a security, or cryptocurrency; they cannot be purchased, transferred outside the platform, redeemed, or withdrawn."),
            new InfoSection("Content and access", "You are responsible for content you submit. The operator may restrict access to investigate abuse or protect the community. Reviews and public-profile features must only be used for genuine, completed exchanges."),
            new InfoSection("Availability", "This service is under active development. Features, data retention, support procedures, and legal terms can change; do not rely on this development instance for a permanent record.")));
        return "info/standard";
    }

    public record InfoSection(String heading, String body) { }
}
