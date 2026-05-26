# Auth Service - Testing Guide

Comprehensive testing documentation for the Auth Service microservice. This guide covers test structure, execution, CI/CD integration, and best practices.

## Test Overview

**Total Test Count:** 76 tests  
**Test Execution Time:** ~8 seconds  
**Coverage:** Unit, Integration, Validation, and Security tests

### Test Distribution

| Test Suite | Count | Type | Coverage |
|------------|-------|------|----------|
| AuthApplicationTests | 7 | Component | Spring context loading, bean wiring |
| AuthControllerTest | 11 | Integration | REST endpoints, HTTP responses, internal endpoint security |
| AuthServiceTests | 10 | Unit | Business logic, authentication flow |
| JwtServiceTest | 16 | Unit/Security | Token generation, validation, parsing |
| LoginRequestValidationTest | 11 | Validation | Login DTO validation rules |
| RegisterRequestValidationTest | 19 | Validation | Register DTO validation |
| **Total** | **76** | - | **Comprehensive** |

## Running Tests

### Quick Start

```bash
cd auth-service
mvn clean test
```

### Run Specific Test Class

```bash
# Run one test class
mvn test -Dtest=AuthServiceTests

# Run multiple test classes
mvn test -Dtest=AuthServiceTests,JwtServiceTest

# Run with pattern
mvn test -Dtest=*ServiceTests
```

### Run Specific Test Method

```bash
mvn test -Dtest=AuthServiceTests#testSuccessfulRegistration
mvn test -Dtest=LoginRequestValidationTest#testValidLoginRequest
```

### Generate Coverage Report

```bash
# Generate JaCoCo coverage
mvn clean test jacoco:report

# Coverage report location
target/site/jacoco/index.html
```

### Run for CI/CD

```bash
# Run tests with debug output
mvn clean test -X -e

# Skip build, just run tests
mvn test -DskipBuild

# Strict execution (fail on warnings)
mvn clean test -DfailIfNoTests=false
```

## Test Breakdown

### 1. AuthApplicationTests (7 tests)

**Purpose:** Verify Spring Boot application context loads correctly

```java
@SpringBootTest
class AuthApplicationTests
```

**Test Cases:**
1. Application context loads
2. AuthController bean exists
3. AuthService bean exists
4. JwtService bean exists
5. UserRepository bean exists
6. PasswordEncoder bean exists
7. AuthenticationManager bean exists

**Run:**
```bash
mvn test -Dtest=AuthApplicationTests
```

### 2. AuthControllerTest (11 tests)

**Purpose:** Integration tests for REST endpoints

```java
@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest
```

**Test Cases:**

| Test | Endpoint | Expected | Validates |
|------|----------|----------|-----------|
| testSuccessfulRegistration | POST /api/auth/register | 201 Created | User created, token returned |
| testSuccessfulLogin | POST /api/auth/login | 200 OK | Login successful, token returned |
| testRegistrationWithDuplicateEmail | POST /api/auth/register | 409 Conflict | Email uniqueness enforced |
| testLoginWithInvalidEmail | POST /api/auth/login | 400 Bad Request | Email validation |
| testLoginWithBlankPassword | POST /api/auth/login | 400 Bad Request | Password required |
| testRegisterWithAllRoles | POST /api/auth/register | 201 Created | All roles (CUSTOMER, EMPLOYEE, ADMIN) |
| testInternalUserEmailLookup | GET /api/auth/internal/users/{id} | 200 OK | Internal lookup with valid token |
| testInternalUserEmailLookupInvalidToken | GET /api/auth/internal/users/{id} | 403 Forbidden | Internal token validation |
| testInternalUserEmailLookupNotFound | GET /api/auth/internal/users/{id} | 404 Not Found | Missing user behavior |

**Run:**
```bash
mvn test -Dtest=AuthControllerTest
```

**Example Request/Response:**
```bash
# Register
curl -X POST http://localhost:8081/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "fullName": "John Doe",
    "email": "john@example.com",
    "password": "Pass1234",
    "role": "CUSTOMER"
  }'

# Login
curl -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "john@example.com",
    "password": "Pass1234"
  }'
```

### 3. AuthServiceTests (10 tests)

**Purpose:** Unit tests for authentication business logic

```java
@ExtendWith(MockitoExtension.class)
class AuthServiceTests
```

**Mocks:**
- `UserRepository` - Database operations
- `AuthenticationManager` - Spring Security
- `JwtService` - Token generation

**Test Cases:**
1. Successful registration (all roles)
2. Registration with duplicate email
3. Successful login
4. Login with invalid credentials
5. Login with user not found
6. Password encoding verification
7. Multiple registrations
8. User entity mapping
9. Exception handling

**Run:**
```bash
mvn test -Dtest=AuthServiceTests
```

**Key Assertions:**
```java
// Verify registration saves user
verify(userRepository).save(any(User.class));

// Verify password is encoded
assertEquals(encoded, user.getPassword());

// Verify JWT token generated
assertNotNull(authResponse.getToken());
```

### 4. JwtServiceTest (16 tests)

**Purpose:** Unit tests for JWT token handling

```java
class JwtServiceTest
```

**Test Cases:**
1. Generate valid token
2. Extract username from token
3. Validate correct token
4. Reject expired token
5. Reject invalid signature
6. Reject malformed token
7. Token expiration within bounds
8. Multiple tokens are unique
9. Special characters in username
10. Large payload handling
11. Token consistency across validations
12. Claims extraction
13. Token generation with different users
14. Token format validation (JWT structure)
15. Signature algorithm verification
16. Token refresh scenarios

**Run:**
```bash
mvn test -Dtest=JwtServiceTest
```

**Key Assertions:**
```java
// Generate and validate token
String token = jwtService.generateToken("user@example.com");
assertTrue(jwtService.validateToken(token));
assertEquals("user@example.com", jwtService.extractUsername(token));
```

### 5. LoginRequestValidationTest (11 tests)

**Purpose:** Validate login request DTO constraints

```java
@DisplayName("LoginRequest Validation Tests")
class LoginRequestValidationTest
```

**Validations Tested:**
- Email validation (null, blank, invalid format)
- Password validation (null, blank, empty)
- Multiple simultaneous errors
- Various valid email formats

**Run:**
```bash
mvn test -Dtest=LoginRequestValidationTest
```

**Validation Rules:**
```
- email: @NotBlank, @Email
- password: @NotBlank
```

### 6. RegisterRequestValidationTest (19 tests)

**Purpose:** Validate registration request DTO constraints

```java
@DisplayName("RegisterRequest Validation Tests")
class RegisterRequestValidationTest
```

**Validations Tested:**
- Full name (null, blank, empty)
- Email (null, blank, invalid, various formats)
- Password (null, blank, minimum 8 characters)
- Role (all three types: CUSTOMER, EMPLOYEE, ADMIN)
- Multiple validation errors
- Edge cases (special characters, very long strings)

**Run:**
```bash
mvn test -Dtest=RegisterRequestValidationTest
```

**Validation Rules:**
```
- fullName: @NotBlank
- email: @NotBlank, @Email
- password: @NotBlank, min 8 characters
- role: @NotNull (optional, defaults to CUSTOMER)
```

## Test Infrastructure

### Test Dependencies

```xml
<!-- JUnit 5 -->
<dependency>
  <groupId>org.junit.jupiter</groupId>
  <artifactId>junit-jupiter</artifactId>
  <version>5.9.3</version>
  <scope>test</scope>
</dependency>

<!-- Mockito -->
<dependency>
  <groupId>org.mockito</groupId>
  <artifactId>mockito-core</artifactId>
  <version>5.2.1</version>
  <scope>test</scope>
</dependency>

<!-- Spring Test -->
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-test</artifactId>
  <version>4.0.5</version>
  <scope>test</scope>
</dependency>

<!-- H2 Database (in-memory for testing) -->
<dependency>
  <groupId>com.h2database</groupId>
  <artifactId>h2</artifactId>
  <version>2.1.214</version>
  <scope>test</scope>
</dependency>
```

### Test Configuration

**File:** `src/test/resources/application.properties`

```properties
# H2 In-Memory Database (Spring and tests use this automatically)
spring.datasource.url=jdbc:h2:mem:testdb
spring.datasource.driver-class-name=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=

# JPA/Hibernate
spring.jpa.database-platform=org.hibernate.dialect.H2Dialect
spring.jpa.hibernate.ddl-auto=create-drop
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.format_sql=false

# JWT Configuration
jwt.secret=test-secret-key-for-testing-purposes-min-256-bits
jwt.expiration=3600000

# Logging
logging.level.com.bank.auth=DEBUG
logging.level.org.springframework.security=DEBUG
logging.level.org.springframework.test=DEBUG
```

### Annotations Used

| Annotation | Purpose |
|-----------|---------|
| `@SpringBootTest` | Full integration test with Spring context |
| `@ExtendWith(MockitoExtension.class)` | Enable Mockito mocking |
| `@MockitoBean` | Mock Spring-managed bean (Spring Framework 7 / Boot 4 style) |
| `@Mock` | Mock non-Spring object |
| `@InjectMocks` | Inject mocks into class under test |
| `@BeforeEach` | Setup before each test |
| `@DirtiesContext` | Reset context between tests |
| `@DisplayName` | Human-readable test names |
| `@Test` | Mark as test method |
| `@AutoConfigureMockMvc` | Configure MockMvc for testing |

## Writing New Tests

### Test Template

```java
@DisplayName("Feature Description")
class FeatureTest {
    
    @Mock
    private SomeDependency dependency;
    
    @InjectMocks
    private ClassUnderTest classUnderTest;
    
    @BeforeEach
    void setUp() {
        // Common setup
    }
    
    @Test
    @DisplayName("Should do something when condition is met")
    void testSomething() {
        // Arrange - set up test data
        String input = "test";
        when(dependency.method()).thenReturn("result");
        
        // Act - call the method under test
        String result = classUnderTest.doSomething(input);
        
        // Assert - verify results
        assertEquals("expected", result);
        verify(dependency).method();
    }
}
```

### Best Practices

1. **Descriptive Names:** Use `test<Condition><Expected>` pattern
2. **AAA Pattern:** Always follow Arrange → Act → Assert
3. **One Assertion:** Test one behavior per test method
4. **Mocking:** Mock external dependencies, test the component
5. **DisplayName:** Add `@DisplayName` with clear descriptions
6. **Independence:** Tests should run independently in any order
7. **Cleanup:** Use `@BeforeEach` and `@AfterEach` for setup/teardown
8. **Exceptions:** Use `assertThrows()` for exception testing

## CI/CD Integration

### GitHub Actions Example

```yaml
name: Auth Service Tests

on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest
    
    steps:
    - uses: actions/checkout@v2
    
    - name: Set up Java
      uses: actions/setup-java@v2
      with:
        java-version: '17'
    
    - name: Build and Test
      run: |
        cd auth-service
        mvn clean test
    
    - name: Generate Coverage Report
      run: mvn jacoco:report
    
    - name: Upload Coverage
      uses: codecov/codecov-action@v2
```

### Pipeline Commands

```bash
# Full CI/CD test run
mvn clean test -DfailIfNoTests=false

# With coverage and reporting
mvn clean test jacoco:report surefire-report:report

# Strict mode (fail on any warning)
mvn clean test -X -e
```

## Debugging Tests

### Run Single Test with Debug

```bash
mvn test -Dtest=AuthServiceTests#testSuccessfulRegistration -Dmaven.surefire.debug
```

### View Test Output

```bash
# Verbose output
mvn test -e

# Extended debug
mvn test -X

# Show failed tests only
mvn test -rf :auth-service
```

### Test Reports

**Location:** `target/surefire-reports/`

- `TEST-*.xml` - Machine-readable test results
- `*.txt` - Human-readable test output

**View HTML Report:**
```bash
open target/site/surefire-report.html
```

## Troubleshooting

### Common Issues

**Tests hang/timeout**
```bash
# Add timeout parameter
mvn test -DtimeoutInMinutes=10
```

**Port already in use**
```bash
# Kill process or use random port
mvn test -Dserver.port=0
```

**Database locked**
```bash
# Clean and rebuild
mvn clean test
```

**Import errors in IDE**
```bash
# Refresh Maven project
mvn clean install
```

## Performance Optimization

Current results:
- Full test suite: ~8 seconds
- 76 tests executed
- No flaky tests (deterministic)
- H2 in-memory database (fast)

To maintain performance:
✅ Use in-memory H2 database for tests
✅ Mock external services
✅ Keep test data minimal
✅ Run tests in parallel (if supported)
✅ Avoid Thread.sleep() in tests

## Additional Resources

- [JUnit 5 Documentation](https://junit.org/junit5/docs/current/user-guide/)
- [Mockito Documentation](https://javadoc.io/doc/org.mockito/mockito-core/latest/org/mockito/Mockito.html)
- [Spring Boot Test Documentation](https://spring.io/guides/gs/testing-web/)
- [Jakarta Validation](https://jakarta.ee/specifications/validation/)
