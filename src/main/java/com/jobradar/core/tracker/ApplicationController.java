package com.jobradar.core.tracker;

import com.jobradar.core.tracker.dto.ApplicationDto;
import com.jobradar.core.tracker.dto.CreateApplicationRequest;
import com.jobradar.core.tracker.dto.MoveRequest;
import com.jobradar.core.tracker.dto.UpdateApplicationRequest;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/applications")
@RequiredArgsConstructor
public class ApplicationController {
    private final ApplicationService service;

    @GetMapping
    public List<ApplicationDto> list(@AuthenticationPrincipal Long userId) {
        return service.list(userId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApplicationDto create(@AuthenticationPrincipal Long userId, @Valid @RequestBody CreateApplicationRequest req) {
        return service.create(userId, req);
    }

    @PatchMapping("/{id}")
    public ApplicationDto update(@AuthenticationPrincipal Long userId, @PathVariable Long id,
                                 @Valid @RequestBody UpdateApplicationRequest req) {
        return service.update(userId, id, req);
    }

    @PatchMapping("/{id}/move")
    public ApplicationDto move(@AuthenticationPrincipal Long userId, @PathVariable Long id,
                               @Valid @RequestBody MoveRequest req) {
        return service.move(userId, id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal Long userId, @PathVariable Long id) {
        service.delete(userId, id);
    }
}
