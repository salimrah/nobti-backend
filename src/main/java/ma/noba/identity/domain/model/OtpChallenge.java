package ma.noba.identity.domain.model;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class OtpChallenge {
    private String id;
    private String target; // phone or email
    private OtpChannel channel;
    private String purpose; // LOGIN, SIGNUP, RESET, STEP_UP
    private String codeHash;
    private int attempts;
    private int maxAttempts;
    private OtpChallengeStatus status;
}