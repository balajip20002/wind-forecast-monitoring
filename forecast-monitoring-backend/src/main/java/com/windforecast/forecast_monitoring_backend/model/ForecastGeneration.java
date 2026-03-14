package com.windforecast.forecast_monitoring_backend.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.OffsetDateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ForecastGeneration {

    @JsonAlias("startTime")
    private OffsetDateTime startTime;
    @JsonAlias("publishTime")
    private OffsetDateTime publishTime;
    @JsonAlias({"quantity", "generation"})
    private double generation;

    public ForecastGeneration() {}

    public ForecastGeneration(OffsetDateTime startTime, OffsetDateTime publishTime, double generation) {
        this.startTime = startTime;
        this.publishTime = publishTime;
        this.generation = generation;
    }

    public OffsetDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(OffsetDateTime startTime) {
        this.startTime = startTime;
    }

    public OffsetDateTime getPublishTime() {
        return publishTime;
    }

    public void setPublishTime(OffsetDateTime publishTime) {
        this.publishTime = publishTime;
    }

    public double getGeneration() {
        return generation;
    }

    public void setGeneration(double generation) {
        this.generation = generation;
    }
}
