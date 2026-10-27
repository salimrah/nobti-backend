package ma.noba.media.application.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ProviderMediaResponse {
    private String id;
    private String providerId;
    private String url;
    private String caption;
    private String category;
    private int mediaOrder;
    private String createdAt;
}
