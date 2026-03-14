package com.windforecast.forecast_monitoring_backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.windforecast.forecast_monitoring_backend.model.ActualGeneration;
import com.windforecast.forecast_monitoring_backend.model.ForecastGeneration;
import com.windforecast.forecast_monitoring_backend.util.ForecastFilterUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class ForecastService {

    private static final Logger log = LoggerFactory.getLogger(ForecastService.class);

    private static final String ACTUAL_API =
            "https://data.elexon.co.uk/bmrs/api/v1/datasets/FUELHH/stream";

    private static final String FORECAST_API =
            "https://data.elexon.co.uk/bmrs/api/v1/datasets/WINDFOR/stream";

    private static final String ACTUAL_RANGE_API =
            "https://data.elexon.co.uk/bmrs/api/v1/datasets/FUELHH";

    private static final String FORECAST_RANGE_API =
            "https://data.elexon.co.uk/bmrs/api/v1/datasets/WINDFOR";

    private static final DateTimeFormatter RESPONSE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ForecastService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public List<ActualGeneration> fetchActualGeneration() {
        ActualGeneration[] response = fetchDataset(ACTUAL_API, ActualGeneration[].class, "actual generation");

        return Arrays.stream(response)
                .filter(Objects::nonNull)
                .filter(this::isWindGeneration)
                .collect(Collectors.toMap(
                        ActualGeneration::getStartTime,
                        item -> item,
                        (left, right) -> new ActualGeneration(
                                left.getStartTime(),
                                left.getGeneration() + right.getGeneration(),
                                "WIND"
                        )
                ))
                .values()
                .stream()
                .sorted(Comparator.comparing(ActualGeneration::getStartTime))
                .toList();
    }

    public List<ForecastGeneration> fetchForecastGeneration() {
        return Arrays.asList(fetchDataset(FORECAST_API, ForecastGeneration[].class, "forecast generation"));
    }

    public List<Map<String, Object>> fetchForecastData(LocalDateTime start, LocalDateTime end, int horizon) {
        if (start == null || end == null) {
            throw new ForecastServiceException("Start time and end time are required");
        }
        if (end.isBefore(start)) {
            throw new ForecastServiceException("End time must be after or equal to start time");
        }
        if (horizon < 0 || horizon > 48) {
            throw new ForecastServiceException("Horizon must be between 0 and 48 hours");
        }

        OffsetDateTime rangeStart = start.atOffset(ZoneOffset.UTC);
        OffsetDateTime rangeEnd = end.atOffset(ZoneOffset.UTC);

        List<ActualGeneration> actualRecords = fetchActualGenerationForRange(start, end);
        List<ForecastGeneration> forecastRecords = fetchForecastGenerationForRange(start, end, horizon);

        log.info("Forecast data request start={}, end={}, horizon={}h", start, end, horizon);
        log.info("Raw dataset sizes - actual records: {}, forecast records: {}", actualRecords.size(), forecastRecords.size());

        List<ActualGeneration> actualGenerations = actualRecords.stream()
                .filter(item -> item.getStartTime() != null)
                .filter(item -> isWithinRange(item.getStartTime(), rangeStart, rangeEnd))
                .sorted(Comparator.comparing(ActualGeneration::getStartTime))
                .toList();

        List<ForecastGeneration> forecastGenerations = forecastRecords.stream()
                .filter(item -> item.getStartTime() != null && item.getPublishTime() != null)
                .filter(item -> isWithinRange(item.getStartTime(), rangeStart, rangeEnd))
                .toList();

        log.info("After date filtering - actual records: {}, forecast records: {}", actualGenerations.size(), forecastGenerations.size());

        int horizonMatchedForecasts = 0;
        List<Map<String, Object>> results = new java.util.ArrayList<>();
        for (ActualGeneration actual : actualGenerations) {
            Map<String, Object> point = buildForecastDataPoint(actual, forecastGenerations, horizon);
            if (point != null) {
                horizonMatchedForecasts++;
                results.add(point);
            }
        }

        log.info("After horizon filtering - matched records: {}", horizonMatchedForecasts);
        return results;
    }

    private boolean isWindGeneration(ActualGeneration generation) {
        if (generation.getStartTime() == null) {
            return false;
        }

        String fuelType = generation.getFuelType();
        return fuelType != null && fuelType.toUpperCase(Locale.ROOT).contains("WIND");
    }

    private Map<String, Object> buildForecastDataPoint(
            ActualGeneration actual,
            List<ForecastGeneration> forecasts,
            int horizon
    ) {
        OffsetDateTime targetTime = actual.getStartTime();

        return ForecastFilterUtil.getLatestValidForecast(targetTime, forecasts, horizon)
                .map(forecast -> {
                    Map<String, Object> point = new LinkedHashMap<>();
                    point.put("time", RESPONSE_TIME_FORMATTER.format(targetTime.atZoneSameInstant(ZoneOffset.UTC)));
                    point.put("actual", actual.getGeneration());
                    point.put("forecast", forecast.getGeneration());
                    return point;
                })
                .orElse(null);
    }

    private List<ActualGeneration> fetchActualGenerationForRange(LocalDateTime start, LocalDateTime end) {
        URI uri = UriComponentsBuilder.fromUriString(ACTUAL_RANGE_API)
                .queryParam("settlementDateFrom", start.toLocalDate())
                .queryParam("settlementDateTo", end.toLocalDate())
                .queryParam("format", "json")
                .build(true)
                .toUri();

        ActualGeneration[] response = fetchWrappedDataset(uri, ActualGeneration[].class, "historical actual generation");
        return Arrays.stream(response)
                .filter(Objects::nonNull)
                .filter(this::isWindGeneration)
                .collect(Collectors.toMap(
                        item -> item.getStartTime().toInstant(),
                        item -> item,
                        (left, right) -> new ActualGeneration(
                                left.getStartTime(),
                                left.getGeneration() + right.getGeneration(),
                                "WIND"
                        )
                ))
                .values()
                .stream()
                .sorted(Comparator.comparing(ActualGeneration::getStartTime))
                .toList();
    }

    private List<ForecastGeneration> fetchForecastGenerationForRange(LocalDateTime start, LocalDateTime end, int horizon) {
        OffsetDateTime publishFrom = start.atOffset(ZoneOffset.UTC).minusHours(Math.max(horizon, 48));
        OffsetDateTime publishTo = end.atOffset(ZoneOffset.UTC);

        URI uri = UriComponentsBuilder.fromUriString(FORECAST_RANGE_API)
                .queryParam("publishDateTimeFrom", publishFrom)
                .queryParam("publishDateTimeTo", publishTo)
                .queryParam("format", "json")
                .build(true)
                .toUri();

        ForecastGeneration[] response = fetchWrappedDataset(uri, ForecastGeneration[].class, "historical forecast generation");
        return Arrays.stream(response)
                .filter(Objects::nonNull)
                .sorted(Comparator
                        .comparing(ForecastGeneration::getStartTime, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(ForecastGeneration::getPublishTime, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    private boolean isWithinRange(OffsetDateTime timestamp, OffsetDateTime start, OffsetDateTime end) {
        return !timestamp.toInstant().isBefore(start.toInstant()) && !timestamp.toInstant().isAfter(end.toInstant());
    }

    private <T> T fetchDataset(String url, Class<T> responseType, String datasetName) {
        try {
            RequestEntity<Void> request = RequestEntity.get(URI.create(url))
                    .accept(MediaType.APPLICATION_JSON)
                    .build();

            ResponseEntity<T> response = restTemplate.exchange(request, responseType);

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new ForecastServiceException(
                        "Upstream service returned status " + response.getStatusCode().value() + " for " + datasetName
                );
            }

            MediaType contentType = response.getHeaders().getContentType();
            if (contentType != null && !MediaType.APPLICATION_JSON.isCompatibleWith(contentType)) {
                throw new ForecastServiceException(
                        "Upstream service returned unsupported content type " + contentType + " for " + datasetName
                );
            }

            T body = response.getBody();
            if (Objects.isNull(body)) {
                log.warn("Upstream service returned an empty body for {}", datasetName);
                return emptyPayload(responseType);
            }

            return body;
        } catch (RestClientException exception) {
            log.error("Failed to fetch {} from {}", datasetName, url, exception);
            throw new ForecastServiceException("Unable to fetch " + datasetName + " from upstream service", exception);
        }
    }

    private <T> T fetchWrappedDataset(URI uri, Class<T> responseType, String datasetName) {
        try {
            RequestEntity<Void> request = RequestEntity.get(uri)
                    .accept(MediaType.APPLICATION_JSON)
                    .build();

            ResponseEntity<String> response = restTemplate.exchange(request, String.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new ForecastServiceException(
                        "Upstream service returned status " + response.getStatusCode().value() + " for " + datasetName
                );
            }

            String body = response.getBody();
            if (body == null || body.isBlank()) {
                log.warn("Upstream service returned an empty body for {}", datasetName);
                return emptyPayload(responseType);
            }

            JsonNode root = objectMapper.readTree(body);
            JsonNode dataNode = root.path("data");
            if (!dataNode.isArray()) {
                throw new ForecastServiceException("Upstream service returned an unexpected payload for " + datasetName);
            }

            return convertWrappedData(dataNode, responseType);
        } catch (ForecastServiceException exception) {
            throw exception;
        } catch (Exception exception) {
            log.error("Failed to fetch {} from {}", datasetName, uri, exception);
            throw new ForecastServiceException("Unable to fetch " + datasetName + " from upstream service", exception);
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T convertWrappedData(JsonNode dataNode, Class<T> responseType) {
        if (responseType == ActualGeneration[].class) {
            ActualGeneration[] items = new ActualGeneration[dataNode.size()];
            for (int index = 0; index < dataNode.size(); index++) {
                JsonNode node = dataNode.get(index);
                items[index] = new ActualGeneration(
                        OffsetDateTime.parse(node.path("startTime").asText()),
                        node.path("generation").asDouble(node.path("quantity").asDouble()),
                        node.path("fuelType").asText(null)
                );
            }
            return (T) items;
        }

        if (responseType == ForecastGeneration[].class) {
            ForecastGeneration[] items = new ForecastGeneration[dataNode.size()];
            for (int index = 0; index < dataNode.size(); index++) {
                JsonNode node = dataNode.get(index);
                items[index] = new ForecastGeneration(
                        OffsetDateTime.parse(node.path("startTime").asText()),
                        OffsetDateTime.parse(node.path("publishTime").asText()),
                        node.path("generation").asDouble(node.path("quantity").asDouble())
                );
            }
            return (T) items;
        }

        throw new ForecastServiceException("Unsupported wrapped dataset type: " + responseType.getSimpleName());
    }

    @SuppressWarnings("unchecked")
    private <T> T emptyPayload(Class<T> responseType) {
        if (responseType == ActualGeneration[].class) {
            return (T) new ActualGeneration[0];
        }
        if (responseType == ForecastGeneration[].class) {
            return (T) new ForecastGeneration[0];
        }
        return (T) Collections.emptyList();
    }
}
