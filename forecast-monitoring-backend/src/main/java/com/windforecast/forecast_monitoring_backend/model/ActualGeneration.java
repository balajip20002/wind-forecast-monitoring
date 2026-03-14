package com.windforecast.forecast_monitoring_backend.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.OffsetDateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ActualGeneration {

    @JsonAlias("startTime")
    private OffsetDateTime startTime;
    @JsonAlias({"quantity", "generation"})
    private double generation;
    @JsonAlias({"fuelType", "fueltype", "fuel"})
    private String fuelType;

    public ActualGeneration() {}

    public ActualGeneration(OffsetDateTime startTime, double generation) {
        this(startTime, generation, null);
    }

    public ActualGeneration(OffsetDateTime startTime, double generation, String fuelType) {
        this.startTime = startTime;
        this.generation = generation;
        this.fuelType = fuelType;
    }

    public OffsetDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(OffsetDateTime startTime) {
        this.startTime = startTime;
    }

    public double getGeneration() {
        return generation;
    }

    public void setGeneration(double generation) {
        this.generation = generation;
    }

    public String getFuelType() {
        return fuelType;
    }

    public void setFuelType(String fuelType) {
        this.fuelType = fuelType;
    }
}
