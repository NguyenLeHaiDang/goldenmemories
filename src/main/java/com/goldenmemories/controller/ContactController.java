package com.goldenmemories.controller;

import jakarta.validation.Valid;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.goldenmemories.model.ContactForm;

@Controller
public class ContactController {

    @GetMapping("/contact")
    public String contact(@RequestParam(value = "sent", required = false) String sent, Model model) {
        if (!model.containsAttribute("contactForm")) {
            model.addAttribute("contactForm", new ContactForm());
        }
        if (sent != null) {
            model.addAttribute("contactSuccess",
                "Thanks, we received your consultation request and will follow up soon.");
        }
        return "contact";
    }

    @PostMapping("/contact")
    public String submitContact(@Valid @ModelAttribute("contactForm") ContactForm form, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return "contact";
        }
        return "redirect:/contact?sent";
    }
}
