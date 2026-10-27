package ma.noba.media.application.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AddProviderMediaRequest {
    @NotBlank
    private String url;

    private String caption;

    private String category;

    private Integer mediaOrder;
}
