package com.jobradar.core.profile;

import com.jobradar.core.common.ApiException;
import com.jobradar.core.profile.dto.ProfileDto;
import com.jobradar.core.profile.dto.ResumeDto;
import com.jobradar.core.profile.dto.ResumeUploadResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class ProfileService {
    private static final byte[] PDF_MAGIC = "%PDF".getBytes(StandardCharsets.US_ASCII);

    private final ProfileRepository profiles;
    private final ResumeRepository resumes;
    private final PdfTextExtractor pdfTextExtractor;
    private final SkillExtractor skillExtractor;

    @Transactional(readOnly = true)
    public ProfileDto get(Long userId) {
        return ProfileDto.from(profiles.findById(userId).orElseGet(() -> new Profile(userId)));
    }

    @Transactional
    public ProfileDto update(Long userId, ProfileDto dto) {
        Profile p = profiles.findById(userId).orElseGet(() -> new Profile(userId));
        dto.applyTo(p);
        return ProfileDto.from(profiles.save(p));
    }

    @Transactional
    public ResumeUploadResponse uploadResume(Long userId, MultipartFile file, boolean mergeSkills) {
        byte[] bytes = readPdf(file);
        String text = pdfTextExtractor.extract(bytes).strip();
        if (text.isEmpty()) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "No text found in PDF (scanned image?)");
        }

        Resume r = new Resume();
        r.setUserId(userId);
        r.setFileName(file.getOriginalFilename() == null ? "resume.pdf" : file.getOriginalFilename());
        r.setContentText(text);
        resumes.save(r);

        List<String> skills = skillExtractor.extract(text);
        if (mergeSkills && !skills.isEmpty()) {
            Profile p = profiles.findById(userId).orElseGet(() -> new Profile(userId));
            p.setSkills(mergeIgnoreCase(p.getSkills(), skills));
            profiles.save(p);
        }
        return new ResumeUploadResponse(r.getId(), r.getFileName(), text.length(), skills, mergeSkills);
    }

    @Transactional(readOnly = true)
    public ResumeDto latestResume(Long userId) {
        return resumes.findFirstByUserIdOrderByUploadedAtDesc(userId)
                .map(r -> new ResumeDto(r.getId(), r.getFileName(), r.getUploadedAt(), r.getContentText()))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "No resume uploaded"));
    }

    private static byte[] readPdf(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "File is empty");
        }
        try {
            byte[] bytes = file.getBytes();
            if (bytes.length < PDF_MAGIC.length || !Arrays.equals(Arrays.copyOf(bytes, PDF_MAGIC.length), PDF_MAGIC)) {
                throw new ApiException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Only PDF resumes are supported");
            }
            return bytes;
        } catch (IOException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Could not read upload");
        }
    }

    private static List<String> mergeIgnoreCase(List<String> existing, List<String> extra) {
        List<String> merged = new ArrayList<>(existing);
        List<String> lower = new ArrayList<>(existing.stream().map(s -> s.toLowerCase(Locale.ROOT)).toList());
        for (String s : extra) {
            if (!lower.contains(s.toLowerCase(Locale.ROOT))) {
                merged.add(s);
                lower.add(s.toLowerCase(Locale.ROOT));
            }
        }
        return merged;
    }
}
