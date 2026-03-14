package com.windforecast.forecast_monitoring_backend.util;

import com.windforecast.forecast_monitoring_backend.model.ForecastGeneration;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class ForecastFilterUtil {

    public static Optional<ForecastGeneration> getLatestValidForecast(
            LocalDateTime targetTime,
            List<ForecastGeneration> forecasts,
            int horizonHours) {
        if (targetTime == null) {
            return Optional.empty();
        }

        return getLatestValidForecast(targetTime.atOffset(ZoneOffset.UTC), forecasts, horizonHours);
    }

    public static Optional<ForecastGeneration> getLatestValidForecast(
            OffsetDateTime targetTime,
            List<ForecastGeneration> forecasts,
            int horizonHours) {
        if (targetTime == null || forecasts == null || horizonHours < 0) {
            return Optional.empty();
        }

        OffsetDateTime allowedPublishTime = targetTime.minusHours(horizonHours);

        return forecasts.stream()
                .filter(f -> f != null)
                .filter(f -> f.getStartTime() != null && f.getStartTime().toInstant().equals(targetTime.toInstant()))
                .filter(f -> f.getPublishTime() != null && !f.getPublishTime().toInstant().isAfter(allowedPublishTime.toInstant()))
                .max(Comparator.comparing(ForecastGeneration::getPublishTime));
    }
}
