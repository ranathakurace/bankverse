# 🏦 🌍 BankVerse Global Banking Ecosystem

![Java](https://img.shields.io/badge/Java-11-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-2.7.18-brightgreen)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-18-blue)
![Build](https://img.shields.io/badge/Build-Maven-red)
![Status](https://img.shields.io/badge/Status-Story--001%20Completed-success)

Enterprise Digital Banking Platform built using **Spring Boot**, **PostgreSQL**, and **Java** following enterprise software architecture and banking domain best practices.

---

# 📖 Overview

BankVerse is an enterprise-style digital banking platform designed to simulate real-world banking systems.

The project focuses on building secure, scalable, and maintainable banking services while following enterprise design principles and clean architecture.

The implementation covers customer management, account management, transactions, authentication, authorization, fraud detection, notifications, API testing, UI automation, and enterprise software engineering practices.

Runtime database and JWT settings are supplied through environment variables; see [Runtime configuration](docs/RUNTIME_CONFIGURATION.md) before starting the application.

Continuous integration, coverage, static analysis, and dependency-security gates are documented in [CI quality gates](docs/CI_QUALITY_GATES.md).

---

# 🎯 Objectives

* Build a production-style banking backend.
* Apply enterprise architecture and design patterns.
* Learn banking business workflows.
* Implement secure REST APIs.
* Practice API, UI, and database testing.
* Develop reusable automation frameworks.
* Follow enterprise coding and documentation standards.

---

# 🛠 Technology Stack

| Category             | Technology                  |
| -------------------- | --------------------------- |
| Programming Language | Java 11                     |
| Backend Framework    | Spring Boot 2.7.18          |
| ORM Framework        | Spring Data JPA / Hibernate |
| Database             | PostgreSQL 18               |
| Build Tool           | Maven                       |
| IDE                  | Eclipse IDE 2024-09         |
| API Client           | Postman                     |
| Version Control      | Git & GitHub                |

---

# 🏗 Architecture

```text
                Client / Postman
                       │
                       ▼
                 REST Controller
                       │
                       ▼
                  Service Layer
             (Business Logic)
                       │
                       ▼
               Repository Layer
             (Spring Data JPA)
                       │
                       ▼
                PostgreSQL Database
```

---

# 📂 Project Structure

```text
src/main/java
│
├── controller
├── service
├── repository
├── model
├── dto
├── exception
├── security
├── client
└── idempotency
```

---
# 🏦 Enterprise Banking Architecture Roadmap

```text
                                              🌍 BANKVERSE
                                  Global Digital Banking Platform
--------------------------------------------------------------------------------------------------

                                            👤 Customer Channels
                                                  │
                     ┌────────────────────────────┼────────────────────────────┐
                     │                            │                            │
               🌐 Web Portal                 📱 Mobile App              🤖 AI Assistant
                     │                            │                            │
                     └────────────────────────────┼────────────────────────────┘
                                                  │
                                           🌐 API Gateway
                                                  │
 ──────────────────────────────────────────────────────────────────────────────────────────────────
                                                  │
        ┌────────────────────┬────────────────────┼────────────────────┬────────────────────┐
        │                    │                    │                    │                    │
 👤 Customer Service   🏦 Account Service   💸 Transaction      💳 Payment Gateway   🔔 Notification
                                              Service                 Service              Service
        │                    │                    │                    │                    │
        │                    │                    │                    │                    │
        ├──────────────┬─────┴────────────┬──────┴──────────────┬─────┴──────────────┐
        │              │                  │                     │                    │
   📑 KYC Service  💰 Loan Service   💳 Card Service    👥 Beneficiary        💱 Forex Service
        │              │                  │                     │                    │
        └──────────────┴──────────────────┴─────────────────────┴────────────────────┘
                                                  │
──────────────────────────────────────────────────────────────────────────────────────────────────
                                         🔐 Security Layer
                            JWT • OAuth2 • RBAC • Audit • Logging
                                                  │
──────────────────────────────────────────────────────────────────────────────────────────────────
                                          📨 Event Driven Layer
                                Kafka • RabbitMQ • Event Streaming
                                                  │
──────────────────────────────────────────────────────────────────────────────────────────────────
                                         💾 Persistence Layer
                         PostgreSQL • Redis Cache • Object Storage
                                                  │
──────────────────────────────────────────────────────────────────────────────────────────────────
                                       🚀 DevOps & Deployment
                     Docker • Kubernetes • Jenkins • GitHub Actions
                                                  │
──────────────────────────────────────────────────────────────────────────────────────────────────
                                         🤖 Artificial Intelligence
                    Fraud Detection • Risk Scoring • Chatbot • Test Generation
                  AI Code Review • AI Analytics • AI Customer Recommendations

```
##Progress Legend

🟢 Completed
🟡 In Progress
⚪ Planned

Customer Module

🟢 Customer Registration

🟢 Search by Customer Number

🟢 Search by PAN

🟢 Search by Email

🟢 Search by Phone

⚪ Search by Aadhaar

⚪ Search by CIF

⚪ Search by Passport

⚪ Search by Account Number

-----------------------------------------------------

Account Module

⚪ Open Account

⚪ Close Account

⚪ Freeze Account

⚪ Account Statement

-----------------------------------------------------

Transaction Module

⚪ Deposit

⚪ Withdrawal

⚪ Fund Transfer

⚪ Transaction History

-----------------------------------------------------

Payment Gateway (Mock)

⚪ Payment Initiation

⚪ Payment Authorization

⚪ Payment Success

⚪ Payment Failure

⚪ Payment Retry

⚪ Refund

⚪ UPI

⚪ IMPS

⚪ NEFT

⚪ RTGS

-----------------------------------------------------

Authentication

🟢 JWT Authentication

🟢 Role Based Authorization

⚪ Refresh Token

⚪ OTP Verification

-----------------------------------------------------

Quality Engineering

🟢 Manual API Testing

🟢 Validation Testing

🟢 Security Testing

⚪ RestAssured Automation

⚪ Integration Testing

⚪ WireMock

⚪ Performance Testing

⚪ Contract Testing
# 🚀 Features

### ✅ Customer Management

* Enterprise Customer Registration
* Customer Number Generation
* Customer Validation
* Duplicate Customer Detection
* Customer Status Management
* KYC Status Initialization
* Risk Score Initialization

### 🔒 Security

* Global Exception Handling
* Standard Error Response
* Enterprise Validation Framework

---

# 📋 Current Progress

| Story     | Module                           | Status       	  |
| --------- | -------------------------------- | -----------  	  |
| Story-001 | Enterprise Customer Registration | ✅ Completed 	  |
| Story-002 | Customer Search API		       | ✅ Completed   	  |
| Story-003 | Cash Deposit                     | ⏳ Planned   	  |
| Story-004 | Cash Withdrawal                  | ⏳ Planned   	  |
| Story-005 | Fund Transfer                    | ⏳ Planned   	  |
| Story-006 | Transaction History              | ⏳ Planned   	  |
| Story-007 | Authentication & JWT             | ⏳ Planned   	  |
| Story-008 | Role-Based Authorization         | ⏳ Planned   	  |
| Story-009 | Fraud Detection                  | ⏳ Planned   	  |
| Story-010 | Notification Service             | ⏳ Planned   	  |

---

# 🔢 Customer Number Format

Customer numbers are generated using a **PostgreSQL Sequence**, ensuring uniqueness even when multiple customer registrations occur simultaneously.

Example:

```text
CUST000001
CUST000002
CUST000003
```

---

# 📌 Completed Stories

## Story-001 – Enterprise Customer Registration

Implemented:

* Layered Architecture
* Customer Entity
* Customer Repository
* Customer Service
* Customer Controller
* Customer Request DTO
* Customer Response DTO
* PostgreSQL Sequence
* Duplicate Validation
* Enterprise Exception Handling
* Global Exception Handler
* Customer Number Generation
* Enterprise Default Initialization
* AI Assisted Fraud Detection
* AI Transaction Risk Scoring
* LLM Powered Customer Support

## Story-002 – Customer Search API

Implemented

* GET Customer by Customer Number
* Optional.orElseThrow()
* CustomerNotFoundException
* DTO Mapping
* REST Response
* HTTP 200
* HTTP 404
* Input Validation

---

# 🔮 Planned Enhancements

* JWT Authentication
* Refresh Tokens
* Role-Based Authorization
* Account Management
* Transaction Management
* Loan Management
* Credit Card Module
* Fraud Detection Engine
* Notification Service
* Audit Logging
* Docker
* Kubernetes
* Redis
* Kafka
* Swagger / OpenAPI
* Jenkins CI/CD
* Selenium Automation
* RestAssured API Automation

---

# 🤝 Contributing

Contributions, suggestions, and improvements are welcome.

Please open an issue or submit a pull request after discussing significant changes.

---

# 📄 License

This repository is intended for educational purposes and enterprise software engineering practice.
## Development Journal

#### Story-001 Completion

✔ Positive API testing completed
✔ Duplicate PAN validation (409 Conflict)
✔ Invalid Email validation
✔ Invalid Phone Number validation
✔ Invalid PAN validation
✔ Invalid Aadhaar validation
✔ Invalid Customer Name validation
✔ Centralized exception handling verified
✔ Enterprise error responses validated
#### Today's Achievement

✔ Completed Story-002

Implemented four enterprise search APIs using:

- Spring Boot
- Spring Data JPA
- Custom Exceptions
- Optional.orElseThrow()
- Input Normalization
- Enterprise Validation
- RESTful API Design

All APIs have been manually validated using Postman with positive and negative test cases.
Current Status:
Story-001 Customer Registration completed successfully.
## 📅 Latest Progress (30-Jun-2026)

### Story-001 Enterprise Customer Registration

#### Development
- ✅ Enterprise validation framework completed
- ✅ Global exception handling implemented
- ✅ PostgreSQL sequence-based customer number generation
- ✅ Enterprise error response structure

#### Testing Completed
- ✅ Positive Testing
- ✅ Mandatory Field Validation
- ✅ Duplicate Validation
- ✅ Format Validation
- ✅ Boundary Value Analysis (Age 17, 18, 19)
- ✅ HTTP Method Validation (GET/PUT/DELETE)
- ✅ Malformed JSON validation

#### Defect Fixed
- Fixed malformed JSON request returning **500 Internal Server Error**.
- Added `HttpMessageNotReadableException` handler.
- API now correctly returns:
  - HTTP 400 Bad Request
  - `INVALID_REQUEST`
  - `Malformed JSON request.`
  
  ## 📅 Latest Progress (01-Jul-2026)

### Story-002 Customer Search API

#### Development

- ✅ GET Customer by Customer Number
- ✅ Customer Search Service
- ✅ CustomerNotFoundException
- ✅ DTO Mapping
- ✅ Repository Search using Optional
- ✅ Spring Security updated for GET endpoint

#### Testing Completed

- ✅ Existing Customer (200 OK)
- ✅ Customer Not Found (404)
- ✅ Blank Customer Number
- ✅ Invalid Customer Number
- ✅ Security Validation

#### Design Improvements

- Added Customer Number validation before database access.
- Applied RESTful HTTP status codes.
- Improved service layer exception handling.
# 📅 Progress Update (02-Jul-2026)

## ✅ Story-002 Completed - Enterprise Customer Search APIs

Implemented enterprise-grade customer search functionality with proper validation, normalization, exception handling and testing.

### Implemented APIs

| API | Status |
|------|--------|
| GET /api/v1/customers/{customerNumber} | ✅ Completed |
| GET /api/v1/customers/search?pan= | ✅ Completed |
| GET /api/v1/customers/search/email?email= | ✅ Completed |
| GET /api/v1/customers/search/phone?phone= | ✅ Completed |

---

## Enterprise Features Implemented

- ✅ Input normalization
- Trim whitespace
- PAN converted to uppercase
- Email converted to lowercase

- ✅ Business validation
- Customer Number validation
- PAN format validation
- Email format validation
- Phone number validation

- ✅ Exception handling
- InvalidCustomerException
- CustomerNotFoundException

- ✅ Repository search using Spring Data JPA

- ✅ RESTful HTTP Status Codes
- 200 OK
- 400 Bad Request
- 404 Not Found

---

## Testing Completed

### Positive Testing

- Existing Customer Number
- Existing PAN
- Existing Email
- Existing Phone Number

### Negative Testing

- Invalid Customer Number
- Invalid PAN
- Invalid Email
- Invalid Phone

### Boundary & Validation

- Leading spaces
- Trailing spaces
- Lowercase PAN
- Uppercase Email
- Invalid regex
- Unknown customer
- Empty values
- Duplicate scenarios

---

## Enterprise Concepts Learned

- Spring Data JPA Query Methods
- Optional<T>
- orElseThrow()
- Custom Exceptions
- DTO Mapping
- Input Normalization
- REST API Design
- HTTP Status Codes
- Layered Architecture

## Playwright UI and API automation

The primary framework is [Playwright Java](automation-java/README.md), using Java 17, TestNG, and Maven for UI and API acceptance tests. Start with the [hands-on handbook](automation-java/docs/HANDBOOK.md) and [demo architecture](automation-java/docs/ARCHITECTURE.md). The earlier TypeScript proof of concept remains in automation as historical reference.
