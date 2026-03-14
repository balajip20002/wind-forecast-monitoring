package com.windforecast.forecast_monitoring_backend.service;

import com.windforecast.forecast_monitoring_backend.model.ActualGeneration;
import com.windforecast.forecast_monitoring_backend.model.ForecastGeneration;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ForecastServiceTest {

    @Test
    void fetchActualGenerationReturnsEmptyListWhenApiReturnsNull() {
        ForecastService forecastService = new ForecastService(new StubRestTemplate(
                "https://data.elexon.co.uk/bmrs/api/v1/datasets/FUELHH/stream",
                ActualGeneration[].class,
                null
        ));

        List<ActualGeneration> result = forecastService.fetchActualGeneration();

        assertTrue(result.isEmpty());
    }

    @Test
    void fetchActualGenerationKeepsOnlyWindRowsAndAggregatesByStartTime() {
        ForecastService forecastService = new ForecastService(new StubRestTemplate(
                        "https://data.elexon.co.uk/bmrs/api/v1/datasets/FUELHH/stream",
                        ActualGeneration[].class,
                        new ActualGeneration[]{
                        new ActualGeneration(OffsetDateTime.parse("2026-03-13T10:00:00Z"), 100.0, "WIND"),
                        new ActualGeneration(OffsetDateTime.parse("2026-03-13T10:00:00Z"), 25.0, "wind offshore"),
                        new ActualGeneration(OffsetDateTime.parse("2026-03-13T10:00:00Z"), 500.0, "GAS"),
                        new ActualGeneration(OffsetDateTime.parse("2026-03-13T10:30:00Z"), 80.0, "WIND")
                }
        ));

        List<ActualGeneration> result = forecastService.fetchActualGeneration();

        assertEquals(2, result.size());
        assertEquals(OffsetDateTime.parse("2026-03-13T10:00:00Z"), result.get(0).getStartTime());
        assertEquals(125.0, result.get(0).getGeneration());
        assertEquals(OffsetDateTime.parse("2026-03-13T10:30:00Z"), result.get(1).getStartTime());
        assertEquals(80.0, result.get(1).getGeneration());
    }

    @Test
    void fetchForecastGenerationReturnsApiPayload() {
        ForecastGeneration record = new ForecastGeneration(
                OffsetDateTime.parse("2026-03-13T10:00:00Z"),
                OffsetDateTime.parse("2026-03-13T09:00:00Z"),
                120.5
        );
        ForecastService forecastService = new ForecastService(new StubRestTemplate(
                "https://data.elexon.co.uk/bmrs/api/v1/datasets/WINDFOR/stream",
                ForecastGeneration[].class,
                new ForecastGeneration[]{record}
        ));

        List<ForecastGeneration> result = forecastService.fetchForecastGeneration();

        assertEquals(1, result.size());
        assertEquals(record, result.get(0));
    }

    @Test
    void fetchActualGenerationThrowsHelpfulExceptionWhenUpstreamReturnsHtml() {
        ForecastService forecastService = new ForecastService(new StubRestTemplate(
                "https://data.elexon.co.uk/bmrs/api/v1/datasets/FUELHH/stream",
                ActualGeneration[].class,
                new ActualGeneration[0],
                MediaType.TEXT_HTML
        ));

        ForecastServiceException exception = assertThrows(
                ForecastServiceException.class,
                forecastService::fetchActualGeneration
        );

        assertTrue(exception.getMessage().contains("unsupported content type"));
    }

    private static final class StubRestTemplate extends RestTemplate {

        private final String expectedUrl;
        private final Class<?> expectedResponseType;
        private final Object response;
        private final MediaType contentType;

        private StubRestTemplate(String expectedUrl, Class<?> expectedResponseType, Object response) {
            this(expectedUrl, expectedResponseType, response, MediaType.APPLICATION_JSON);
        }

        private StubRestTemplate(
                String expectedUrl,
                Class<?> expectedResponseType,
                Object response,
                MediaType contentType
        ) {
            this.expectedUrl = expectedUrl;
            this.expectedResponseType = expectedResponseType;
            this.response = response;
            this.contentType = contentType;
        }

        @Override
        public <T> ResponseEntity<T> exchange(RequestEntity<?> requestEntity, Class<T> responseType) {
            assertEquals(expectedUrl, requestEntity.getUrl().toString());
            assertEquals(expectedResponseType, responseType);
            assertEquals(List.of(MediaType.APPLICATION_JSON), requestEntity.getHeaders().getAccept());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(contentType);

            return new ResponseEntity<>(responseType.cast(response), headers, HttpStatus.OK);
        }
    }
}
