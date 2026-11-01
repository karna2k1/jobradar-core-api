package com.jobradar.core.internal;

import com.jobradar.core.auth.User;
import com.jobradar.core.auth.UserRepository;
import com.jobradar.core.profile.Profile;
import com.jobradar.core.profile.ProfileRepository;
import com.jobradar.core.profile.dto.ProfileDto;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Service-to-service API (ROLE_SERVICE tokens only). Used by match-notify-service. */
@RestController
@RequestMapping("/internal")
@RequiredArgsConstructor
public class InternalController {
    private final UserRepository users;
    private final ProfileRepository profiles;

    public record MatchingProfile(Long userId, String email, String fullName, ProfileDto profile) {}

    /** Users who have at least one skill in their profile. */
    @GetMapping("/profiles")
    @Transactional(readOnly = true)
    public List<MatchingProfile> profiles() {
        Map<Long, Profile> byUser = profiles.findAll().stream()
                .filter(p -> !p.getSkills().isEmpty())
                .collect(Collectors.toMap(Profile::getUserId, Function.identity()));
        List<User> owners = users.findAllById(byUser.keySet());
        return owners.stream()
                .map(u -> new MatchingProfile(u.getId(), u.getEmail(), u.getFullName(), ProfileDto.from(byUser.get(u.getId()))))
                .toList();
    }
}
