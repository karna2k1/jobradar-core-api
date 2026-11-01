package com.jobradar.core.tracker.dto;

import com.jobradar.core.tracker.Stage;
import jakarta.validation.constraints.NotNull;

/** position null = append to the end of the target column. */
public record MoveRequest(@NotNull Stage stage, Double position) {
}
