package com.balaji.controller;

import com.balaji.service.FrameService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class HomeController {

    private final FrameService frameService;

    @Value("${app.business.upi:8765673141@apl}")
    private String businessUpi;

    @Value("${app.business.phone:8299576949}")
    private String businessPhone;

    @Value("${app.business.name:BALAJI Photo Frames}")
    private String businessName;

    @Value("${razorpay.key.id:rzp_test_XXXXXXXXXXXXXXXX}")
    private String razorpayKeyId;

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("sizes",        frameService.getAllSizes());
        model.addAttribute("beadings",     frameService.getAllBeadings());
        model.addAttribute("covers",       frameService.getAllCovers());
        model.addAttribute("businessUpi",  businessUpi);
        model.addAttribute("businessPhone",businessPhone);
        model.addAttribute("businessName", businessName);
        model.addAttribute("razorpayKeyId",razorpayKeyId);
        return "index";
    }
}
