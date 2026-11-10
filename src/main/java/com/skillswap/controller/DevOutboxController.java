package com.skillswap.controller;

import com.skillswap.service.DevMailOutboxService;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

@Controller
@Profile({"dev", "test"})
@ConditionalOnProperty(name = "skillswap.mail.dev-outbox", havingValue = "true")
@RequestMapping("/dev/outbox")
public class DevOutboxController {
    private final DevMailOutboxService outbox;

    public DevOutboxController(DevMailOutboxService outbox) {
        this.outbox = outbox;
    }

    @GetMapping
    public String outbox(Model model) {
        model.addAttribute("messages", outbox.latest());
        return "account/dev-outbox";
    }

    @PostMapping("/clear")
    public String clear(RedirectAttributes redirect) {
        // The local-only outbox is intentionally ephemeral. Recreating the queue is unnecessary;
        // clear via the supported service method so messages do not linger in this process.
        outbox.clear();
        redirect.addFlashAttribute("cleared", true);
        return "redirect:/dev/outbox";
    }
}
