package ma.noba.payment.application.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@AllArgsConstructor
public class RevenueSummaryResponse {
    private BigDecimal todayRevenue;
    private int todayTransactions;
    private BigDecimal thisWeekRevenue;
    private int thisWeekTransactions;
    private BigDecimal thisMonthRevenue;
    private int thisMonthTransactions;
    private BigDecimal totalRevenue;
    private int totalTransactions;
    private BigDecimal pendingPayouts;
    private List<RevenueDay> dailyBreakdown;

    @Data
    @AllArgsConstructor
    public static class RevenueDay {
        private String date;
        private BigDecimal revenue;
        private int transactions;
    }
}
