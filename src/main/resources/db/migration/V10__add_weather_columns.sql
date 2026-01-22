-- V10: Add weather columns to activities table
-- Records weather data at the time of running activity

ALTER TABLE activities ADD COLUMN weather_temp INT NULL;           -- Temperature in Celsius
ALTER TABLE activities ADD COLUMN weather_humidity INT NULL;       -- Humidity percentage
ALTER TABLE activities ADD COLUMN weather_wind_speed INT NULL;     -- Wind speed (m/s × 10 for decimal precision)
ALTER TABLE activities ADD COLUMN weather_precip_type VARCHAR(10) NULL;  -- Precipitation type: NONE, RAIN, SLEET, SNOW
ALTER TABLE activities ADD COLUMN weather_sky VARCHAR(10) NULL;    -- Sky condition: CLEAR, PARTLY_CLOUDY, CLOUDY

COMMENT ON COLUMN activities.weather_temp IS 'Temperature in Celsius at the time of activity';
COMMENT ON COLUMN activities.weather_humidity IS 'Humidity percentage at the time of activity';
COMMENT ON COLUMN activities.weather_wind_speed IS 'Wind speed in m/s × 10 for decimal precision';
COMMENT ON COLUMN activities.weather_precip_type IS 'Precipitation type: NONE, RAIN, SLEET, SNOW';
COMMENT ON COLUMN activities.weather_sky IS 'Sky condition: CLEAR, PARTLY_CLOUDY, CLOUDY';
