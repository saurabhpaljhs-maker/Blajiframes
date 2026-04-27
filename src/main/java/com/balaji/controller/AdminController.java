package com.balaji.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AdminController {

    // Login page
    @GetMapping("/login")
    public String loginPage(
            @RequestParam(required = false) String error,
            @RequestParam(required = false) String logout,
            Model model) {

        if (error != null)  model.addAttribute("error",  "Invalid username or password!");
        if (logout != null) model.addAttribute("logout", "Logged out successfully.");
        return "admin/login";
    }
}
