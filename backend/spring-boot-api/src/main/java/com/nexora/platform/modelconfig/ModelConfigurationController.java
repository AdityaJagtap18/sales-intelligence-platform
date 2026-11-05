package com.nexora.platform.modelconfig;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/model-configuration")
public class ModelConfigurationController {

    private final ModelConfigurationService service;

    public ModelConfigurationController(ModelConfigurationService service) {
        this.service = service;
    }

    @GetMapping
    public ModelConfigurationDto current() {
        return service.current();
    }

    @PostMapping("/chat-model")
    public ModelConfigurationDto setChatModel(@Valid @RequestBody SetChatModelRequest request) {
        return service.setActiveChatModel(request.model());
    }
}
