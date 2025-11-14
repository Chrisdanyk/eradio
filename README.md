# E-Radio Backend

A RESTful API backend for an online radio station application built with Spring Boot. This application provides endpoints for user authentication, radio station search, and favorite management.

## 🚀 Features

- **User Authentication**: JWT-based authentication with registration and login
- **Radio Station Search**: Search and filter radio stations by name, country, language, or tags
- **Favorites Management**: Add, remove, and retrieve favorite radio stations
- **API Documentation**: Interactive Swagger/OpenAPI documentation
- **Health Monitoring**: Spring Boot Actuator for health checks and monitoring
- **Database Support**: H2 (development) and PostgreSQL (production)
- **Security**: Spring Security with JWT tokens

## 🛠️ Tech Stack

- **Java 17**
- **Spring Boot 3.5.7**
- **Spring Security** - Authentication and authorization
- **Spring Data JPA** - Database access
- **PostgreSQL** - Production database
- **H2 Database** - Development database
- **JWT (jjwt)** - Token-based authentication
- **SpringDoc OpenAPI** - API documentation
- **Maven** - Build tool
- **Lombok** - Boilerplate code reduction

## 📋 Prerequisites

- Java 17 or higher
- Maven 3.6+ (or use the included Maven wrapper)
- PostgreSQL 12+ (for production)
- Docker (optional, for containerized deployment)

## 🔧 Installation

### 1. Clone the Repository

```bash
git clone https://github.com/Chrisdanyk/eradio.git
cd eradio/backend
```

### 2. Configure Environment Variables

Create a `.env` file in the root directory:

```bash
# Spring Profile
SPRING_PROFILES_ACTIVE=dev

# Server Configuration
SERVER_PORT=8080

# Database Configuration (Production)
DB_URL=jdbc:postgresql://localhost:5432/eradio
DB_USERNAME=your_username
DB_PASSWORD=your_password

# JWT Configuration
JWT_SECRET=your-512-bit-secret-key-must-be-at-least-64-characters-long-for-hs512-algorithm-in-production-use-please-change-this
JWT_EXPIRATION=86400000

# Radio Browser API
RADIO_BROWSER_API_URL=https://de1.api.radio-browser.info
```

> **Note**: For production, use a strong, randomly generated JWT secret key.

### 3. Build the Application

```bash
./mvnw clean install
```

Or with Maven installed:

```bash
mvn clean install
```

## 🏃 Running the Application

### Development Mode

```bash
./mvnw spring-boot:run
```

The application will start on `http://localhost:8080` (or the port specified in `SERVER_PORT`).

### Production Mode

Set the profile to `prod` in your `.env` file:

```env
SPRING_PROFILES_ACTIVE=prod
```

Ensure PostgreSQL is running and configured, then start the application:

```bash
./mvnw spring-boot:run
```

## 🐳 Docker Deployment

### Build Docker Image

```bash
docker build -t eradio-backend:latest .
```

### Run with Docker

```bash
docker run -d \
  --name eradio-backend \
  -p 8080:8080 \
  --env-file .env \
  eradio-backend:latest
```

### Docker Compose (Recommended)

Create a `docker-compose.yml`:

```yaml
version: '3.8'

services:
  backend:
    build: .
    container_name: eradio-backend
    ports:
      - "8080:8080"
    env_file:
      - .env
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - DB_URL=jdbc:postgresql://postgres:5432/eradio
    depends_on:
      - postgres
    healthcheck:
      test: ["CMD", "wget", "--no-verbose", "--tries=1", "--spider", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 3s
      retries: 3
      start_period: 40s

  postgres:
    image: postgres:15-alpine
    container_name: eradio-postgres
    environment:
      - POSTGRES_DB=eradio
      - POSTGRES_USER=${DB_USERNAME}
      - POSTGRES_PASSWORD=${DB_PASSWORD}
    volumes:
      - postgres_data:/var/lib/postgresql/data
    ports:
      - "5432:5432"

volumes:
  postgres_data:
```

Run with Docker Compose:

```bash
docker-compose up -d
```

## 📚 API Documentation

Once the application is running, access the interactive API documentation:

- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **OpenAPI JSON**: http://localhost:8080/v3/api-docs

### API Endpoints

#### Authentication (`/api/auth`)

- `POST /api/auth/register` - Register a new user
- `POST /api/auth/login` - Login and get JWT token

#### Radio Stations (`/api/stations`)

- `GET /api/stations/search` - Search radio stations (requires authentication)
- `GET /api/stations/{id}` - Get station by ID (requires authentication)

#### Favorites (`/api/favorites`)

- `GET /api/favorites` - Get user's favorites (requires authentication)
- `POST /api/favorites/{stationId}` - Add station to favorites (requires authentication)
- `DELETE /api/favorites/{stationId}` - Remove station from favorites (requires authentication)

### Authentication

Most endpoints require JWT authentication. Include the token in the Authorization header:

```
Authorization: Bearer <your-jwt-token>
```

## 🧪 Testing

Run tests:

```bash
./mvnw test
```

## 🔍 Health Check

The application includes health check endpoints via Spring Boot Actuator:

- **Health**: http://localhost:8080/actuator/health
- **Info**: http://localhost:8080/actuator/info

## 📁 Project Structure

```
backend/
├── src/
│   ├── main/
│   │   ├── java/cg/xis/eradio/
│   │   │   ├── config/          # Configuration classes
│   │   │   ├── controller/      # REST controllers
│   │   │   ├── domain/          # Entities and repositories
│   │   │   ├── dto/             # Data Transfer Objects
│   │   │   ├── exception/       # Exception handlers
│   │   │   ├── infrastructure/  # External API clients
│   │   │   ├── security/        # Security configuration
│   │   │   └── service/          # Business logic
│   │   └── resources/
│   │       ├── application.yml       # Main configuration
│   │       ├── application-dev.yml   # Development profile
│   │       └── application-prod.yml  # Production profile
│   └── test/                    # Test files
├── .dockerignore
├── .gitignore
├── Dockerfile
├── pom.xml
└── README.md
```

## 🔐 Security

- JWT tokens for stateless authentication
- Password encryption using BCrypt
- Spring Security for endpoint protection
- CORS configuration for cross-origin requests

## 📝 Environment Variables

| Variable                 | Description                | Default | Required   |
| ------------------------ | -------------------------- | ------- | ---------- |
| `SPRING_PROFILES_ACTIVE` | Spring profile (dev/prod)  | `dev`   | No         |
| `SERVER_PORT`            | Application port           | `8080`  | No         |
| `JWT_SECRET`             | Secret key for JWT signing | -       | Yes        |
| `JWT_EXPIRATION`         | JWT expiration time (ms)   | -       | Yes        |
| `DB_URL`                 | Database connection URL    | -       | Yes (prod) |
| `DB_USERNAME`            | Database username          | -       | Yes (prod) |
| `DB_PASSWORD`            | Database password          | -       | Yes (prod) |
| `RADIO_BROWSER_API_URL`  | Radio Browser API base URL | -       | Yes        |

## 🐛 Troubleshooting

### Port Already in Use

If port 8080 is already in use, change it in your `.env` file:

```env
SERVER_PORT=8081
```

### Database Connection Issues

- Ensure PostgreSQL is running
- Verify database credentials in `.env`
- Check database URL format: `jdbc:postgresql://host:port/database`

### JWT Token Issues

- Ensure `JWT_SECRET` is set and at least 64 characters long
- Check token expiration time

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 📄 License

This project is licensed under the MIT License.

## 👤 Author

**Chrisdanyk**

- GitHub: [@Chrisdanyk](https://github.com/Chrisdanyk)

## 🙏 Acknowledgments

- Radio Browser API for radio station data
- Spring Boot community
- All contributors and users

---

For more information, visit the [API Documentation](http://localhost:8080/swagger-ui.html) when the application is running.

