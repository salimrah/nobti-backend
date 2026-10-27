package ma.noba.packagegift.application.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class UpdatePackageRequest {
    private String name;
    private String description;
    private BigDecimal price;
    private BigDecimal originalPrice;
    private Integer validDays;
}
