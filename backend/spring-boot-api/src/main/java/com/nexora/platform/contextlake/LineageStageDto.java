package com.nexora.platform.contextlake;

public record LineageStageDto(String stage, String system, String location, Long rowCount, String detail) {
}
