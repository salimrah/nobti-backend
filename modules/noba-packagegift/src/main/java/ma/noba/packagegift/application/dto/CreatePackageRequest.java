package ma.noba.packagegift.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
public class CreatePackageRequest {
    @NotBlank
    private String name;

    private String description;

    @NotNull
    @Positive
    private BigDecimal price;

    @NotNull
    @Positive
    private BigDecimal originalPrice;

    @Positive
    private int validDays = 30;

    private List<ServiceItemRequest> serviceItems;

    @Data
    public static class ServiceItemRequest {
        @NotNull
        private UUID serviceId;

        @Positive
        private int quantity = 1;
    }
}
