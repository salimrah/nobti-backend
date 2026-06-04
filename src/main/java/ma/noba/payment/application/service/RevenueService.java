package ma.noba.payment.application.service;

import lombok.RequiredArgsConstructor;
import ma.noba.payment.application.dto.RevenueSummaryResponse;
import ma.noba.payment.domain.model.PaymentStatus;
import ma.noba.payment.domain.model.PaymentTransaction;
import ma.noba.payment.domain.model.Payout;
import ma.noba.payment.domain.model.PayoutStatus;
import ma.noba.payment.domain.port.PaymentTransactionRepository;
import ma.noba.payment.domain.port.PayoutRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RevenueService {

    private final PaymentTransactionRepository transactionRepository;
    private final PayoutRepository payoutRepository;

    public RevenueSummaryResponse getRevenueSummary(UUID providerId) {
        ZoneId zone = ZoneId.of("UTC");
        LocalDate today = LocalDate.now(zone);
        LocalDate weekStart = today.with(DayOfWeek.MONDAY);
        LocalDate monthStart = today.withDayOfMonth(1);

        Instant todayStart = today.atStartOfDay(zone).toInstant();
        Instant weekStartInst = weekStart.atStartOfDay(zone).toInstant();
        Instant monthStartInst = monthStart.atStartOfDay(zone).toInstant();

        List<PaymentTransaction> allTx = transactionRepository.findByProviderIdAndStatus(providerId, PaymentStatus.SUCCEEDED);

        BigDecimal totalRevenue = BigDecimal.ZERO;
        BigDecimal todayRevenue = BigDecimal.ZERO;
        BigDecimal weekRevenue = BigDecimal.ZERO;
        BigDecimal monthRevenue = BigDecimal.ZERO;

        Map<LocalDate, List<PaymentTransaction>> groupedByDay = new HashMap<>();

        for (PaymentTransaction tx : allTx) {
            BigDecimal net = tx.getNetAmount() != null ? tx.getNetAmount() : tx.getAmount();
            totalRevenue = totalRevenue.add(net);

            LocalDate txDate = tx.getCreatedAt().atZone(zone).toLocalDate();
            groupedByDay.computeIfAbsent(txDate, k -> new ArrayList<>()).add(tx);

            if (tx.getCreatedAt().isAfter(todayStart)) {
                todayRevenue = todayRevenue.add(net);
            }
            if (tx.getCreatedAt().isAfter(weekStartInst)) {
                weekRevenue = weekRevenue.add(net);
            }
            if (tx.getCreatedAt().isAfter(monthStartInst)) {
                monthRevenue = monthRevenue.add(net);
            }
        }

        List<Payout> pendingPayouts = payoutRepository.findByProviderIdAndStatus(providerId, PayoutStatus.PENDING);
        BigDecimal pendingAmount = pendingPayouts.stream()
            .map(Payout::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<RevenueSummaryResponse.RevenueDay> dailyBreakdown = groupedByDay.entrySet().stream()
            .sorted(Map.Entry.comparingByKey(Comparator.reverseOrder()))
            .limit(30)
            .map(e -> {
                BigDecimal dayRevenue = e.getValue().stream()
                    .map(tx -> tx.getNetAmount() != null ? tx.getNetAmount() : tx.getAmount())
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
                return new RevenueSummaryResponse.RevenueDay(
                    e.getKey().toString(), dayRevenue, e.getValue().size()
                );
            })
            .toList();

        return new RevenueSummaryResponse(
            todayRevenue, (int) allTx.stream().filter(tx -> tx.getCreatedAt().isAfter(todayStart)).count(),
            weekRevenue, (int) allTx.stream().filter(tx -> tx.getCreatedAt().isAfter(weekStartInst)).count(),
            monthRevenue, (int) allTx.stream().filter(tx -> tx.getCreatedAt().isAfter(monthStartInst)).count(),
            totalRevenue, allTx.size(),
            pendingAmount,
            dailyBreakdown
        );
    }
}
