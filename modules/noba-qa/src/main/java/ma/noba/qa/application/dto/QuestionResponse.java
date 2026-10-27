package ma.noba.qa.application.dto;

import java.time.Instant;

public record QuestionResponse(
        String id,
        String providerId,
        String userId,
        String question,
        String answer,
        Instant answeredAt,
        Instant createdAt,
        boolean answered
) {}
