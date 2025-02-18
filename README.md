# Office Management System

## Technologies Used

- Java 11
- Hibernate 6.2.7
- RESTEasy 6.2.7
- Undertow 2.3.10
- WildFly Application Server
- PostgreSQL
- HikariCP
- Maven

## Building and Running

### Building
Build the project and run tests:

```bash
mvn clean package
```

This will:
1. Compile the Java code
2. Run all tests
3. Package the application into a WAR file

### Running with WildFly
1. Start WildFly:
   ```bash
   mvn wildfly:run
   ```

2. The application will be available at `http://localhost:8080/office-management-system`

### Running Tests
Tests use an in-memory H2 database and don't require any additional configuration:

```bash
mvn test
```

## Database Setup

1. Make sure PostgreSQL is running and the `office_management` database is created.

## WildFly Configuration

Before running the application, you need to configure the WildFly datasource:

1. Start WildFly:
   ```bash
   ./mvnw wildfly:start
   ```

2. Execute the datasource configuration script:
   ```bash
   $WILDFLY_HOME/bin/jboss-cli.sh --connect --file=src/main/resources/configure-datasource.cli
   ```

   This will:
   - Create a PostgreSQL driver module
   - Configure the PostgreSQL JDBC driver
   - Create a datasource named `OfficeManagementDS`

### Creating WildFly Admin User

Before accessing the WildFly Admin Console, you need to create an admin user:

1. After starting WildFly, run the following command:
   ```bash
   target/server/bin/add-user.sh -u admin -p admin123 -g admin --silent
   ```
   This will create an admin user with:
   - Username: `admin`
   - Password: `admin123`
   - Group: `admin`

2. You can now access the WildFly Admin Console at: http://localhost:9990
   - Login with the credentials created above
   - The admin console allows you to:
     - Monitor server status
     - Deploy/undeploy applications
     - Configure data sources
     - Manage server resources
     - View logs

## Running the Application

1. Build and deploy:
   ```bash
   ./mvnw clean package wildfly:deploy
   ```

2. The application will be available at: http://localhost:8080/office-management-system

## Running Tests

Tests use an in-memory H2 database and don't require any additional configuration:

```bash
./mvnw test
