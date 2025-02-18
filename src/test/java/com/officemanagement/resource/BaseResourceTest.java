package com.officemanagement.resource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.jakarta.rs.json.JacksonJsonProvider;
import com.officemanagement.config.RestEasyConfig;
import com.officemanagement.util.HibernateUtil;
import io.restassured.RestAssured;
import io.restassured.config.ObjectMapperConfig;
import io.restassured.config.RestAssuredConfig;
import io.undertow.Undertow;
import org.jboss.resteasy.core.ResteasyDeploymentImpl;
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

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;
import com.fasterxml.jackson.databind.SerializationFeature;

@ExtendWith(MockitoExtension.class)
@Execution(ExecutionMode.SAME_THREAD)
@ApplicationPath("/api")
public abstract class BaseResourceTest {
    private static final Logger logger = LoggerFactory.getLogger(BaseResourceTest.class);
    private static boolean initialized = false;
    protected static ObjectMapper objectMapper;
    protected static SessionFactory sessionFactory;
    protected Session session;
    protected Transaction transaction;
    protected static UndertowJaxrsServer server;

    @BeforeAll
    public static void setupClass() {
        if (!initialized) {
            try {
                logger.info("Starting test server setup...");
                
                // Configure ObjectMapper
                objectMapper = new ObjectMapper();
                objectMapper.registerModule(new JavaTimeModule());
                objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
                objectMapper.configure(SerializationFeature.WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS, false);
                logger.info("Configured ObjectMapper");
                
                // Initialize Hibernate SessionFactory
                sessionFactory = HibernateUtil.getSessionFactory();
                logger.info("Initialized Hibernate SessionFactory");

                // Start the server
                server = new UndertowJaxrsServer();
                server.start(Undertow.builder().addHttpListener(8081, "localhost"));

                // Create deployment
                ResteasyDeploymentImpl deployment = new ResteasyDeploymentImpl();
                deployment.setApplication(new RestEasyConfig());
                
                // Register providers
                JacksonJsonProvider jsonProvider = new JacksonJsonProvider(objectMapper);
                deployment.getProviders().add(jsonProvider);

                // Deploy the application
                server.deploy(deployment);
                logger.info("Deployed RESTEasy application");
                
                // Configure RestAssured
                RestAssured.baseURI = "http://localhost:8081";
                RestAssured.config = RestAssuredConfig.config().objectMapperConfig(
                    ObjectMapperConfig.objectMapperConfig().jackson2ObjectMapperFactory(
                        (type, s) -> {
                            ObjectMapper clientMapper = new ObjectMapper();
                            clientMapper.registerModule(new JavaTimeModule());
                            clientMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
                            clientMapper.configure(SerializationFeature.WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS, false);
                            return clientMapper;
                        }
                    )
                );
                logger.info("Configured RestAssured");
                
                initialized = true;
                logger.info("Test server setup completed successfully");
            } catch (Exception e) {
                logger.error("Failed to start test server", e);
                if (server != null) {
                    server.stop();
                }
                throw new RuntimeException("Failed to start test server", e);
            }
        }
    }

    @AfterAll
    public static void tearDownClass() {
        try {
            logger.info("Starting test server teardown...");
            if (server != null) {
                server.stop();
                server = null;
                logger.info("Stopped Undertow server");
            }
            if (sessionFactory != null) {
                sessionFactory.close();
                sessionFactory = null;
                logger.info("Closed Hibernate SessionFactory");
            }
            logger.info("Test server teardown completed successfully");
        } catch (Exception e) {
            logger.error("Failed to stop test server", e);
            throw new RuntimeException("Failed to stop test server", e);
        }
    }

    @BeforeEach
    public void setup() throws Exception {
        // Ensure we have a valid server
        if (server == null || !initialized) {
            logger.warn("Server not initialized, running setup again...");
            setupClass();
        }
        
        RestAssured.basePath = "/api";
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
        
        // Start a new transaction
        session = sessionFactory.openSession();
        transaction = session.beginTransaction();

        // Clean the database before each test
        cleanDatabase();
    }

    private void cleanDatabase() {
        try {
            logger.info("Cleaning database...");
            // Disable foreign key checks temporarily
            session.createNativeQuery("SET REFERENTIAL_INTEGRITY FALSE").executeUpdate();
            
            // Truncate all tables
            session.createNativeQuery("TRUNCATE TABLE seats").executeUpdate();
            session.createNativeQuery("TRUNCATE TABLE employees").executeUpdate();
            session.createNativeQuery("TRUNCATE TABLE office_rooms").executeUpdate();
            session.createNativeQuery("TRUNCATE TABLE floors").executeUpdate();
            
            // Reset sequences
            session.createNativeQuery("ALTER SEQUENCE seat_seq RESTART WITH 1").executeUpdate();
            session.createNativeQuery("ALTER SEQUENCE employee_seq RESTART WITH 1").executeUpdate();
            session.createNativeQuery("ALTER SEQUENCE office_room_seq RESTART WITH 1").executeUpdate();
            session.createNativeQuery("ALTER SEQUENCE floor_seq RESTART WITH 1").executeUpdate();
            
            // Re-enable foreign key checks
            session.createNativeQuery("SET REFERENTIAL_INTEGRITY TRUE").executeUpdate();
            
            // Commit the changes
            transaction.commit();
            transaction = session.beginTransaction();
            logger.info("Database cleaned successfully");
        } catch (Exception e) {
            logger.error("Failed to clean database", e);
            throw e;
        }
    }

    @AfterEach
    public void cleanup() throws Exception {
        if (transaction != null && transaction.isActive()) {
            try {
                transaction.commit();
            } catch (Exception e) {
                logger.error("Failed to commit transaction", e);
                transaction.rollback();
                throw e;
            }
        }
        if (session != null && session.isOpen()) {
            session.close();
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