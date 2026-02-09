package com.suifeng.sfchain.configcenter.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Console UI entry routing.
 */
@Controller
public class ConsoleEntryController {

    @GetMapping({
            "/",
            "/sf",
            "/sf/"
    })
    public String index() {
        return "forward:/index.html";
    }

    @GetMapping("/sf/favicon.ico")
    public String favicon() {
        return "forward:/favicon.ico";
    }

    @GetMapping("/sf/assets/{fileName:.+}")
    public String asset(@PathVariable String fileName) {
        return "forward:/assets/" + fileName;
    }

    @GetMapping("/sf/icons/{fileName:.+}")
    public String icon(@PathVariable String fileName) {
        return "forward:/icons/" + fileName;
    }
}
