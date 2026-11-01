package com.jobradar.core.profile.dto;

import java.util.List;

public record ResumeUploadResponse(Long resumeId, String fileName, int characters, List<String> extractedSkills,
                                   boolean skillsMerged) {
}
