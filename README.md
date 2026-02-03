# Microservices E-Commerce Project

A complete microservices architecture demonstration using Spring Boot, Eureka, and OpenFeign.

## Project Overview

This project consists of 5 microservices + Web UI:

1. **Eureka Server** (Port 8761) - Service Discovery
2. **API Gateway** (Port 8080) - Single entry point, routing, load balancing
3. **Product Service** (Port 8081) - Manages products and inventory
4. **User Service** (Port 8082) - Manages user information
5. **Order Service** (Port 8083) - Handles orders with Feign clients
6. **Web UI** - HTML/JS client for testing the system

## Architecture

```
                        ┌─────────────────┐
                        │  Eureka Server  │ (Service Registry)
                        │   Port: 8761    │
                        └────────┬────────┘
                                 │
                    ┌────────────┴────────────┐
                    │                         │
              ┌─────▼──────┐           ┌─────▼──────────────┐
              │API Gateway │           │  All Services      │
              │ Port: 8080 │◄──────────┤  Register Here     │
              │            │           └────────────────────┘
              │  Routes:   │
              │  /api/**   │
              └─────┬──────┘
                    │
        ┌───────────┼───────────┐
        │           │           │
┌───────▼───┐  ┌───▼──────┐  ┌─▼──────────┐
│  Product  │  │   User   │  │   Order    │
│  Service  │  │ Service  │  │  Service   │
│ Port:8081 │  │Port:8082 │  │ Port:8083  │
│ [RedisDB] │  │ [H2 DB]  │  │  [H2 DB]   │
└───────────┘  └──────────┘  └─────┬──────┘
                                    │
                  ┌─────────────────┴────────────┐
                  │    OpenFeign Clients         │
                  │  - ProductClient             │
                  │  - UserClient                │
                  └──────────────────────────────┘

┌──────────────────────────────────────────────┐
│            Web UI (Client)                   │
│       http://localhost:8080/                 │
│    Communicates via API Gateway              │
└──────────────────────────────────────────────┘
```

## Technologies Used

- **Java 17**
- **Spring Boot 3.2.1**
- **Spring Cloud Gateway** - API Gateway with routing, filtering, load balancing
- **Netflix Eureka** - Service Discovery
- **OpenFeign** - Inter-service Communication
- **Spring Data JPA** - Database Access
- **H2 Database** - In-memory Database
- **Redis Database** - Used as high performance Database
- **Lombok** - Reduce Boilerplate Code
- **Maven** - Build Tool
- **HTML/CSS/JavaScript** - Web UI


## Prerequisites

- Java 17 or higher
- Maven 3.6+
- Docker (for Redis)

## Setup Instructions

### Setup REDIS for Product service
```
docker run --name redis-local -p 6379:6379 -d redis:7-alpine
docker exec -it redis-local redis-cli

```

### Setup AWS user account for Order service to send email

Set the below environment values to be used by Order Service.
```
export AWS_ACCESS_KEY_ID=<>
export AWS_SECRET_ACCESS_KEY=<>
export AWS_REGION=ap-south-1

```


### Build All Services

```bash
# Build Eureka Server
cd eureka-server
mvn clean install
cd ..

# Build Product Service
cd product-service
mvn clean install
cd ..

# Build User Service
cd user-service
mvn clean install
cd ..

# Build Order Service
cd order-service
mvn clean install
cd ..


```

### Run the Services

**IMPORTANT: Start services in this order:**

Open 5 separate terminals:

```bash
# Terminal 1 - Eureka Server
cd eureka-server
mvn spring-boot:run

# Terminal 2 - API Gateway (wait for Eureka to start)
cd api-gateway
mvn spring-boot:run

# Terminal 3 - Product Service (wait for Eureka to start)
cd product-service
mvn spring-boot:run

# Terminal 4 - User Service (wait for Eureka to start)
cd user-service
mvn spring-boot:run

# Terminal 5 - Order Service (wait for all services to register)
cd order-service
mvn spring-boot:run
```

### Verify Services are Running

- **Eureka Dashboard**: http://localhost:8761
  - You should see all 4 services registered (api-gateway, product-service, user-service, order-service)
- **API Gateway**: http://localhost:8080/actuator/health
- **Product Service** (via Gateway): http://localhost:8080/api/products
- **User Service** (via Gateway): http://localhost:8080/api/users
- **Order Service** (via Gateway): http://localhost:8080/api/orders

### Open the Web UI

Open `web-ui/index.html` in your browser

**The Web UI provides:**
- Product management interface
- User management interface
- Order creation and viewing
- Real-time API testing through API Gateway

## Testing the Microservices

### Using the Web UI 

1. Open `web-ui/index.html` in your browser
2. Create users, products, and orders through the interface
3. View all data in beautiful cards


**Note**: The `name` attribute in `@FeignClient` matches the `spring.application.name` in the respective service's `application.yml`.


## API Endpoints Summary

**All endpoints accessible via API Gateway: http://localhost:8080**

### Product Service
- `POST /api/products` - Create product
- `GET /api/products` - Get all products
- `GET /api/products/{id}` - Get product by ID
- `PUT /api/products/{id}` - Update product
- `DELETE /api/products/{id}` - Delete product

### User Service
- `POST /api/users` - Create user
- `GET /api/users` - Get all users
- `GET /api/users/{id}` - Get user by ID
- `PUT /api/users/{id}` - Update user
- `DELETE /api/users/{id}` - Delete user

### Order Service
- `POST /api/orders` - Create order
- `GET /api/orders` - Get all orders
- `GET /api/orders/{id}` - Get order by ID
- `GET /api/orders/user/{userId}` - Get orders by user
- `PUT /api/orders/{id}/status?status=SHIPPED` - Update order status
- `DELETE /api/orders/{id}` - Cancel order

**Direct service access (for debugging):**
- Product Service: http://localhost:8081/api/products
- User Service: http://localhost:8082/api/users
- Order Service: http://localhost:8083/api/orders
