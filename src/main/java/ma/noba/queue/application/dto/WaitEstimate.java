package ma.noba.queue.application.dto;

/**
 * An honest wait estimate: a best guess plus an uncertainty range and a confidence level derived
 * from how much real history the provider has. Surfacing the range (rather than a single precise
 * number) avoids promising a wait time we cannot back up.
 */
public record WaitEstimate(
        int estimatedMinutes,
        int minMinutes,
        int maxMinutes,
        String confidence,
        String basis,
        int sampleSize
) {}
