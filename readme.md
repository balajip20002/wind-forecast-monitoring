# Wind Power Forecast Monitoring Dashboard

## Overview

This project implements a **Wind Power Forecast Monitoring application** that compares forecasted wind generation with actual generation values.

The system helps users analyze the **accuracy of wind power forecasts** and understand the reliability of wind energy.

The project consists of:

* **Spring Boot backend** for data processing and API endpoints
* **React frontend** for interactive visualization
* **Jupyter notebook analysis** for forecast error evaluation

---

# Architecture

Frontend → React Dashboard
Backend → Spring Boot API
Data Source → BMRS Elexon Wind Generation Dataset

```
React UI → Spring Boot API → BMRS API
```

---

# Features

## Forecast Monitoring Dashboard

The web dashboard allows users to:

* Select **Start Time and End Time**
* Adjust **Forecast Horizon (0–48 hours)**
* Visualize **Actual vs Forecast generation**
* Dynamically reload chart data

Chart shows:

* Actual wind generation
* Forecasted wind generation

---

# Forecast Filtering Logic

For each target time:

```
allowedPublishTime = targetTime - horizonHours
```

The system selects:

* forecasts where `publishTime <= allowedPublishTime`
* the **latest forecast before that time**

This ensures only forecasts available before the horizon are evaluated.

---

# Technology Stack

Backend

* Java
* Spring Boot
* REST APIs
* Maven

Frontend

* React
* JavaScript
* SVG chart rendering

Analysis

* Python
* Pandas
* Matplotlib
* Jupyter Notebook

---

# Running the Application

## Backend

Navigate to backend folder:

```
cd forecast-monitoring-backend
```

Run:

```
mvn spring-boot:run
```

Backend runs at:

```
http://localhost:8080
```

---

## Frontend

Navigate to frontend folder:

```
cd forecast-monitoring-frontend
```

Install dependencies:

```
npm install
```

Run frontend:

```
npm start
```

Frontend runs at:

```
http://localhost:3000
```

---

# API Endpoints

### Get Actual Wind Generation

```
GET /forecast/actual
```

---

### Get Forecast Wind Generation

```
GET /forecast/predicted
```

---

### Get Filtered Forecast Monitoring Data

```
GET /forecast/data?start=&end=&horizon=
```

Example:

```
/forecast/data?start=2024-01-13T00:00&end=2024-01-14T00:00&horizon=6
```

Response:

```
[
  {
    "time": "2024-01-13T01:00",
    "actual": 6801,
    "forecast": 7938
  }
]
```

---

# Forecast Analysis Notebook

The Jupyter notebook analyzes forecast performance.

Metrics calculated:

* Mean Absolute Error (MAE)
* Median Error
* P99 Error
* Error vs Forecast Horizon
* Error vs Time of Day

Wind reliability is estimated using percentile analysis of historical generation.

Notebook location:

```
analysis/wind_forecast_analysis.ipynb
```

---

# Deployment

Frontend can be deployed using **Vercel**.

Backend can be deployed using:

* Render
* Railway
* Heroku

---

# AI Tools Used

AI tools were used to assist with:

* debugging
* UI generation
* analysis code suggestions

All logic and architecture decisions were validated manually.

---

# Author

Balaji P
