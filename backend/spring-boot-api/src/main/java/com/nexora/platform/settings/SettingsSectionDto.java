package com.nexora.platform.settings;

import java.util.List;

public record SettingsSectionDto(String section, List<SettingEntryDto> entries) {

    public record SettingEntryDto(String key, String value) {
    }
}
