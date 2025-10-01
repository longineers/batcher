# Spring Batch CSV to PostgreSQL Importer

## Overview

This project is a Spring Boot application that demonstrates how to efficiently import large datasets from a CSV file into a PostgreSQL database using Spring Batch. It's a common scenario in data-driven applications where large amounts of data need to be processed and stored in a structured manner.

## The Problem

Imagine you have a very large CSV file containing millions of product records. Reading the entire file into memory and inserting the data row-by-row into a database would be inefficient and could lead to memory issues and slow performance. This project solves this problem by using Spring Batch to process the data in manageable chunks, providing a robust and scalable solution for batch processing.

## Solution Diagram

The following diagram illustrates the architecture of the solution:

```mermaid
graph TD
    subgraph "Input"
        A[massive_products.csv]
    end

    subgraph "Spring Batch Application"
        B[csvImportJob]
        C[csvImportStep]
        D[FlatFileItemReader]
        H[CompositeItemProcessor]
        I[categoryFilterProcessor]
        J[customiseLinkProcessor]
        E[JpaItemWriter]

        B --> C;
        C --> D;
        C --> H;
        C --> E;
        D -- reads --> A;
        H --> I;
        H --> J;
    end

    subgraph "Database"
        F[PostgreSQL]
        G[products table]

        E -- writes to --> F;
        F -- contains --> G;
    end
```

## Item Processors

The application uses a `CompositeItemProcessor` to chain multiple processors together. This allows for modular and reusable processing steps.

### Category Filter Processor

The `categoryFilterProcessor` filters products based on a list of categories provided as a job parameter. If no categories are provided, it allows all products to pass through.

### Customise Link Processor

The `customiseLinkProcessor` generates a custom link for each product by appending a suffix to the product's image URL. The suffix is configurable via the `customise.link.suffix` property in `application.properties`.

## Technologies Used

*   **Java 17:** The programming language used for the project.
*   **Spring Boot:** Provides a fast and easy way to create stand-alone, production-grade Spring based Applications.
*   **Spring Batch:** A lightweight, comprehensive batch framework designed to enable the development of robust batch applications.
*   **Spring Data JPA:** Simplifies data access in a Spring application.
*   **PostgreSQL:** A powerful, open source object-relational database system.
*   **Flyway:** An open-source database migration tool.
*   **Lombok:** A Java library that automatically plugs into your editor and build tools to reduce boilerplate code.
*   **Gradle:** A build automation tool.
*   **Docker:** Used to containerize the PostgreSQL database for easy setup.

## Setup and Configuration

1.  **Prerequisites:**
    *   Java 17 or higher
    *   Docker and Docker Compose

2.  **Database Setup:**
    *   The project uses a PostgreSQL database, which can be easily started using Docker Compose:
        ```bash
        docker-compose up -d
        ```

## Configuration

The application can be configured using environment variables and properties in `application.properties`.

### Environment Variables

*   `DB_NAME`: The name of the database (e.g., `batcher`).
*   `DB_USER`: The username for the database (e.g., `batcher`).
*   `DB_PASSWORD`: The password for the database (e.g., `password`).

### Application Properties

*   `batch.chunk-size`: The number of items to process in each chunk (default: `1000`).
*   `customise.link.suffix`: The suffix to append to the image URL to create the customise link (default: `?source=batcher`).

## How to Run

1.  **Using Gradle:**
    *   You can run the application using the Gradle wrapper. Make sure to provide the required environment variables.
        ```bash
        DB_NAME=batcher DB_USER=batcher DB_PASSWORD=password ./gradlew bootRun
        ```

2.  **From the JAR file:**
    *   First, build the application:
        ```bash
        ./gradlew build
        ```
    *   Then, run the generated JAR file:
        ```bash
        DB_NAME=batcher DB_USER=batcher DB_PASSWORD=password java -jar build/libs/batcher-0.0.1-SNAPSHOT.jar
        ```

## Triggering the Job

The batch job can be triggered by sending a POST request to the `/run` endpoint.
This endpoint optionally accepts a JSON body to filter the import by product categories.

### Examples

**1. Run the job without any filtering:**

```bash
curl -X POST http://localhost:8080/run
```

**2. Run the job with category filtering:**

To import only products belonging to specific categories, you can provide a JSON array of category names in the request body.

```bash
curl -X POST http://localhost:8080/run \
-H "Content-Type: application/json" \
-d '{
    "categories": ["Electronics", "Home Appliances"]
}'
```
This will import only the products that belong to the "Electronics" or "Home Appliances" categories.

## How to Run Tests

To run the tests, use the following command:

```bash
./gradlew test
```
