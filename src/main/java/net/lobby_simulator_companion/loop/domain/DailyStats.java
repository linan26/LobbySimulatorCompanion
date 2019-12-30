package net.lobby_simulator_companion.loop.domain;

import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * @author NickyRamone
 */
public class DailyStats extends PeriodStats {

    public DailyStats(PeriodStats other) {
        super(other);
    }

    public DailyStats(LocalDateTime now) {
        super(now);
    }

    @Override
    protected LocalDateTime getPeriodStart(LocalDateTime now) {
        return now.toLocalDate().atStartOfDay();
    }

    @Override
    protected LocalDateTime getPeriodEnd(LocalDateTime now) {
        return now.toLocalDate().atTime(LocalTime.MAX);
    }

    @Override
    public DailyStats clone() {
        return new DailyStats(this);
    }


}
