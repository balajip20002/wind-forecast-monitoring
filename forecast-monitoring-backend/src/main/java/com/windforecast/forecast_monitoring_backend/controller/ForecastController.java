package com.windforecast.forecast_monitoring_backend.controller;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.format.annotation.DateTimeFormat;

import com.windforecast.forecast_monitoring_backend.model.ActualGeneration;
import com.windforecast.forecast_monitoring_backend.model.ForecastGeneration;
import com.windforecast.forecast_monitoring_backend.service.ForecastService;

@CrossOrigin(origins = "http://localhost:3000")
@RestController
@RequestMapping("/forecast")
public class ForecastController {

    private final ForecastService forecastService;

    public ForecastController(ForecastService forecastService) {
        this.forecastService = forecastService;
    }

    @GetMapping("/actual")
    public List<ActualGeneration> getActual() {
        return forecastService.fetchActualGeneration();
    }

    @GetMapping("/predicted")
    public List<ForecastGeneration> getForecast() {
        return forecastService.fetchForecastGeneration();
    }

    @GetMapping("/data")
    public List<Map<String, Object>> getForecastData(
            @RequestParam("start") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam("end") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end,
            @RequestParam("horizon") int horizon
    ) {
        return forecastService.fetchForecastData(start, end, horizon);
    }

    @GetMapping("/test")
    public String test() {
        return "Forecast Monitoring Backend Running";
    }
}
