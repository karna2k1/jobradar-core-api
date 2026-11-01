package com.jobradar.core.profile;

import com.jobradar.core.profile.dto.ProfileDto;
import com.jobradar.core.profile.dto.ResumeDto;
import com.jobradar.core.profile.dto.ResumeUploadResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/profile")
@RequiredArgsConstructor
public class ProfileController {
    private final ProfileService service;

    @GetMapping
    public ProfileDto get(@AuthenticationPrincipal Long userId) {
        return service.get(userId);
    }

    @PutMapping
    public ProfileDto update(@AuthenticationPrincipal Long userId, @Valid @RequestBody ProfileDto dto) {
        return service.update(userId, dto);
    }

    @PostMapping(value = "/resume", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ResumeUploadResponse uploadResume(@AuthenticationPrincipal Long userId,
                                             @RequestParam("file") MultipartFile file,
                                             @RequestParam(defaultValue = "true") boolean mergeSkills) {
        return service.uploadResume(userId, file, mergeSkills);
    }

    @GetMapping("/resume/latest")
    public ResumeDto latestResume(@AuthenticationPrincipal Long userId) {
        return service.latestResume(userId);
    }
}
