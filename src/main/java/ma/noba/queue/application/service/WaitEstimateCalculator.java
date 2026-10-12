package ma.noba.queue.application.service;

import ma.noba.queue.application.dto.WaitEstimate;

/**
 * Turns a point estimate of the remaining wait into an honest range with a confidence level.
 * <p>
 * Confidence is driven by how much real history the provider has (completed visits with a measured
 * duration). The width of the range grows with uncertainty, and the number of people still ahead in
 * the line widens the upper bound to account for knock-on delays.
 */
public final class WaitEstimateCalculator {

    private WaitEstimateCalculator() {
    }

    public static WaitEstimate forMember(int estimatedMinutes, int sampleSize, int waitingAhead) {
        int est = Math.max(0, estimatedMinutes);
        int samples = Math.max(0, sampleSize);

        String confidence;
        if (samples >= 15) {
            confidence = "HIGH";
        } else if (samples >= 5) {
            confidence = "MEDIUM";
        } else {
            confidence = "LOW";
        }

        double spreadPct = switch (confidence) {
            case "HIGH" -> 0.20;
            case "MEDIUM" -> 0.40;
            default -> 0.75;
        };
        int baseSpread = Math.max(5, (int) Math.round(est * spreadPct));
        int extra = Math.max(0, waitingAhead) * 2;

        int min = Math.max(0, est - baseSpread);
        int max = est + baseSpread + extra;

        String basis = switch (confidence) {
            case "HIGH", "MEDIUM" -> "Based on " + samples + " recent visits";
            default -> samples == 0
                    ? "No recent history — rough estimate"
                    : "Limited history (" + samples + " visits) — may vary";
        };

        return new WaitEstimate(est, min, max, confidence, basis, samples);
    }
}
