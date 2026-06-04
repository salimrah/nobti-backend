package ma.noba.loyalty.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CreateRewardRequest {
    @NotBlank
    private String name;

    private String description;

    @Positive
    private int pointsRequired;
}
