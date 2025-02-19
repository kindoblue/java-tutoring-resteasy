package com.officemanagement.resource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.officemanagement.util.HibernateUtil;
import com.officemanagement.config.UnsupportedMediaTypeExceptionMapper;
import io.restassured.RestAssured;
import io.restassured.config.ObjectMapperConfig;
import io.restassured.config.RestAssuredConfig;
import io.undertow.Undertow;
import org.jboss.resteasy.plugins.server.undertow.UndertowJaxrsServer;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;
import java.util.HashSet;
import java.util.Set;

@ExtendWith(MockitoExtension.class)
@Execution(ExecutionMode.SAME_THREAD)
public abstract class BaseResourceTest {
    private static final Logger logger = LoggerFactory.getLogger(BaseResourceTest.class);
    private static final Object LOCK = new Object();
    private static volatile boolean initialized = false;
    protected static ObjectMapper objectMapper;
    protected static SessionFactory sessionFactory;
    protected Session session;
    protected Transaction transaction;
    protected static UndertowJaxrsServer server;
    protected static final int PORT = 8081;

    @ApplicationPath("/api")
    public static class TestApplication extends Application {
        @Override
        public Set<Class<?>> getClasses() {
            Set<Class<?>> classes = new HashSet<>();
            classes.add(SeatResource.class);
            classes.add(EmployeeResource.class);
            classes.add(FloorResource.class);
            classes.add(RoomResource.class);
            classes.add(StatsResource.class);
            classes.add(com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider.class);
            classes.add(UnsupportedMediaTypeExceptionMapper.class);
            return classes;
        }
    }

    @BeforeAll
    public static void setupClass() {
        synchronized (LOCK) {
            if (!initialized) {
                try {
                    logger.info("Starting test server initialization...");

                    // Initialize Hibernate
                    HibernateUtil.setTestEnvironment(true);
                    sessionFactory = HibernateUtil.getSessionFactory();
                    logger.info("Hibernate initialized successfully");

                    // Start Undertow JAX-RS server
                    server = new UndertowJaxrsServer();
                    server.start(Undertow.builder().addHttpListener(PORT, "localhost"));
                    
                    // Deploy application
                    server.deploy(TestApplication.class);
                    
                    logger.info("Undertow server started on port: {}", PORT);

                    // Configure RestAssured
                    RestAssured.baseURI = "http://localhost";
                    RestAssured.port = PORT;
                    RestAssured.basePath = "/api";
                    RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();

                    RestAssured.config = RestAssuredConfig.config()
                            .objectMapperConfig(new ObjectMapperConfig().jackson2ObjectMapperFactory(
                                    (cls, charset) -> {
                                        ObjectMapper mapper = new ObjectMapper();
                                        mapper.registerModule(new JavaTimeModule());
                                        return mapper;
                                    }));

                    logger.info("RestAssured configured with basePath: {}, and Jackson ObjectMapper",
                            RestAssured.basePath);

                    initialized = true;
                    logger.info("Test server initialization completed successfully");
                } catch (Exception e) {
                    logger.error("Failed to initialize test server", e);
                    throw e;
                }
            }
        }
    }

    @BeforeEach
    public void setupTest() throws Exception {
        synchronized (LOCK) {
            session = sessionFactory.openSession();
            transaction = session.beginTransaction();
            cleanDatabase();
        }
    }

    private void cleanDatabase() {
        try {
            logger.info("Cleaning database...");

            // Disable foreign key checks
            session.createNativeQuery("SET REFERENTIAL_INTEGRITY FALSE").executeUpdate();

            // Clean tables
            session.createNativeQuery("DELETE FROM seats").executeUpdate();
            session.createNativeQuery("DELETE FROM employees").executeUpdate();
            session.createNativeQuery("DELETE FROM office_rooms").executeUpdate();
            session.createNativeQuery("DELETE FROM floors").executeUpdate();

            // Reset sequences
            session.createNativeQuery("ALTER SEQUENCE seat_seq RESTART WITH 1").executeUpdate();
            session.createNativeQuery("ALTER SEQUENCE employee_seq RESTART WITH 1").executeUpdate();
            session.createNativeQuery("ALTER SEQUENCE office_room_seq RESTART WITH 1").executeUpdate();
            session.createNativeQuery("ALTER SEQUENCE floor_seq RESTART WITH 1").executeUpdate();

            // Re-enable foreign key checks
            session.createNativeQuery("SET REFERENTIAL_INTEGRITY TRUE").executeUpdate();

            transaction.commit();
            transaction = session.beginTransaction();
            logger.info("Database cleaned successfully");
        } catch (Exception e) {
            logger.error("Failed to clean database", e);
            if (transaction != null && transaction.isActive()) {
                transaction.rollback();
            }
            throw e;
        }
    }

    @AfterEach
    public void cleanup() {
        try {
            if (session != null && session.isOpen()) {
                if (transaction != null && transaction.isActive()) {
                    transaction.rollback();
                }
                session.close();
            }
        } catch (Exception e) {
            logger.error("Error during cleanup", e);
        }
    }

    @AfterAll
    public static void tearDownClass() {
        synchronized (LOCK) {
            if (server != null) {
                server.stop();
                server = null;
            }
            initialized = false;
        }
    }

    protected void flushAndClear() {
        session.flush();
        session.clear();
    }

    protected void commitAndStartNewTransaction() {
        transaction.commit();
        transaction = session.beginTransaction();
    }

    protected String getApiPath(String path) {
        return path.startsWith("/") ? path : "/" + path;
    }
}