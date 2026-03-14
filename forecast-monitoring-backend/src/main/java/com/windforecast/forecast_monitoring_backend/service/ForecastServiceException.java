package com.windforecast.forecast_monitoring_backend.service;

public class ForecastServiceException extends RuntimeException {

    public ForecastServiceException(String message) {
        super(message);
    }

    public ForecastServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
