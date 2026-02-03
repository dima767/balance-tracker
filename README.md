# Balance Tracker

[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-25-orange.svg)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0-green.svg)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-17-blue.svg)](https://www.postgresql.org/)

A personal finance web application for tracking checking account balances across bill payment periods. Built with Spring Boot 4.0, PostgreSQL, and modern frontend technologies.

## Features

- **Payment Periods** - Track balances across billing cycles with start/end dates
- **Payment Items** - Record individual expenses with payees, amounts, and notes
- **Payee Management** - Reusable payees with autocomplete suggestions
- **Auto-calculation** - Ending balance computed automatically as you add items
- **Reports & Analytics** - Visual charts and spending statistics
- **Keyboard Shortcuts** - Productivity shortcuts for power users
- **Responsive Design** - Works on desktop, tablet, and mobile
- **Dark-friendly UI** - Modern, clean interface

## Quick Start

Choose your preferred setup method:

| Method | Best For | Requirements |
|--------|----------|--------------|
| [Docker Standalone](#docker-standalone-recommended) | Quick demo, no Java needed | Docker only |
| [Local Development](#local-development) | Contributing, debugging | Java 25+, Docker |

---

## Docker Standalone (Recommended)

Run the complete application in Docker containers. No Java installation required.

### Prerequisites

- [Docker Desktop](https://www.docker.com/products/docker-desktop/) (includes Docker Compose)

### Step 1: Clone and Start

```bash
git clone https://github.com/dima767/balance-tracker.git
cd balance-tracker

# Build and start (first time)
./compose-up-standalone.sh -b
```

The first build takes a few minutes to download dependencies and compile.

### Step 2: Access the Application

Open your browser to: **https://localhost:8443/balancetracker**

> **SSL Certificate Warning:** Your browser will show a security warning because the app uses a self-signed certificate. This is normal for local development.
>
> - **Chrome/Edge:** Click "Advanced" → "Proceed to localhost (unsafe)"
> - **Firefox:** Click "Advanced" → "Accept the Risk and Continue"
> - **Safari:** Click "Show Details" → "visit this website"

### Step 3: Start Using

1. Click **"New Period"** to create your first payment period
2. Enter the period date and starting balance
3. Add payment items (bills, expenses) with payees and amounts
4. Watch the ending balance calculate automatically

### Managing the Application

```bash
# Start (after first build)
./compose-up-standalone.sh

# Stop (keeps your data)
./compose-down-standalone.sh

# Stop and delete all data
./compose-down-standalone.sh -v

# Rebuild after code changes
./compose-up-standalone.sh -b

# View logs
docker compose -f docker-compose.standalone.yml logs -f app
```

### Customizing Ports

If ports 8443/8080 are already in use, create a `.env` file:

```bash
cp .env.example .env
```

Edit `.env` to change ports:

```properties
APP_HTTPS_PORT=9443
APP_HTTP_PORT=9080
```

Then restart:

```bash
./compose-down-standalone.sh
./compose-up-standalone.sh
```

---

## Local Development

Run the application locally with hot reload for development and debugging.

### Prerequisites

| Requirement | Version | Check Command |
|-------------|---------|---------------|
| Java JDK | 25+ | `java -version` |
| Docker | Latest | `docker --version` |
| Docker Compose | Latest | `docker compose version` |

**Install Java 25:**
- macOS: `brew install openjdk@25`
- Linux: [Eclipse Temurin](https://adoptium.net/)
- Windows: [Eclipse Temurin](https://adoptium.net/)

### Step 1: Start the Database

```bash
./compose-up.sh
```

This starts PostgreSQL 17 on `localhost:5432`.

### Step 2: Build and Run

```bash
# Build and run
./build-and-run.sh -b

# Or just run (if already built)
./build-and-run.sh
```

### Step 3: Access the Application

Open: **https://localhost:8443/balancetracker**

(See SSL certificate warning note above)

### Development Commands

| Command | Description |
|---------|-------------|
| `./compose-up.sh` | Start PostgreSQL database |
| `./compose-down.sh` | Stop database (keep data) |
| `./compose-down.sh -v` | Stop database and delete data |
| `./build-and-run.sh` | Run application (must be built) |
| `./build-and-run.sh -b` | Build and run |
| `./build-and-run.sh -br` | Force rebuild and run |
| `./generate-certs.sh` | Regenerate SSL certificate |
| `./gradlew test` | Run tests |
| `./gradlew build` | Build without running |

### IDE Setup

**IntelliJ IDEA:**
1. Open the project folder
2. Import as Gradle project
3. Set Project SDK to Java 25
4. Run `BalanceTrackerApplication.java`

**VS Code:**
1. Install "Extension Pack for Java"
2. Open the project folder
3. Run via the Run/Debug panel

### Remote Debugging

The application starts with debug port **5005** enabled.

**IntelliJ IDEA:**
1. Run → Edit Configurations → Add New → Remote JVM Debug
2. Host: `localhost`, Port: `5005`
3. Click Debug

### Hot Reload

Changes to Thymeleaf templates and static resources reload automatically. For Java changes, restart the application.

---

## SSL Certificate Configuration

Both modes use self-signed SSL certificates for HTTPS.

### Local Development

The certificate is pre-generated at `src/main/resources/ssl/keystore.p12`.

**To regenerate with custom hostnames:**

```bash
# Default (localhost)
./generate-certs.sh

# Custom hostnames
CERT_HOSTS="localhost,myapp.local" ./generate-certs.sh
```

**To use a custom hostname:**

1. Add to `/etc/hosts` (or `C:\Windows\System32\drivers\etc\hosts` on Windows):
   ```
   127.0.0.1   myapp.local
   ```

2. Generate certificate:
   ```bash
   CERT_HOSTS="localhost,myapp.local" ./generate-certs.sh
   ```

3. Rebuild: `./build-and-run.sh -b`

4. Access: https://myapp.local:8443/balancetracker

### Docker Standalone

Certificates are auto-generated on first start and stored in a Docker volume.

**To use a custom hostname:**

1. Add to `/etc/hosts`:
   ```
   127.0.0.1   balancetracker.local
   ```

2. Create/edit `.env`:
   ```bash
   cp .env.example .env
   ```

   Set:
   ```properties
   CERT_HOSTS=localhost,balancetracker.local
   ```

3. Rebuild (regenerates certificate):
   ```bash
   ./compose-down-standalone.sh -v
   ./compose-up-standalone.sh -b
   ```

4. Access: https://balancetracker.local:8443/balancetracker

---

## Configuration Reference

### Environment Variables (Docker Standalone)

Create a `.env` file from the template:

```bash
cp .env.example .env
```

| Variable | Default | Description |
|----------|---------|-------------|
| `APP_HTTPS_PORT` | 8443 | Host HTTPS port |
| `APP_HTTP_PORT` | 8080 | Host HTTP port |
| `POSTGRES_DB` | balancetracker | Database name |
| `POSTGRES_USER` | balancetracker | Database username |
| `POSTGRES_PASSWORD` | balancetracker | Database password |
| `CERT_HOSTS` | localhost,balancetracker.local | SSL certificate hostnames |
| `CERT_IPS` | 127.0.0.1,0.0.0.0 | SSL certificate IPs |
| `JAVA_OPTS` | -Xms256m -Xmx512m | JVM memory settings |

### Memory Tuning

Adjust `JAVA_OPTS` in `.env` based on available RAM:

| System RAM | Recommended Setting |
|------------|---------------------|
| 512MB | `-Xms128m -Xmx384m` |
| 1GB | `-Xms256m -Xmx512m` (default) |
| 2GB | `-Xms512m -Xmx1g` |
| 4GB+ | `-Xms1g -Xmx2g` |

---

## Troubleshooting

### Port Already in Use

**Error:** `Bind for 0.0.0.0:8443 failed: port is already allocated`

**Solution:** Change ports in `.env`:
```properties
APP_HTTPS_PORT=9443
APP_HTTP_PORT=9080
```

### Database Connection Failed

**Error:** `Connection to localhost:5432 refused`

**Solution:**
```bash
# Check if PostgreSQL is running
docker ps | grep postgres

# Restart database
./compose-down.sh
./compose-up.sh
```

### SSL Certificate Issues

**Error:** Browser shows NET::ERR_CERT_INVALID

**Solution:** This is expected with self-signed certificates. Proceed through the warning (see instructions above).

**To regenerate certificates:**
```bash
# Local development
./generate-certs.sh
./build-and-run.sh -b

# Docker standalone
./compose-down-standalone.sh -v
./compose-up-standalone.sh -b
```

### Build Failures

**Error:** `gradlew: Permission denied`

**Solution:**
```bash
chmod +x gradlew
chmod +x *.sh
```

### Docker Build Issues

**Error:** Build fails with memory errors

**Solution:** Increase Docker Desktop memory allocation:
- Docker Desktop → Settings → Resources → Memory → Set to 4GB+

---

## Project Structure

```
balance-tracker/
├── src/
│   ├── main/
│   │   ├── java/dk/balancetracker/
│   │   │   ├── config/        # Spring configuration
│   │   │   ├── domain/        # JPA entities
│   │   │   ├── repository/    # Data access layer
│   │   │   ├── service/       # Business logic
│   │   │   └── web/           # Controllers
│   │   └── resources/
│   │       ├── static/        # CSS, JavaScript
│   │       ├── templates/     # Thymeleaf views
│   │       └── ssl/           # SSL keystore
│   └── test/                  # Test classes
├── docker-compose.yml              # Dev database only
├── docker-compose.standalone.yml   # Full stack
├── Dockerfile                      # Application container
├── build.gradle                    # Gradle build config
└── README.md
```

## Tech Stack

| Component | Technology |
|-----------|------------|
| Backend | Spring Boot 4.0, Java 25 |
| Database | PostgreSQL 17 |
| ORM | Spring Data JPA, Hibernate |
| Currency | Java Money API (JSR 354) |
| Templates | Thymeleaf 3.1 |
| Frontend | htmx 2.0, Bootstrap 5.3 |
| Charts | Chart.js |
| Build | Gradle 9.2 |
| Testing | JUnit 6, Testcontainers |

## Contributing

1. Fork the repository
2. Create a feature branch: `git checkout -b feature/my-feature`
3. Make your changes
4. Run tests: `./gradlew test`
5. Commit: `git commit -m "Add my feature"`
6. Push: `git push origin feature/my-feature`
7. Open a Pull Request

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

**Questions or issues?** Please [open an issue](https://github.com/dima767/balance-tracker/issues) on GitHub.
