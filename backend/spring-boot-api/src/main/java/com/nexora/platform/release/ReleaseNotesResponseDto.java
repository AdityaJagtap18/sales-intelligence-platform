package com.nexora.platform.release;

import java.util.List;

public record ReleaseNotesResponseDto(String currentVersion, List<ReleaseNoteDto> notes) {
}
