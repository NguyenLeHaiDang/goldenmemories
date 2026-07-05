package com.goldenmemories.controller;

import com.goldenmemories.model.LoginForm;
import com.goldenmemories.model.OtpForm;
import com.goldenmemories.model.RegisterForm;
import com.goldenmemories.service.OtpService;
import com.goldenmemories.service.UserService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthController {

    // Session key for the email pending OTP verification
    static final String SESSION_PENDING_EMAIL = "pendingEmail";

    private final OtpService otpService;
    private final UserService userService;

    public AuthController(OtpService otpService, UserService userService) {
        this.otpService = otpService;
        this.userService = userService;
    }

    // ── Login ────────────────────────────────────────────────────────────────

    @GetMapping("/login")
    public String login(@RequestParam(required = false) String error,
                        @RequestParam(required = false) String logout,
                        Model model) {
        if (!model.containsAttribute("loginForm")) {
            model.addAttribute("loginForm", new LoginForm());
        }
        if (error != null) {
            model.addAttribute("loginError", "Invalid email or password, or your email is not yet verified.");
        }
        if (logout != null) {
            model.addAttribute("logoutMessage", "You have been signed out.");
        }
        return "login";
    }

    // ── Registration ─────────────────────────────────────────────────────────

    @GetMapping("/register")
    public String register(Model model) {
        if (!model.containsAttribute("registerForm")) {
            model.addAttribute("registerForm", new RegisterForm());
        }
        return "register";
    }

    @PostMapping("/register")
    public String submitRegister(@Valid @ModelAttribute("registerForm") RegisterForm form,
                                 BindingResult bindingResult,
                                 HttpSession session,
                                 RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "register";
        }

        try {
            userService.register(form.getFullName(), form.getEmail(), form.getPhone(), form.getPassword());
        } catch (IllegalArgumentException e) {
            if ("email_taken".equals(e.getMessage())) {
                bindingResult.rejectValue("email", "email.taken",
                    "An account with this email already exists.");
                return "register";
            }
            throw e;
        }

        // Issue OTP and store the pending email in the session
        otpService.issueAndSend(form.getEmail());
        session.setAttribute(SESSION_PENDING_EMAIL, form.getEmail());

        redirectAttributes.addFlashAttribute("otpForm", new OtpForm(form.getEmail()));
        redirectAttributes.addFlashAttribute("otpSent", true);
        return "redirect:/otp";
    }

    // ── OTP verification ─────────────────────────────────────────────────────

    @GetMapping("/otp")
    public String otp(Model model) {
        if (!model.containsAttribute("otpForm")) {
            model.addAttribute("otpForm", new OtpForm());
        }
        return "otp";
    }

    @PostMapping("/otp")
    public String submitOtp(@Valid @ModelAttribute("otpForm") OtpForm form,
                            BindingResult bindingResult,
                            HttpSession session,
                            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "otp";
        }

        boolean valid = otpService.verify(form.getEmail(), form.getCode());
        if (!valid) {
            bindingResult.rejectValue("code", "otp.invalid",
                "The code is incorrect or has expired. Please request a new one.");
            return "otp";
        }

        // Mark account as verified and clear the pending-email session key
        userService.markEmailVerified(form.getEmail());
        session.removeAttribute(SESSION_PENDING_EMAIL);

        redirectAttributes.addFlashAttribute("registrationSuccess",
            "Your email has been verified. Please sign in.");
        return "redirect:/login";
    }

    // ── Resend OTP ────────────────────────────────────────────────────────────

    @PostMapping("/otp/resend")
    public String resendOtp(HttpSession session, RedirectAttributes redirectAttributes) {
        String email = (String) session.getAttribute(SESSION_PENDING_EMAIL);
        if (email == null) {
            return "redirect:/register";
        }
        otpService.issueAndSend(email);
        redirectAttributes.addFlashAttribute("otpForm", new OtpForm(email));
        redirectAttributes.addFlashAttribute("otpResent", true);
        return "redirect:/otp";
    }
}
