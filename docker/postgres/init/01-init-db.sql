-- Create database for real-time data pipeline
-- This script will run automatically when PostgreSQL container starts

-- Create extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pg_stat_statements";

-- Create schemas
CREATE SCHEMA IF NOT EXISTS events;
CREATE SCHEMA IF NOT EXISTS analytics;
CREATE SCHEMA IF NOT EXISTS monitoring;

-- Create tables for event storage
CREATE TABLE IF NOT EXISTS events.transactions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    transaction_id VARCHAR(100) NOT NULL UNIQUE,
    user_id VARCHAR(50) NOT NULL,
    amount DECIMAL(15,2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    transaction_type VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    merchant_id VARCHAR(50),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMP WITH TIME ZONE,
    metadata JSONB
);

CREATE TABLE IF NOT EXISTS events.iot_sensors (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    sensor_id VARCHAR(50) NOT NULL,
    device_type VARCHAR(30) NOT NULL,
    location VARCHAR(100),
    temperature DECIMAL(5,2),
    humidity DECIMAL(5,2),
    pressure DECIMAL(8,2),
    battery_level INTEGER CHECK (battery_level >= 0 AND battery_level <= 100),
    signal_strength INTEGER,
    timestamp TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    raw_data JSONB
);

CREATE TABLE IF NOT EXISTS events.system_logs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    log_level VARCHAR(10) NOT NULL,
    service_name VARCHAR(50) NOT NULL,
    message TEXT NOT NULL,
    exception_stack TEXT,
    request_id VARCHAR(100),
    user_id VARCHAR(50),
    session_id VARCHAR(100),
    ip_address INET,
    user_agent TEXT,
    timestamp TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    additional_data JSONB
);

-- Create indexes for better query performance
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_transactions_created_at ON events.transactions(created_at);
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_transactions_user_id ON events.transactions(user_id);
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_transactions_status ON events.transactions(status);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_iot_sensors_timestamp ON events.iot_sensors(timestamp);
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_iot_sensors_sensor_id ON events.iot_sensors(sensor_id);
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_iot_sensors_device_type ON events.iot_sensors(device_type);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_system_logs_timestamp ON events.system_logs(timestamp);
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_system_logs_service ON events.system_logs(service_name);
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_system_logs_level ON events.system_logs(log_level);

-- Create analytics tables for aggregated data
CREATE TABLE IF NOT EXISTS analytics.hourly_transaction_stats (
    id SERIAL PRIMARY KEY,
    hour_bucket TIMESTAMP WITH TIME ZONE NOT NULL,
    total_transactions INTEGER NOT NULL DEFAULT 0,
    total_amount DECIMAL(15,2) NOT NULL DEFAULT 0,
    avg_amount DECIMAL(15,2) NOT NULL DEFAULT 0,
    successful_transactions INTEGER NOT NULL DEFAULT 0,
    failed_transactions INTEGER NOT NULL DEFAULT 0,
    unique_users INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(hour_bucket)
);

-- Create monitoring tables
CREATE TABLE IF NOT EXISTS monitoring.kafka_consumer_lag (
    id SERIAL PRIMARY KEY,
    consumer_group VARCHAR(100) NOT NULL,
    topic VARCHAR(100) NOT NULL,
    partition_id INTEGER NOT NULL,
    current_offset BIGINT NOT NULL,
    log_end_offset BIGINT NOT NULL,
    lag BIGINT NOT NULL,
    timestamp TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Create partitioned tables for high-volume data (optional, for advanced setup)
-- CREATE TABLE events.transactions_partitioned (
--     LIKE events.transactions INCLUDING ALL
-- ) PARTITION BY RANGE (created_at);

-- Grant permissions
GRANT USAGE ON SCHEMA events TO rtuser;
GRANT USAGE ON SCHEMA analytics TO rtuser;
GRANT USAGE ON SCHEMA monitoring TO rtuser;

GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA events TO rtuser;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA analytics TO rtuser;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA monitoring TO rtuser;

GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA events TO rtuser;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA analytics TO rtuser;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA monitoring TO rtuser;

-- Insert some sample data for testing
INSERT INTO events.transactions (transaction_id, user_id, amount, currency, transaction_type, status, merchant_id) VALUES
('TXN_001', 'user123', 99.99, 'USD', 'PURCHASE', 'COMPLETED', 'merchant_abc'),
('TXN_002', 'user456', 149.50, 'USD', 'PURCHASE', 'PENDING', 'merchant_xyz'),
('TXN_003', 'user789', 25.00, 'USD', 'REFUND', 'COMPLETED', 'merchant_abc');

INSERT INTO events.iot_sensors (sensor_id, device_type, location, temperature, humidity, pressure, battery_level) VALUES
('TEMP_001', 'TEMPERATURE', 'Office Floor 1', 22.5, 45.0, 1013.25, 85),
('TEMP_002', 'TEMPERATURE', 'Office Floor 2', 23.1, 48.2, 1012.80, 92),
('HUM_001', 'HUMIDITY', 'Warehouse A', 18.5, 65.8, 1014.10, 78);

-- Create a function to clean old data (optional)
CREATE OR REPLACE FUNCTION monitoring.cleanup_old_data(retention_days INTEGER DEFAULT 30)
RETURNS INTEGER AS $$
DECLARE
    deleted_count INTEGER;
BEGIN
    DELETE FROM monitoring.kafka_consumer_lag 
    WHERE timestamp < NOW() - INTERVAL '1 day' * retention_days;
    
    GET DIAGNOSTICS deleted_count = ROW_COUNT;
    RETURN deleted_count;
END;
$$ LANGUAGE plpgsql;