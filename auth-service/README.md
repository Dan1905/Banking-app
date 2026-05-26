# Auth Service

A secure authentication and authorization microservice for the Banking App. Handles user registration, login, JWT token generation, and role-based access control.

## Features

- **User Registration** - Register new users with email and password validation
- **User Authentication** - Secure login with JWT token generation
- **JWT Token Management** - Token generation, validation, and expiration handling
- **Role-Based Access Control** - Support for CUSTOMER, EMPLOYEE, and ADMIN roles
- **Admin User Provisioning** - Admin-only user creation via gateway headers
- **Password Encryption** - Bcrypt password encoding with salt
- **Email Validation** - Prevent duplicate email registrations
- **Global Exception Handling** - Consistent error responses across the service

## Architecture

### Components

| Component | Purpose |
|-----------|---------|
| **AuthController** | REST API endpoints for registration and login |
| **AuthService** | Business logic for authentication operations |
| **JwtService** | JWT token generation, validation, and parsing |
| **SecurityConfig** | Spring Security configuration and password encoding |
| **UserRepository** | Database access layer for User entity |

### Data Model

#### User Entity
```java
- id: Long (Primary Key)
- email: String (Unique, Required)
- password: String (Encrypted, Required)
- fullName: String (Required)
- role: Role (CUSTOMER, EMPLOYEE, ADMIN)
- createdAt: LocalDateTime
```

#### Role Enum
- `CUSTOMER` - Regular banking customer (default)
- `EMPLOYEE` - Bank employee with elevated privileges
- `ADMIN` - System administrator

## API Endpoints

### Register User
```
POST /api/auth/register
Content-Type: application/json

Request Body:
{
  "fullName": "John Doe",
  "email": "john@example.com",
  "password": "SecurePass123"
}

Note: public registration always creates `CUSTOMER` users.

Success Response (201 Created):
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "email": "john@example.com",
  "fullName": "John Doe",
  "role": "CUSTOMER",
  "expiresIn": 86400
}

Error Responses:
- 400 Bad Request - Validation errors
- 409 Conflict - Email already exists
```

### Admin Create User (Gateway-Only)
```
POST /api/auth/admin/users
Content-Type: application/json
X-User-Role: ADMIN

Request Body:
{
  "fullName": "Jane Employee",
  "email": "jane@example.com",
  "password": "SecurePass123",
  "role": "EMPLOYEE"
}

Note: This endpoint trusts the API gateway to set `X-User-Role: ADMIN`.

Success Response (201 Created):
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "email": "jane@example.com",
  "fullName": "Jane Employee",
  "role": "EMPLOYEE",
  "expiresIn": 86400
}

Error Responses:
- 400 Bad Request - Validation errors
- 403 Forbidden - Admin role required
- 409 Conflict - Email already exists
```

### Login User
```
POST /api/auth/login
Content-Type: application/json

Request Body:
{
  "email": "john@example.com",
  "password": "SecurePass123"
}

Success Response (200 OK):
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "email": "john@example.com",
  "fullName": "John Doe",
  "role": "CUSTOMER",
  "expiresIn": 86400
}

Error Responses:
- 400 Bad Request - Invalid email or password format
- 401 Unauthorized - Invalid credentials
```

### Internal User Email Lookup (Service-to-Service)
```
GET /api/auth/internal/users/{userId}
X-Internal-Token: <internal-token>

Success Response (200 OK):
{
  "id": 42,
  "email": "john@example.com"
}

Error Responses:
- 403 Forbidden - Invalid internal token
- 404 Not Found - User does not exist
```

## Installation & Setup

### Prerequisites
- Java 17 or higher
- Maven 3.8+
- MySQL 8.0+ (for production)
- H2 (for testing/development)

### Development Setup

1. **Clone and navigate to auth-service:**
```bash
cd auth-service
```

2. **Build the project:**
```bash
mvn clean install
```

3. **Run the service:**
```bash
mvn spring-boot:run
```

The service will start on `http://localhost:8081` (default port).

### Configuration

Edit `src/main/resources/application.properties`:

```properties
# Server Configuration
server.port=8081

# Database Configuration
spring.datasource.url=jdbc:mysql://localhost:3306/auth_db?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true
spring.datasource.username=root
spring.datasource.password=root
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQLDialect
spring.jpa.properties.hibernate.format_sql=true

# JWT Configuration
jwt.secret=banking-app-super-secret-key-change-in-production-min32
jwt.expiration-ms=86400000

# Logging
logging.level.com.bank.auth=DEBUG
logging.level.org.springframework.security=INFO
```

## Testing

Full test suite with 76 tests covering all layers:
- **Unit Tests** - Service and JWT logic
- **Integration Tests** - Controller endpoints with MockMvc
- **Validation Tests** - DTO validation rules
- **Security Tests** - JWT token handling

### Run Tests

```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=AuthServiceTests

# Run with coverage report
mvn clean test jacoco:report

# Run for CI/CD with debug output
mvn clean test -X -e
```

See [TESTING.md](TESTING.md) for detailed testing documentation.

## Security Considerations

### Password Security
- Passwords are hashed using **BCrypt** with salt
- Plain text passwords are never stored
- Minimum password length: 8 characters
- Passwords are validated on registration

### JWT Token Security
- Tokens are signed with **HS256** algorithm
- Tokens expire after **24 hours** (configurable)
- Tokens include email (subject) and role claim
- Invalid or expired tokens are rejected

### Email Security
- Email format validation using regex
- Duplicate email prevention at database level
- Unique constraint on email column

### API Security
- `/api/auth/**` and `/actuator/health` are public
- CSRF protection is disabled for the stateless API
- CORS is not configured by default
- Use the API gateway or downstream services to enforce auth on protected endpoints
- The admin user creation endpoint trusts gateway-provided headers (e.g., `X-User-Role`)

## Error Handling

The service uses global exception handling with consistent error response format:

```json
{
  "timestamp": "2026-04-04T12:00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Email is required"
}
```

### Common Error Codes

| Status | Error | Cause |
|--------|-------|-------|
| 400 | Bad Request | Invalid input or validation failure |
| 401 | Unauthorized | Invalid credentials or expired token |
| 403 | Forbidden | Invalid internal token or forbidden operation |
| 404 | Not Found | Requested user not found |
| 409 | Conflict | Email already registered |
| 500 | Internal Server Error | Unexpected server error |

## Dependencies

Key dependencies:
- **Spring Boot 4.0.5** - Application framework
- **Spring Security** - Authentication & authorization
- **Spring Data JPA** - Database abstraction
- **JWT (io.jsonwebtoken)** - Token handling
- **Lombok** - Boilerplate reduction
- **Jakarta Validation** - Input validation
- **H2 Database** - In-memory DB for testing
- **JUnit 5** - Testing framework
- **Mockito** - Mocking library

See `pom.xml` for full dependency list.

## Database Schema

### Users Table
```sql
CREATE TABLE users (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  email VARCHAR(255) UNIQUE NOT NULL,
  password VARCHAR(255) NOT NULL,
  full_name VARCHAR(255) NOT NULL,
  role VARCHAR(50) NOT NULL DEFAULT 'CUSTOMER',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
);

CREATE INDEX idx_email ON users(email);
```

## Troubleshooting

### Common Issues

**Issue: Port already in use**
```bash
# Find and kill process on port 8081
lsof -i :8081
kill -9 <PID>

# Or change port in application.properties
server.port=8083
```

**Issue: Database connection failed**
- Verify MySQL is running
- Check database credentials in application.properties
- Ensure database exists: `CREATE DATABASE banking_auth;`

**Issue: JWT token validation fails**
- Verify `jwt.secret` is set and consistent
- Check token expiration time hasn't passed
- Ensure token format is correct (Bearer prefix)

**Issue: Tests failing**
- Run `mvn clean test` to clear cache
- Ensure Java 17+ is installed: `java -version`
- Check H2 database isn't locked

## Performance Metrics

Current performance benchmarks:

| Operation | Time | Note |
|-----------|------|------|
| Registration | 150-200ms | Includes password hashing |
| Login | 100-150ms | Token generation |
| Token Validation | 10-20ms | Per request |
| Full Test Suite | ~8 seconds | 76 tests |

## Contributing

1. Create feature branch: `git checkout -b feature/auth-feature`
2. Make changes and commit: `git commit -am 'Add feature'`
3. Push to branch: `git push origin feature/auth-feature`
4. Submit pull request

## License

This project is part of the Banking App microservices architecture.
