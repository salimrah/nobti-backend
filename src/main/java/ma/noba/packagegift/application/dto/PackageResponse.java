package ma.noba.packagegift.application.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@AllArgsConstructor
public class PackageResponse {
    private String id;
    private String providerId;
    private String name;
    private String description;
    private BigDecimal price;
    private BigDecimal originalPrice;
    private int validDays;
    private boolean active;
    private List<ServiceItemResponse> serviceItems;

    @Data
    @AllArgsConstructor
    public static class ServiceItemResponse {
        private String serviceId;
        private int quantity;
    }
}
