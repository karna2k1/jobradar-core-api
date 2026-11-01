package com.jobradar.core.profile.dto;

import java.time.Instant;

public record ResumeDto(Long id, String fileName, Instant uploadedAt, String contentText) {
}
