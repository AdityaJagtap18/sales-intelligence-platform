package com.nexora.platform.contextlake;

import java.util.List;

public record LineageDto(String table, List<LineageStageDto> stages) {
}
