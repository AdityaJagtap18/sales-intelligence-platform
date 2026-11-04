package com.nexora.platform.prompts;

import jakarta.validation.constraints.Pattern;

public record RatingRequest(@Pattern(regexp = "PASS|FAIL|UNRATED") String rating) {
}
