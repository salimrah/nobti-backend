package ma.noba.media.application.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AddMediaRequest {
    @NotBlank
    private String url;

    private String mediaType;
}
