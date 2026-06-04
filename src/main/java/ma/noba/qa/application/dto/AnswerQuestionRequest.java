package ma.noba.qa.application.dto;

import jakarta.validation.constraints.NotBlank;

public record AnswerQuestionRequest(
        @NotBlank String answer
) {}
