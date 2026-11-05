package com.nexora.platform.release;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/release-notes")
public class ReleaseNotesController {

    private final ReleaseNotesService service;

    public ReleaseNotesController(ReleaseNotesService service) {
        this.service = service;
    }

    @GetMapping
    public ReleaseNotesResponseDto get() {
        return new ReleaseNotesResponseDto(service.currentVersion(), service.all());
    }
}
