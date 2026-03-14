package com.windforecast.forecast_monitoring_backend.util;

import com.windforecast.forecast_monitoring_backend.model.ForecastGeneration;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ForecastFilterUtilTest {

    @Test
    void getLatestValidForecastReturnsNewestAllowedPublishTime() {
        LocalDateTime targetTime = LocalDateTime.parse("2026-03-13T12:00:00");
        ForecastGeneration older = new ForecastGeneration(
                OffsetDateTime.parse("2026-03-13T12:00:00Z"),
                OffsetDateTime.parse("2026-03-13T09:00:00Z"),
                95.0
        );
        ForecastGeneration latestAllowed = new ForecastGeneration(
                OffsetDateTime.parse("2026-03-13T12:00:00Z"),
                OffsetDateTime.parse("2026-03-13T10:00:00Z"),
                101.0
        );
        ForecastGeneration tooLate = new ForecastGeneration(
                OffsetDateTime.parse("2026-03-13T12:00:00Z"),
                OffsetDateTime.parse("2026-03-13T11:30:00Z"),
                110.0
        );

        Optional<ForecastGeneration> result = ForecastFilterUtil.getLatestValidForecast(
                targetTime,
                List.of(older, latestAllowed, tooLate),
                2
        );

        assertTrue(result.isPresent());
        assertEquals(latestAllowed, result.get());
    }

    @Test
    void getLatestValidForecastReturnsEmptyWhenNothingMatchesTargetTime() {
        LocalDateTime targetTime = LocalDateTime.parse("2026-03-13T12:00:00");
        ForecastGeneration otherTime = new ForecastGeneration(
                OffsetDateTime.parse("2026-03-13T13:00:00Z"),
                OffsetDateTime.parse("2026-03-13T10:00:00Z"),
                101.0
        );

        Optional<ForecastGeneration> result = ForecastFilterUtil.getLatestValidForecast(
                targetTime,
                List.of(otherTime),
                2
        );

        assertTrue(result.isEmpty());
    }
}
