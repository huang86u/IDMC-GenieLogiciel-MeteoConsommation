package com.example.genielogicielmeteoconsommation.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {

    @GetMapping({"/", "/index.html"})
    public String index() {
        return "index";
    }

    @GetMapping("/vue-ensemble")
    public String vueEnsemble() {
        return "vue-ensemble";
    }

    @GetMapping("/explorer")
    public String explorer() {
        return "explorer";
    }

    @GetMapping("/relation")
    public String relation() {
        return "relation";
    }

    @GetMapping("/analyse-saisonniere")
    public String analyseSaisonniere() {
        return "analyse-saisonniere";
    }

    @GetMapping("/scenarios")
    public String scenarios() {
        return "scenarios";
    }

    @GetMapping("/transparence")
    public String transparence() {
        return "transparence";
    }
}
