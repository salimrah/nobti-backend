package ma.noba.qa.domain.event;

import ma.noba.shared.domain.event.BaseDomainEvent;

import java.util.UUID;

public class QuestionAskedEvent extends BaseDomainEvent {

    private final UUID questionId;
    private final UUID providerId;
    private final UUID userId;

    public QuestionAskedEvent(UUID questionId, UUID providerId, UUID userId) {
        this.questionId = questionId;
        this.providerId = providerId;
        this.userId = userId;
    }

    public UUID questionId() { return questionId; }
    public UUID providerId() { return providerId; }
    public UUID userId() { return userId; }

    @Override
    public String eventType() { return "qa.question.asked"; }
}
