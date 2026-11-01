# Predictive Cloud Resource Autoscaling & Intelligent Resource Allocation on AWS

> **Production-Quality Spring Boot 3 / Java 21 Cloud Infrastructure Monitoring & Reactive Autoscaling Platform**

---

## 1. Project Overview

The **Predictive Cloud Resource Autoscaling and Intelligent Resource Allocation** platform is a resilient, modular cloud backend designed to monitor distributed application workloads and compute infrastructure, ingest granular telemetry, dynamically compute capacity requirements, and manage rule-based reactive autoscaling decisions with full human-in-the-loop auditability.

While the future roadmap incorporates machine learning-based workload forecasting (LSTM / Prophet / XGBoost) and direct AWS cloud provisioning (Amazon EC2, ALB, CloudWatch, and Auto Scaling Groups via a Python controller), this phase establishes an **enterprise-grade, provider-independent Java 21 / Spring Boot 3.3.x backend** capable of operating fully on simulated cloud infrastructure.

### Core Capabilities
- **Stateless JWT Security & RBAC**: Granular role-based access for `ADMIN` (full control, policy configuration, approval) and `VIEWER` (read-only dashboards and metrics).
- **Multi-Tenant Application Management**: Life-cycle operations with referential consistency safeguards preventing catastrophic deletion of active infrastructure.
- **Provider-Agnostic Infrastructure Layer**: Decoupled `InfrastructureProvider` SPI enabling seamless plug-and-play transitions between local simulated nodes and AWS EC2 / ASG.
- **High-Throughput Telemetry Ingestion**: Sub-second metric capture (CPU, Memory, RPS, Latency, Error Rate, Network I/O) with indexed historical persistence.
- **Independent Workload Tracking**: Application traffic monitoring with automated trend analysis (`STABLE`, `INCREASING`, `TRAFFIC_SPIKE`, `DECREASING`).
- **Reactive Autoscaling Engine**: Multi-metric evaluation (CPU, Memory, Request Rate), dynamic capacity calculations (`ceil(current * metric / target)`), hysteresis cooldown protection, and bounded capacity clamping (`[min, max]`).
- **Human-in-the-Loop Audit Trail**: Recommendation lifecycle (`PENDING` -> `APPROVED` / `REJECTED`) and immutable scaling execution event history.
- **Active Alert Management & Deduplication**: Real-time alarm generation with suppression of duplicate active alarms.
- **Angular-Ready Dashboard Analytics**: Single-query aggregate metrics and time-series trend endpoints tailored for frontend charting components.
- **Managed Workload Simulation Engine**: Scheduled simulation harness supporting `NORMAL`, `INCREASING`, `TRAFFIC_SPIKE`, and `TRAFFIC_REDUCTION` profiles with deterministic seeding.

---

## 2. Architecture

The backend follows a **Modular Layered Architecture** with strict separation of concerns, decoupling domain entities from external REST contracts via strongly-typed DTOs.

```
com.project.autoscaling
├── config                    # Spring Security, CORS, OpenAPI, Managed Scheduling
├── security                  # JWT Service, Authentication Filters, Entry Points
├── auth                      # Registration, Authentication, Profile APIs & DTOs
├── user                      # User persistence & role management (ADMIN, VIEWER)
├── application               # Multi-application registration & status management
├── infrastructure
│   ├── provider              # InfrastructureProvider abstraction (Simulated / AWS ready)
│   ├── controller, service   # Compute instance lifecycle & capacity calculations
│   └── entity, repository    # Infrastructure instance persistence
├── monitoring
│   ├── collector             # Telemetry collection & validation layer
│   └── controller, service   # Metric history, aggregation & utilization analytics
├── workload                  # Traffic patterns, RPS, p95/p99 latency observations
├── scaling
│   ├── engine                # CapacityCalculationService & ReactiveScalingEngine
│   ├── controller, service   # Scaling policy CRUD, evaluation & recommendation workflows
│   └── entity, repository    # Policies, Recommendations & Audit Event records
├── alerts                    # Alarm generation, deduplication & incident resolution
├── dashboard                 # Aggregated overview, system health & trend analytics
├── simulation                # Managed background telemetry simulation engine
└── common
    ├── enums                 # Domain types (Role, Status, Action, Severity, Profile)
    ├── exception             # Centralized @RestControllerAdvice error handler
    └── response              # Standard ApiResponse<T> & PagedResponse<T> wrappers
```

### Decoupled Cloud Provider Architecture
```
                        +----------------------------+
                        |   ReactiveScalingEngine    |
                        +----------------------------+
                                      |
                                      v
                        +----------------------------+
                        |   InfrastructureProvider   | (Interface SPI)
                        +----------------------------+
                                      |
                 +--------------------+--------------------+
                 |                                         |
                 v                                         v
+----------------------------------+     +----------------------------------+
|   SimulatedInfrastructureProvider|     |      AwsInfrastructureProvider   |
|   (Local In-Memory / DB Nodes)   |     |      (Future AWS SDK / Boto3)    |
+----------------------------------+     +----------------------------------+
```

---

## 3. Technology Stack

- **Runtime**: Java 21 LTS
- **Framework**: Spring Boot 3.3.4
- **Persistence**: Spring Data JPA / Hibernate 6
- **Database**: MySQL 8.0 (Production / Docker), H2 (In-memory Test Suite)
- **Database Migrations**: Flyway Migration Engine (`V1__init_schema.sql`)
- **Security**: Spring Security 6 & JJWT 0.12.6 (HMAC-SHA256 stateless tokens)
- **Validation**: Jakarta Validation API (`@Valid`, `@NotNull`, `@Min`, `@Max`, `@DecimalMin`, `@DecimalMax`)
- **API Documentation**: Springdoc OpenAPI 2.6.0 / Swagger UI 3
- **Testing**: JUnit 5, Mockito, Spring Boot Test, Spring MockMvc
- **Build Tool**: Apache Maven 3.9+
- **Containerization**: Docker & Docker Compose

---

## 4. Prerequisites

- **Java Development Kit (JDK)**: Version 21 or higher installed (`java -version`)
- **Apache Maven**: Version 3.8+ installed (`mvn -version`)
- **Docker & Docker Compose**: For local MySQL 8 database containerization
- **Git**: Version control client

---

## 5. Installation Instructions

1. **Clone the repository:**
   ```bash
   git clone https://github.com/Rohith1972/Predictive-Cloud-Resource-Autoscaling.git
   cd Predictive-Cloud-Resource-Autoscaling
   ```

2. **Launch MySQL Database with Docker Compose:**
   ```bash
   docker-compose up -d mysql
   ```
   *Verify container status:*
   ```bash
   docker ps
   ```

3. **Navigate to the backend directory:**
   ```bash
   cd backend
   ```

4. **Build and package the project:**
   ```bash
   mvn clean package -DskipTests
   ```

---

## 6. MySQL Configuration

The application utilizes **Flyway** for database migrations, ensuring all database tables, foreign keys, and indexes are created automatically on startup.

Default local connection parameters:
- **Host**: `localhost`
- **Port**: `3306`
- **Database**: `autoscaling_db`
- **Username**: `root`
- **Password**: `root`

To manually connect to MySQL:
```bash
mysql -h 127.0.0.1 -P 3306 -u root -proot autoscaling_db
```

---

## 7. Environment Variables

Create a `.env` file or export the following environment variables:

| Variable | Default Value | Description |
| :--- | :--- | :--- |
| `SERVER_PORT` | `8080` | Embedded Tomcat HTTP port |
| `SPRING_PROFILES_ACTIVE` | `dev` | Active Spring profile (`dev`, `prod`, `test`) |
| `DB_HOST` | `localhost` | MySQL hostname |
| `DB_PORT` | `3306` | MySQL port |
| `DB_NAME` | `autoscaling_db` | MySQL database name |
| `DB_USERNAME` | `root` | Database username |
| `DB_PASSWORD` | `root` | Database password |
| `JWT_SECRET` | *(256-bit Hex String)* | Secret key for signing HMAC-SHA256 JWTs |
| `JWT_EXPIRATION_MS` | `86400000` | Access token lifespan (24 hours) |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:4200` | Allowed origins for Angular frontend |

---

## 8. How to Run the Spring Boot Application

### Method 1: Using Maven Spring Boot Plugin (Recommended for Development)
```bash
cd backend
mvn spring-boot:run
```

### Method 2: Running the Packaged JAR
```bash
cd backend
mvn clean package -DskipTests
java -jar target/autoscaling-backend-1.0.0.jar
```

### Method 3: Running Entire Stack with Docker Compose
```bash
docker-compose up --build
```

---

## 9. Swagger UI & API Documentation

Once the application is running, access interactive OpenAPI documentation and Swagger UI at:

- **Swagger UI**: [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)
- **OpenAPI JSON Spec**: [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)

To authorize requests in Swagger UI, click **Authorize** (top right) and paste your Bearer token:
```
Bearer <your_jwt_token>
```

---

## 10. Authentication & Authorization

All endpoints (except `/api/v1/auth/**`, `/swagger-ui/**`, `/v3/api-docs/**`) require an `Authorization: Bearer <token>` header.

### Roles & Access Matrix

| Feature / Endpoint | ADMIN | VIEWER |
| :--- | :---: | :---: |
| Register / Login / View Profile | Yes | Yes |
| Create / Update / Delete Applications | Yes | No |
| Register / Change Instance Status | Yes | No |
| Ingest Resource Metrics & Workload | Yes | Yes |
| Create / Modify Scaling Policies | Yes | No |
| Evaluate Scaling / Trigger Dry-Run | Yes | No |
| Approve / Reject Recommendations | Yes | No |
| View Monitoring, Workload & History | Yes | Yes |
| View Dashboard Analytics & Trends | Yes | Yes |
| Acknowledge / Resolve Alerts | Yes | No |
| Start / Stop Workload Simulation | Yes | No |
| View Simulation Status | Yes | Yes |

---

## 11. API Endpoint Catalog

### Authentication
- `POST /api/v1/auth/register` - Register a new user (`ADMIN` or `VIEWER`)
- `POST /api/v1/auth/login` - Authenticate with email/password and receive JWT
- `GET /api/v1/auth/me` - Retrieve authenticated user profile

### Application Management
- `POST /api/v1/applications` - Register an application
- `GET /api/v1/applications` - List all applications (supports pagination)
- `GET /api/v1/applications/{id}` - Retrieve application by ID
- `PUT /api/v1/applications/{id}` - Update application details
- `PATCH /api/v1/applications/{id}/status` - Activate or deactivate application
- `DELETE /api/v1/applications/{id}` - Safe deletion (prevented if instances exist)
- `GET /api/v1/applications/{id}/summary` - Application summary with running instances count

### Infrastructure Management
- `POST /api/v1/instances` - Register a simulated infrastructure instance
- `GET /api/v1/instances` - List all instances with optional status filter
- `GET /api/v1/instances/{id}` - Retrieve instance details
- `GET /api/v1/instances/application/{applicationId}` - List instances for application
- `PATCH /api/v1/instances/{id}/status` - Update instance status (`RUNNING`, `STOPPED`, `TERMINATED`, `UNHEALTHY`)
- `GET /api/v1/instances/application/{applicationId}/capacity` - Total active cores and RAM capacity

### Telemetry & Monitoring
- `POST /api/v1/monitoring/metrics` - Ingest resource metrics (CPU, Memory, RPS, Latency, Error Rate)
- `GET /api/v1/monitoring/latest/{applicationId}` - Retrieve most recent telemetry
- `GET /api/v1/monitoring/instance/{instanceId}` - Paginated metrics for specific instance
- `GET /api/v1/monitoring/history/{applicationId}` - Historical metrics within time range
- `GET /api/v1/monitoring/aggregates/{applicationId}` - Min, Max, and Average metric aggregates
- `GET /api/v1/monitoring/summary/{applicationId}` - Real-time application resource utilization summary

### Workload Management
- `POST /api/v1/workload/records` - Ingest application traffic and latency observations
- `GET /api/v1/workload/{applicationId}/trend` - Traffic trend analysis (`TRAFFIC_SPIKE`, `INCREASING`, `DECREASING`, `STABLE`)
- `GET /api/v1/workload/{applicationId}/summary` - Workload summary over time window
- `GET /api/v1/workload/{applicationId}/history` - Paginated workload history

### Scaling Policies & Engine
- `POST /api/v1/scaling/policies` - Create scaling policy (`min`, `max`, `desired`, thresholds, `cooldown`)
- `GET /api/v1/scaling/policies/{id}` - Retrieve scaling policy by ID
- `GET /api/v1/scaling/policies/application/{applicationId}` - Retrieve policy by application ID
- `PUT /api/v1/scaling/policies/{id}` - Update scaling policy configuration
- `PATCH /api/v1/scaling/policies/{id}/status` - Enable or disable scaling policy
- `POST /api/v1/scaling/evaluate/{applicationId}` - Trigger reactive scaling evaluation (`DRY_RUN` / `MANUAL_APPROVAL`)
- `GET /api/v1/scaling/recommendations/{applicationId}` - Retrieve pending recommendations
- `POST /api/v1/scaling/recommendations/{id}/approve` - Approve recommendation and adjust capacity
- `POST /api/v1/scaling/recommendations/{id}/reject` - Reject recommendation with reason
- `GET /api/v1/scaling/history/{applicationId}` - Audit trail of completed scaling events
- `GET /api/v1/scaling/history/{applicationId}/summary` - Total scale-out and scale-in event counts

### Alert Management
- `GET /api/v1/alerts` - List alerts with optional status/severity filtering and pagination
- `GET /api/v1/alerts/active` - List all currently active alerts
- `GET /api/v1/alerts/{id}` - Retrieve alert by ID
- `PATCH /api/v1/alerts/{id}/acknowledge` - Mark alert as acknowledged (`ADMIN` only)
- `PATCH /api/v1/alerts/{id}/resolve` - Mark alert as resolved (`ADMIN` only)

### Dashboard Analytics
- `GET /api/v1/dashboard/overview` - Global system overview (apps, instances, avg CPU/RAM, active alerts)
- `GET /api/v1/dashboard/application/{applicationId}` - Single-application dashboard snapshot
- `GET /api/v1/dashboard/resource-trends/{applicationId}` - Time-series CPU, Memory & RPS for charts
- `GET /api/v1/dashboard/workload-trends/{applicationId}` - Time-series RPS, Latency & Failures for charts
- `GET /api/v1/dashboard/scaling-summary/{applicationId}` - Scaling activity summary
- `GET /api/v1/dashboard/instance-summary/{applicationId}` - Compute capacity summary

### Workload Simulation
- `POST /api/v1/simulation/start/{applicationId}` - Start scheduled telemetry generation (`NORMAL`, `INCREASING`, `TRAFFIC_SPIKE`, `TRAFFIC_REDUCTION`)
- `POST /api/v1/simulation/stop/{applicationId}` - Stop running simulation
- `GET /api/v1/simulation/status/{applicationId}` - Retrieve simulation status and generated count

---

## 12. Database Schema Overview

```
users
├── id (BIGINT, PK)
├── email (VARCHAR, UNIQUE)
├── password (VARCHAR)
├── full_name (VARCHAR)
├── role (VARCHAR: ADMIN, VIEWER)
└── created_at, updated_at

applications
├── id (BIGINT, PK)
├── application_name (VARCHAR, UNIQUE)
├── application_url (VARCHAR)
├── environment (VARCHAR: DEVELOPMENT, TESTING, PRODUCTION)
├── status (VARCHAR: ACTIVE, INACTIVE)
└── created_at, updated_at

infrastructure_instances
├── id (BIGINT, PK)
├── instance_identifier (VARCHAR, UNIQUE)
├── application_id (FK -> applications.id)
├── cpu_cores, total_memory_mb
├── status (VARCHAR: RUNNING, STOPPED, TERMINATED, UNHEALTHY)
└── provider (VARCHAR: LOCAL_SIMULATED, AWS)

resource_metrics
├── id (BIGINT, PK)
├── application_id (FK -> applications.id, INDEX)
├── instance_id (FK -> infrastructure_instances.id, INDEX)
├── cpu_utilization, memory_utilization, request_rate
├── average_response_time, error_rate, active_requests
└── timestamp (DATETIME, INDEX)

workload_metrics
├── id (BIGINT, PK)
├── application_id (FK -> applications.id, INDEX)
├── request_count, requests_per_second, successful_requests, failed_requests
├── average_latency, p95_latency, p99_latency
└── timestamp (DATETIME, INDEX)

scaling_policies
├── id (BIGINT, PK)
├── application_id (FK -> applications.id, UNIQUE)
├── minimum_instances, maximum_instances, desired_instances
├── cpu_scale_out_threshold, cpu_scale_in_threshold
├── memory_scale_out_threshold, memory_scale_in_threshold
├── request_rate_threshold, cooldown_seconds, enabled
└── created_at, updated_at

scaling_recommendations
├── id (BIGINT, PK)
├── application_id (FK -> applications.id, INDEX)
├── scaling_action (VARCHAR: SCALE_OUT, SCALE_IN, NO_ACTION)
├── current_instance_count, recommended_instance_count
├── triggering_metric, triggering_metric_value, threshold_value
├── execution_mode (VARCHAR: DRY_RUN, MANUAL_APPROVAL)
└── status (VARCHAR: PENDING, APPROVED, REJECTED)

scaling_events
├── id (BIGINT, PK)
├── application_id (FK -> applications.id, INDEX)
├── previous_instance_count, new_instance_count, scaling_action
├── status (VARCHAR: PENDING, IN_PROGRESS, COMPLETED, FAILED)
└── created_at, completed_at

alerts
├── id (BIGINT, PK)
├── application_id (FK -> applications.id, INDEX)
├── instance_id (FK -> infrastructure_instances.id)
├── alert_type (HIGH_CPU, HIGH_MEMORY, HIGH_REQUEST_RATE, HIGH_LATENCY, etc.)
├── severity (INFO, WARNING, CRITICAL, INDEX)
├── status (ACTIVE, ACKNOWLEDGED, RESOLVED, INDEX)
└── created_at, resolved_at
```

---

## 13. How to Generate Simulated Data

1. **Register and login as an ADMIN** to obtain a Bearer token:
   ```bash
   curl -X POST http://localhost:8080/api/v1/auth/login \
     -H "Content-Type: application/json" \
     -d '{"email":"admin@example.com","password":"Password123!"}'
   ```

2. **Register a monitored application**:
   ```bash
   curl -X POST http://localhost:8080/api/v1/applications \
     -H "Authorization: Bearer <TOKEN>" \
     -H "Content-Type: application/json" \
     -d '{
       "applicationName": "E-Commerce Checkout API",
       "description": "Critical checkout service",
       "applicationUrl": "https://checkout.example.com",
       "environment": "PRODUCTION"
     }'
   ```

3. **Start workload simulation**:
   ```bash
   curl -X POST http://localhost:8080/api/v1/simulation/start/1 \
     -H "Authorization: Bearer <TOKEN>" \
     -H "Content-Type: application/json" \
     -d '{
       "workloadProfile": "TRAFFIC_SPIKE",
       "intervalSeconds": 5,
       "randomSeed": 42
     }'
   ```
   *The simulation engine will automatically bootstrap baseline instances if none exist and generate realistic CPU, Memory, RPS, and Latency metrics every 5 seconds.*

4. **Verify generated metrics**:
   ```bash
   curl -X GET http://localhost:8080/api/v1/monitoring/latest/1 \
     -H "Authorization: Bearer <TOKEN>"
   ```

5. **Stop workload simulation**:
   ```bash
   curl -X POST http://localhost:8080/api/v1/simulation/stop/1 \
     -H "Authorization: Bearer <TOKEN>"
   ```

---

## 14. How to Test the Reactive Scaling Engine

1. **Configure a Scaling Policy**:
   ```bash
   curl -X POST http://localhost:8080/api/v1/scaling/policies \
     -H "Authorization: Bearer <TOKEN>" \
     -H "Content-Type: application/json" \
     -d '{
       "applicationId": 1,
       "minimumInstances": 2,
       "maximumInstances": 8,
       "desiredInstances": 2,
       "cpuScaleOutThreshold": 75.0,
       "cpuScaleInThreshold": 30.0,
       "memoryScaleOutThreshold": 80.0,
       "memoryScaleInThreshold": 35.0,
       "requestRateThreshold": 250.0,
       "cooldownSeconds": 300,
       "enabled": true
     }'
   ```

2. **Ingest high resource metrics (breaching threshold)**:
   ```bash
   curl -X POST http://localhost:8080/api/v1/monitoring/metrics \
     -H "Authorization: Bearer <TOKEN>" \
     -H "Content-Type: application/json" \
     -d '{
       "applicationId": 1,
       "instanceId": 1,
       "cpuUtilization": 92.5,
       "memoryUtilization": 84.0,
       "requestRate": 320.0,
       "averageResponseTime": 140.0,
       "errorRate": 0.5
     }'
   ```

3. **Evaluate scaling**:
   ```bash
   curl -X POST http://localhost:8080/api/v1/scaling/evaluate/1 \
     -H "Authorization: Bearer <TOKEN>" \
     -H "Content-Type: application/json" \
     -d '{"executionMode": "MANUAL_APPROVAL"}'
   ```
   *Output will recommend `SCALE_OUT` to `ceil(2 * 92.5 / 75.0) = 3` instances.*

4. **Approve recommendation**:
   ```bash
   curl -X POST http://localhost:8080/api/v1/scaling/recommendations/1/approve \
     -H "Authorization: Bearer <TOKEN>"
   ```
   *Verifies simulated capacity adjustment, creates a `COMPLETED` scaling event, and resets the policy cooldown.*

---

## 15. Automated Test Suite

The project includes an exhaustive suite of 39 automated tests covering unit logic, integration flows, Spring Security authentication, and critical edge cases:
- `AuthIntegrationTest`: User registration, password encryption, JWT issuance, duplicate checks, profile retrieval.
- `ApplicationIntegrationTest`: Full CRUD, unique names, status transitions, safe deletion safeguards.
- `InstanceIntegrationTest`: Registration, status mutations, capacity aggregation.
- `MonitoringIntegrationTest`: Metric ingestion, 0-100% boundary validations, pagination, statistical aggregates.
- `WorkloadIntegrationTest`: Traffic observation persistence, trend classification algorithms.
- `ScalingPolicyIntegrationTest`: Boundary validations (`min >= 1`, `max >= min`, `scaleIn < scaleOut`, `cooldown >= 0`).
- `ScalingEngineIntegrationTest`: Scale-out triggers, scale-in underutilization, approval/rejection lifecycle, cooldown suppression.
- `AlertsAndDashboardIntegrationTest`: Alarm triggering, deduplication, resolution, overview and chart trend calculations.
- `SimulationIntegrationTest`: Simulation lifecycle, duplicate start prevention, deterministic telemetry generation.
- `AutoscalingEdgeCasesIntegrationTest`: Edge cases including 0% & 100% CPU limits, missing metrics, capacity ceilings, duplicate approval rejection, and RBAC authorization barriers.

### Execute All Tests
```bash
cd backend
mvn clean test
```

---

## 16. Current Limitations

1. **Simulation Scope**: Infrastructure instances and compute metrics are simulated locally; real EC2 instances are neither provisioned nor terminated in this phase.
2. **Forecast Model**: Scaling recommendations currently operate on reactive threshold evaluations and capacity calculations rather than predictive machine learning forecasts.
3. **Execution Modes**: Execution is confined to `DRY_RUN` and `MANUAL_APPROVAL` to prevent unauthorized automated capacity changes before cloud integration.

---

## 17. Future Integration Roadmap

### Future AWS Cloud Integration Plan
1. **AWS Infrastructure Provider**: Implement `AwsInfrastructureProvider` adhering to the existing `InfrastructureProvider` SPI.
2. **AWS CloudWatch Adapter**: Ingest metrics from AWS CloudWatch metric streams directly into the `TelemetryCollector`.
3. **AWS Auto Scaling Group & ALB Controller**: Dispatch approved capacity changes to Amazon EC2 Auto Scaling Groups (`UpdateAutoScalingGroup` desired capacity API) and Application Load Balancer target groups.
4. **IAM Role-Based Authentication**: Replace simulated providers with AWS IAM Instance Profiles / OIDC assume-role policies without changing scaling engine contracts.

### Future Machine Learning Integration Plan
1. **Workload Prediction Microservice**: Deploy Python/FastAPI forecasting service running LSTM/Prophet models trained on historical `workload_metrics`.
2. **Proactive Scaling Policy Extension**: Extend `ScalingPolicy` to support predictive lead-time provisioning (scaling out 10-15 minutes prior to forecasted traffic surges).
3. **Hybrid Engine**: Seamlessly blend reactive anomaly protection with proactive ML-driven capacity schedules.

---

## 18. License & Attribution

Developed as part of the **Machine Learning-Based Predictive Autoscaling and Intelligent Resource Allocation on AWS** platform.
Built with Java 21, Spring Boot 3, and industry-standard backend design principles.
