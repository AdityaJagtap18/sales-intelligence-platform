package com.nexora.platform.release;

import java.util.List;

public record ReleaseNoteDto(String version, String title, List<String> added, List<String> changed,
        List<String> fixed) {
}
