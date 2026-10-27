package ma.noba.statement.application.dto;

import java.math.BigDecimal;
import java.util.List;

public record StatementResponse(
        String userId,
        BigDecimal totalSpent,
        int totalPoints,
        List<StatementEntry> entries
) {}
