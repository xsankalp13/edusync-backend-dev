# EduSync.ai - Monolithic Backend (MVP)

[![Build Status](https://img.shields.io/your-ci/your-repo/main.svg)](https://your-ci-url.com)
[![Code Coverage](https://img.shields.io/codecov/c/github/your-user/your-repo.svg)](https://codecov.io)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

This repository contains the backend for EduSync.ai, a next-generation, white-labelled School ERP platform.

This project is the **Monolithic MVP**, built with **Spring Boot (Java)**. It is designed for single-tenant (per-school) deployment, prioritizing speed-to-market and feature delivery.

## 📚 Table of Contents

* [1. Architectural Strategy](#1-architectural-strategy)
* [2. Technology Stack](#2-technology-stack)
* [3. Getting Started](#3-getting-started)
    * [Prerequisites](#prerequisites)
    * [Installation & Setup](#installation--setup)
* [4. Running the Application](#4-running-the-application)
* [5. Testing](#5-testing)
* [6. API Documentation](#6-api-documentation)
* [7. Key Design Decisions](#7-key-design-decisions)
* [8. Future Migration Path](#8-future-migration-path)
* [9. Contributing](#9-contributing)

---

## 1. Architectural Strategy: The "Modular Monolith"

To balance MVP speed with long-term maintainability, this application is built as a **Modular Monolith**.

* **Single Application:** It is a single Spring Boot application, deploying as one `.jar` file and connecting to a single PostgreSQL database.
* **Logical Modules:** The codebase is separated into distinct "modules" (e.t., `iam`, `uis`, `attendance`). These modules represent future microservices.
* **Enforced Boundaries:** Modules are decoupled. They must not depend on each other directly. They communicate only through shared `Interfaces` or asynchronous `ApplicationEvents`.

This design allows for rapid development while ensuring that we can easily extract these modules into independent microservices in the future with minimal refactoring.

## 2. Technology Stack

* **Framework:** Spring Boot 3+ (Java 17+)
* **Data:** Spring Data JPA / Hibernate
* **Database:** PostgreSQL (One database per school deployment)
* **Security:** Spring Security (JWT for stateless authentication)
* **Build:** Maven
* **Testing:** JUnit 5, Mockito, Testcontainers
* **API Docs:** OpenAPI (Swagger)

## 3. Getting Started

Follow these steps to get a local development environment up and running.

### Prerequisites

You must have the following tools installed on your local machine:
* Java JDK 17+
* Apache Maven
* Docker & Docker Compose (for running the database)
* An IDE (IntelliJ IDEA is recommended)
* A Git client

### Installation & Setup

1.  **Clone the repository:**
    ```bash
    git clone [https://github.com/your-username/edusync-backend.git](https://github.com/your-username/edusync-backend.git)
    cd edusync-backend
    ```

2.  **Configure Environment Variables:**
    The application configuration is managed in `src/main/resources/application.yml`. You will need to override properties for your local environment. The most common way is via IDE environment variables or a local `application-local.yml` file.

    **Key properties to set:**
    ```yaml
    spring:
      datasource:
        url: jdbc:postgresql://localhost:5432/edusync_school_db
        username: your_db_user
        password: your_db_password
      jpa:
        hibernate:
          ddl-auto: validate # Use 'update' or 'create-drop' for initial setup
    
    # Your secret key for signing JWT tokens
    jwt:
      secret-key: 'your-32-byte-long-random-secret-key-goes-here'
    ```

3.  **Start Local Database:**
    We provide a `docker-compose.yml` file to easily start a local PostgreSQL database.
    ```bash
    docker-compose up -d postgres-db
    ```
    *This will start a PostgreSQL instance on `localhost:5432`.*

4.  **Build the Project:**
    Use Maven to download dependencies and build the application.
    ```bash
    mvn clean install
    ```

## 4. Running the Application

You can run the entire application from your IDE.

1.  Ensure your `postgres-db` Docker container is running.
2.  Open the project in your IDE (e.g., IntelliJ).
3.  Navigate to the main application class (e.g., `com.edusync.Application.java`).
4.  Run the `main()` method.

The server will start, typically on `http://localhost:8080`.

## 5. Testing

We use JUnit 5 for unit and integration testing.

* **Unit Tests:** Standard JUnit tests with Mockito for mocking dependencies.
* **Integration Tests:** We use `@SpringBootTest` and **Testcontainers** to spin up a real PostgreSQL database for each test run, ensuring our queries and JPA mappings are correct.

To run all tests:
```bash
mvn test
