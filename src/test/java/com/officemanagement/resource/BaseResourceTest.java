package com.officemanagement.resource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import com.officemanagement.util.HibernateUtil;
import io.restassured.RestAssured;
import io.undertow.Undertow;
import io.undertow.servlet.api.DeploymentInfo;
import org.jboss.resteasy.spi.ResteasyDeployment;
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
import javax.servlet.ServletException;
import io.undertow.server.HttpHandler;
import io.undertow.servlet.Servlets;
import io.undertow.servlet.api.DeploymentManager;
import java.util.List;

import com.fasterxml.jackson.databind.SerializationFeature;

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
    protected static Undertow server;
    private static final ThreadLocal<Session> threadLocalSession = new ThreadLocal<>();
    protected static final int PORT = 8081;
    protected static final String CONTEXT_PATH = "/officemanagement";
    protected static DeploymentInfo deploymentInfo;

    @BeforeAll
    public static void setupClass() throws ServletException {
        synchronized (LOCK) {
            if (!initialized) {
                try {
                    // Initialize Hibernate
                    HibernateUtil.setTestEnvironment(true);
                    sessionFactory = HibernateUtil.getSessionFactory(); // Force initialization
                    
                    // Create deployment info
                    deploymentInfo = Servlets.deployment()
                        .setClassLoader(BaseResourceTest.class.getClassLoader())
                        .setContextPath(CONTEXT_PATH)
                        .setDeploymentName("office-management-test")
                        .addServletContextAttribute(ResteasyDeployment.class.getName(), createDeployment());
                        
                    DeploymentManager manager = Servlets.defaultContainer().addDeployment(deploymentInfo);
                    manager.deploy();
                    
                    HttpHandler httpHandler = manager.start();
                    
                    // Start Undertow server
                    server = Undertow.builder()
                        .addHttpListener(PORT, "localhost")
                        .setHandler(httpHandler)
                        .build();
                    server.start();
                    
                    // Configure RestAssured
                    RestAssured.baseURI = "http://localhost";
                    RestAssured.port = PORT;
                    RestAssured.basePath = "/officemanagement/api";
                    
                    initialized = true;
                    logger.info("Test server initialized successfully");
                } catch (Exception e) {
                    logger.error("Failed to initialize test server", e);
                    throw e;
                }
            }
        }
    }
    
    private static ResteasyDeployment createDeployment() {
        ResteasyDeployment deployment = new ResteasyDeployment();
        
        // Don't set application class, instead register resources directly
        deployment.setResources(List.of(
            new SeatResource(),
            new EmployeeResource(),
            new FloorResource(),
            new RoomResource(),
            new StatsResource()
        ));
        
        // Register providers
        deployment.setProviderClasses(List.of(
            JacksonJsonProvider.class.getName()
        ));
        
        // Configure Jackson
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        mapper.configure(SerializationFeature.WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS, false);
        deployment.setProviders(List.of(new JacksonJsonProvider(mapper)));
        
        return deployment;
    }

    @AfterAll
    public static void tearDownClass() {
        synchronized (LOCK) {
            try {
                logger.info("Starting test server teardown...");
                if (server != null) {
                    server.stop();
                    server = null;
                    logger.info("Stopped Undertow server");
                }
                initialized = false;
                logger.info("Test server teardown completed successfully");
            } catch (Exception e) {
                logger.error("Failed to stop test server", e);
                throw new RuntimeException("Failed to stop test server", e);
            }
        }
    }

    @BeforeEach
    public void setupTest() throws Exception {
        synchronized (LOCK) {
            // Get fresh session for each test
            session = sessionFactory.openSession();
            transaction = session.beginTransaction();
            cleanDatabase();
        }
    }

    private void cleanDatabase() {
        try {
            logger.info("Cleaning database...");
            
            // Use database-agnostic approach to clean tables
            String[] tables = {"seats", "employees", "office_rooms", "floors"};
            
            // First disable foreign key constraints if possible
            try {
                // Try H2 syntax
                session.createNativeQuery("SET REFERENTIAL_INTEGRITY FALSE").executeUpdate();
            } catch (Exception e) {
                try {
                    // Try PostgreSQL syntax
                    tables = new String[]{"seats", "employees", "office_rooms", "floors"};
                    for (String table : tables) {
                        session.createNativeQuery("ALTER TABLE " + table + " DISABLE TRIGGER ALL").executeUpdate();
                    }
                } catch (Exception ex) {
                    logger.warn("Could not disable constraints, will try to delete in order: {}", ex.getMessage());
                }
            }
            
            // Delete from tables in reverse order of dependencies
            for (String table : tables) {
                try {
                    session.createNativeQuery("DELETE FROM " + table).executeUpdate();
                } catch (Exception e) {
                    logger.error("Error cleaning table {}: {}", table, e.getMessage());
                }
            }
            
            // Try to reset sequences in a database-agnostic way
            String[][] sequences = {
                {"seat_seq", "seats"}, 
                {"employee_seq", "employees"},
                {"office_room_seq", "office_rooms"}, 
                {"floor_seq", "floors"}
            };
            
            for (String[] sequence : sequences) {
                try {
                    // Try H2 syntax
                    session.createNativeQuery("ALTER SEQUENCE " + sequence[0] + " RESTART WITH 1").executeUpdate();
                } catch (Exception e) {
                    try {
                        // Try PostgreSQL syntax
                        session.createNativeQuery("SELECT setval('" + sequence[0] + "', 1, false)").executeUpdate();
                    } catch (Exception ex) {
                        logger.warn("Could not reset sequence {}: {}", sequence[0], ex.getMessage());
                    }
                }
            }
            
            // Re-enable foreign key constraints
            try {
                // Try H2 syntax
                session.createNativeQuery("SET REFERENTIAL_INTEGRITY TRUE").executeUpdate();
            } catch (Exception e) {
                try {
                    // Try PostgreSQL syntax
                    for (String table : tables) {
                        session.createNativeQuery("ALTER TABLE " + table + " ENABLE TRIGGER ALL").executeUpdate();
                    }
                } catch (Exception ex) {
                    logger.warn("Could not re-enable constraints: {}", ex.getMessage());
                }
            }
            
            // Commit the changes
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
        } finally {
            threadLocalSession.remove();
            transaction = null;
            session = null;
        }
    }

    protected void flushAndClear() {
        session.flush();
        session.clear();
    }

    protected void commitAndStartNewTransaction() {
        if (session != null && session.isOpen()) {
            if (session.getTransaction().isActive()) {
                session.getTransaction().commit();
            }
            session.beginTransaction();
        }
    }

    protected String getApiPath(String path) {
        return path.startsWith("/") ? path : "/" + path;
    }
}
