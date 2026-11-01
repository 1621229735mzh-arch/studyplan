package com.kaoyan.study.review.service;

import com.kaoyan.study.review.entity.MasteryResult;

/**
 * Day-granularity FSRS4Anki v5.0.0 memory model, retention 0.9, deterministic (no fuzz).
 * Equations/default weights: https://github.com/open-spaced-repetition/fsrs4anki/blob/v5.0.0/fsrs4anki_scheduler.js
 * Upstream MIT notice: docs/licenses/fsrs4anki-MIT.txt.
 * No fabricated initial rating: the first real retrieval response initializes memory.
 * This adapter has no minute-scale learning steps; a failure is retried on the next study day.
 */
public final class FsrsScheduler {
    public static final String VERSION = "FSRS4ANKI_5_0_0";
    private static final double[] W = {0.41,1.18,3.04,15.24,7.14,0.64,1.00,0.06,1.65,0.17,1.11,2.02,0.09,0.30,2.12,0.24,2.94,0.48,0.64};
    private static final double FACTOR = Math.pow(0.9, -2) - 1;
    private FsrsScheduler() {}
    public record Memory(double stability, double difficulty) {}
    public record Result(Memory memory, int intervalDays) {}

    public static Result review(Memory previous, int elapsedDays, MasteryResult response) {
        if (response == null || elapsedDays < 0) throw new IllegalArgumentException("Invalid retrieval response/date");
        int grade = switch (response) { case FORGOT -> 1; case VAGUE -> 2; case MASTERED -> 3; };
        if (previous == null) {
            var initial = new Memory(W[grade - 1], difficulty(W[4] - Math.exp(W[5] * (grade - 1)) + 1));
            return new Result(initial, grade == 1 ? 1 : interval(initial.stability()));
        }
        double s = previous.stability(), d = previous.difficulty();
        if (!Double.isFinite(s) || s <= 0 || !Double.isFinite(d) || d < 1 || d > 10)
            throw new IllegalArgumentException("Invalid FSRS memory");
        double retrieval = Math.pow(1 + FACTOR * elapsedDays / s, -0.5);
        double nextS = grade == 1
                ? Math.min(s, W[11] * Math.pow(d, -W[12]) * (Math.pow(s + 1, W[13]) - 1) * Math.exp((1 - retrieval) * W[14]))
                : recall(s, d, retrieval, grade);
        double nextD = difficulty(W[7] * W[4] + (1 - W[7]) * (d - W[6] * (grade - 3)));
        nextS = Math.max(0.01, round(nextS));
        int days = grade == 1 ? 1 : interval(nextS);
        if (grade == 3) {
            int hard = interval(round(recall(s, d, retrieval, 2)));
            days = Math.min(36500, Math.max(days, Math.min(hard, days) + 1));
        }
        return new Result(new Memory(nextS, nextD), days);
    }
    private static double recall(double s, double d, double r, int grade) {
        return s * (1 + Math.exp(W[8]) * (11 - d) * Math.pow(s, -W[9])
                * Math.expm1((1 - r) * W[10]) * (grade == 2 ? W[15] : 1));
    }
    private static int interval(double stability) { return (int) Math.min(36500, Math.max(1, Math.round(stability))); }
    private static double difficulty(double value) { return Math.min(10, Math.max(1, round(value))); }
    private static double round(double value) { return Math.round(value * 100) / 100.0; }
}
