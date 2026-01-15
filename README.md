# eKYC (Electronic Know Your Customer) Verification System

## Project Overview
This is an **Enterprise-grade eKYC Verification System** that orchestrates multiple verification services to perform comprehensive customer identity verification. The system integrates with external verification providers to validate customer identity documents, biometric data, address information, and sanctions screening.

---

## Business Purpose
The system automates the KYC (Know Your Customer) compliance process required by financial institutions and regulated businesses to verify customer identities. It makes intelligent decisions based on multiple verification checks and routes edge cases to manual review.

---

## Core Features Implemented

### 1. **Multi-Verification Orchestration**
- **ID Document Verification**: Validates government-issued identity documents (passport, driver's license, etc.)
- **Face Match/Biometric Verification**: Matches customer selfies with document photos using facial recognition
- **Address Verification**: Validates customer residential address against external databases
- **Sanctions Screening**: Critical check against watchlists, PEP (Politically Exposed Persons), and sanctions databases

### 2. **Intelligent Decision Engine**
- **Automated Decision Making**: Analyzes verification results and makes approval/rejection/manual-review decisions
- **Risk-Based Logic**:
    - Sanctions hits → Immediate rejection
    - High confidence matches → Automatic approval
    - Low confidence/failures → Route to manual review
- **Configurable Thresholds**:
    - Document confidence: 85%
    - Biometric confidence: 85%
    - Address confidence: 80%
    - Similarity score: 85%

### 3. **Resilience & Reliability Features**

#### **Rate Limiting**
- Sliding window rate limiter (10 requests/minute per service)
- Prevents overwhelming external APIs
- Automatic queuing when limits are reached

#### **Retry Mechanism**
- Exponential backoff retry strategy (3 attempts by default)
- Initial backoff: 100ms
- Backoff multiplier: 2.0x
- Configurable retry counts per service

#### **Correlation ID Tracking**
- End-to-end request tracking across all services
- Comprehensive logging with correlation IDs
- Easy debugging and audit trail

### 4. **External Service Integration**
- HTTP client wrapper for external API calls
- DTO (Data Transfer Object) layer for all external communications
- Timeout handling (5 seconds default)
- Failure handling with graceful degradation

### 5. **Comprehensive Logging**
- Structured logging using SLF4J/Logback
- Request/response logging for all verification steps
- Performance metrics (duration tracking)
- Decision audit trail
- Error and exception tracking

---

## Technical Architecture

### **Architecture Pattern**
- **Service-Oriented Architecture (SOA)**
- **Orchestration Pattern**: Central orchestrator coordinates multiple verification services
- **Client-Service Pattern**: Dedicated clients for each external service
- **Decision Engine Pattern**: Centralized business logic for KYC decisions

### **Key Components**

#### **Service Layer**
- `KYCOrchestrationService`: Main orchestrator that coordinates all verification flows
- `DecisionEngineService`: Business logic engine for making KYC approval decisions

#### **Client Layer**
- `DocumentVerificationClient`: Handles ID document verification
- `BiometricVerificationClient`: Handles face matching
- `AddressVerificationClient`: Handles address validation
- `SanctionsScreeningClient`: Handles sanctions/watchlist screening
- `HttpClientWrapper`: Reusable HTTP client for external API calls

#### **Infrastructure Components**
- `RateLimiter`: Sliding window rate limiting
- `RetryHandler`: Exponential backoff retry mechanism
- `EKYCConfiguration`: Central configuration for Jackson ObjectMapper

#### **Domain Models**
- `Customer`: Customer entity with personal information
- `VerificationRequest`: Request model for KYC verification
- `VerificationResult`: Result from individual verification checks
- `KYCDecisionResult`: Final decision with all verification results
- `KYCDecision`: Enum (APPROVED, REJECTED, MANUAL_REVIEW)
- `VerificationType`: Enum (ID_DOCUMENT, FACE_MATCH, ADDRESS, SANCTIONS)
- `VerificationStatus`: Enum for verification outcomes

#### **DTOs**
- Separate request/response DTOs for each external service
- Clean separation between domain models and API contracts

---

## Tools & Technologies Used

### **Core Framework**
- **Spring Boot 4.0.1** - Latest enterprise Java framework
- **Spring Web MVC** - RESTful web services

### **Java**
- **Java 21** - Latest LTS version with modern language features

### **Build Tool**
- **Apache Maven** - Dependency management and build automation
- Maven Wrapper included for consistent builds

### **Libraries & Dependencies**

#### **Lombok**
- Reduces boilerplate code
- Annotations: `@Slf4j`, `@Data`, `@Builder`, `@RequiredArgsConstructor`, etc.

#### **Jackson**
- `jackson-databind` - JSON serialization/deserialization
- `jackson-module-kotlin` - Kotlin module support
- `jackson-datatype-jsr310` - Java 8 Date/Time API support (LocalDate, Instant, etc.)

#### **Logging**
- SLF4J - Logging facade
- Logback (via Spring Boot) - Logging implementation

### **Development Tools**
- Maven Compiler Plugin with Lombok annotation processing
- Spring Boot Maven Plugin for packaging

---

## Key Design Decisions

### 1. **Fail-Fast for Critical Checks**
Sanctions screening is treated as critical. Any hit or failure stops further processing immediately.

### 2. **Graceful Degradation**
Non-critical service failures result in MANUAL_REVIEW rather than system failure.

### 3. **Observability First**
Extensive logging with correlation IDs ensures every request is traceable.

### 4. **Resilience by Design**
- Rate limiting prevents cascade failures
- Retry logic handles transient failures
- Timeouts prevent resource exhaustion

### 5. **Clean Architecture**
- Clear separation of concerns (clients, services, domain models)
- DTOs isolate external contracts from domain models
- Dependency injection for testability

---

## Data Flow

```
1. Customer submits verification request
   ↓
2. KYCOrchestrationService generates correlation ID
   ↓
3. Sanctions screening (CRITICAL - must pass)
   ↓
4. Parallel/Sequential verification checks:
   - Document verification
   - Biometric verification
   - Address verification
   ↓
5. DecisionEngineService evaluates all results
   ↓
6. Return KYCDecisionResult:
   - APPROVED (all checks passed)
   - REJECTED (sanctions hit or critical failures)
   - MANUAL_REVIEW (edge cases, low confidence)
```

---

## Configuration

### **Application Properties**
- Service URLs configurable via `application.properties`
- Default values provided for local development
- Environment-specific configuration support

### **Thresholds & Limits**
- Rate limit: 10 requests/minute/service
- Retry attempts: 3 (configurable)
- Timeout: 5 seconds per request
- Confidence thresholds: 80-85% depending on verification type

---


## Project Structure

```
src/main/java/com/coding/interview/ekyc/
├── client/          # External service clients
│   ├── dto/         # Data Transfer Objects
├── config/          # Configuration classes
├── model/           # Domain models
├── ratelimit/       # Rate limiting
├── retry/           # Retry logic
└── service/         # Business logic
```

---

## Summary

This eKYC system demonstrates enterprise-grade software engineering practices including:
- ✅ Microservice integration patterns
- ✅ Resilience engineering (retry, rate limiting)
- ✅ Clean architecture and separation of concerns
- ✅ Comprehensive logging and observability
- ✅ Production-ready error handling
- ✅ Configurable and extensible design
- ✅ Modern Java and Spring Boot best practices

**Status**: Core verification orchestration and decision engine implemented and compiled successfully.

