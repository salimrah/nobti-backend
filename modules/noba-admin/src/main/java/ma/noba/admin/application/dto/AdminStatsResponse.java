package ma.noba.admin.application.dto;

public record AdminStatsResponse(
        long totalUsers,
        long activeUsers,
        long suspendedUsers,
        long totalProviders,
        long pendingProviders,
        long activeQueues,
        long totalQueueMembers
) {}