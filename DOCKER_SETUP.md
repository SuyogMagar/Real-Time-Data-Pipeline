# Real-Time Data Pipeline - Docker Environment

This Docker Compose setup provides a complete development environment for the Real-Time Data Pipeline project.

## 🚀 Services Overview

| Service | Port | Purpose | Credentials |
|---------|------|---------|-------------|
| **Kafka** | 9092 | Message Broker | N/A |
| **Zookeeper** | 2181 | Kafka Coordination | N/A |
| **Kafka UI** | 8080 | Kafka Management Interface | N/A |
| **PostgreSQL** | 5432 | Primary Database | `rtuser` / `rtpassword` |
| **Prometheus** | 9090 | Metrics Collection | N/A |
| **Grafana** | 3000 | Visualization Dashboard | `admin` / `admin123` |
| **Redis** | 6379 | Caching & Session Store | N/A |

## 📋 Prerequisites

- Docker Desktop installed and running
- At least 4GB RAM available for containers
- Ports 2181, 3000, 5432, 6379, 8080, 9090, 9092, 9101 available

## 🛠️ Quick Start

### 1. Start All Services
```bash
# Start in detached mode
docker-compose up -d

# Or start with logs visible
docker-compose up
```

### 2. Verify Services
```bash
# Check all containers are running
docker-compose ps

# Check individual service logs
docker-compose logs kafka
docker-compose logs postgres
docker-compose logs grafana
```

### 3. Access Web Interfaces

- **Kafka UI**: http://localhost:8080
- **Grafana**: http://localhost:3000 (admin/admin123)
- **Prometheus**: http://localhost:9090

## 🔧 Service Configuration

### Kafka Configuration
- **Bootstrap Servers**: `localhost:9092`
- **Topics**: Auto-created when first message is published
- **JMX Port**: 9101 (for monitoring)

### PostgreSQL Configuration
- **Database**: `realtimedb`
- **Username**: `rtuser`
- **Password**: `rtpassword`
- **Schemas**: `events`, `analytics`, `monitoring`

### Monitoring Stack
- **Prometheus**: Collects metrics from Kafka and Spring Boot app
- **Grafana**: Pre-configured with Prometheus datasource
- **Dashboard Location**: `docker/grafana/dashboards/`

## 📊 Database Schema

The PostgreSQL database is automatically initialized with:

### Events Schema
- `events.transactions` - Financial transaction events
- `events.iot_sensors` - IoT sensor data
- `events.system_logs` - Application and system logs

### Analytics Schema
- `analytics.hourly_transaction_stats` - Aggregated transaction metrics

### Monitoring Schema
- `monitoring.kafka_consumer_lag` - Consumer lag tracking

## 🎯 Kafka Topics (Auto-Created)

The following topics will be created when your application publishes messages:
- `transaction-events`
- `iot-sensor-data`
- `system-logs`
- `processed-transactions`

## 🐛 Troubleshooting

### Common Issues

1. **Port Conflicts**
   ```bash
   # Check what's using a port
   netstat -ano | findstr :9092
   
   # Kill process if needed
   taskkill /PID <PID> /F
   ```

2. **Container Won't Start**
   ```bash
   # Check container logs
   docker-compose logs [service-name]
   
   # Restart specific service
   docker-compose restart [service-name]
   ```

3. **Database Connection Issues**
   ```bash
   # Test PostgreSQL connection
   docker-compose exec postgres psql -U rtuser -d realtimedb -c "SELECT version();"
   ```

4. **Kafka Connection Issues**
   ```bash
   # List Kafka topics
   docker-compose exec kafka kafka-topics --bootstrap-server localhost:9092 --list
   
   # Test producer
   docker-compose exec kafka kafka-console-producer --bootstrap-server localhost:9092 --topic test-topic
   ```

### Health Checks

All services include health checks. Check status with:
```bash
docker-compose ps
```

Healthy services show `Up (healthy)` status.

## 🧹 Cleanup

### Stop Services
```bash
# Stop all services
docker-compose down

# Stop and remove volumes (⚠️ This deletes all data)
docker-compose down -v

# Remove images as well
docker-compose down -v --rmi all
```

### Partial Cleanup
```bash
# Stop specific service
docker-compose stop kafka

# Remove specific service
docker-compose rm kafka
```

## 📈 Monitoring Your Application

Once your Spring Boot application is running on port 8090, it will automatically be monitored by:

1. **Prometheus** - Metrics collection from `/actuator/prometheus`
2. **Grafana** - Visualization of application and Kafka metrics
3. **Kafka UI** - Topic management and message inspection

## 🔄 Development Workflow

1. Start Docker services: `docker-compose up -d`
2. Run your Spring Boot application on port 8090
3. Monitor via Grafana dashboards
4. Inspect Kafka messages via Kafka UI
5. Query database directly or via application

## 📝 Next Steps

After the Docker environment is running:
1. Configure Spring Boot application with Kafka and database connections
2. Implement Kafka producers and consumers
3. Add Micrometer metrics to your application
4. Create custom Grafana dashboards
5. Implement data processing logic

## 🔗 Useful Commands

```bash
# View all logs
docker-compose logs -f

# Scale Kafka consumers (if using multiple instances)
docker-compose up -d --scale kafka-consumer=3

# Execute commands in containers
docker-compose exec postgres psql -U rtuser -d realtimedb
docker-compose exec kafka kafka-topics --bootstrap-server localhost:9092 --list
docker-compose exec redis redis-cli

# Backup database
docker-compose exec postgres pg_dump -U rtuser realtimedb > backup.sql

# Restore database
cat backup.sql | docker-compose exec -T postgres psql -U rtuser -d realtimedb
```

---

**Note**: This setup is configured for development. For production deployment, consider:
- Using Docker secrets for passwords
- Implementing proper network security
- Setting up data persistence strategies
- Configuring resource limits
- Using environment-specific configurations