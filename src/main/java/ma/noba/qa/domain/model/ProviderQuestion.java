package ma.noba.qa.domain.model;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "provider_qa")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ProviderQuestion {

    @Id
    @GeneratedValue
    @EqualsAndHashCode.Include
    private UUID id;

    @Column(nullable = false)
    private UUID providerId;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String question;

    @Column(columnDefinition = "TEXT")
    private String answer;

    private Instant answeredAt;

    @Column(nullable = false)
    private Instant createdAt;

    public ProviderQuestion(UUID providerId, UUID userId, String question) {
        this.providerId = providerId;
        this.userId = userId;
        this.question = question;
        this.createdAt = Instant.now();
    }

    public void answer(String answer) {
        this.answer = answer;
        this.answeredAt = Instant.now();
    }

    public boolean isAnswered() {
        return answer != null && answeredAt != null;
    }
}
