package com.springai.controller;


import com.springai.service.GeminiService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@CrossOrigin(origins = "http://localhost:3000")
@RestController
@RequestMapping("/api/chat")
public class GeminiController {

    private final GeminiService geminiService;

    public GeminiController(GeminiService geminiService) {
        this.geminiService = geminiService;
    }

    @PostMapping("/ask")
    public String askGemini(@RequestBody Map<String, String> request, @RequestParam(defaultValue = "models/gemini-2.5-flash") String model) {
        String prompt = request.get("prompt");
        return geminiService.generateContent(prompt, model);
    }

}
