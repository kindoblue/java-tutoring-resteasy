package com.officemanagement.resource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import com.officemanagement.config.RestEasyConfig;
import com.officemanagement.filter.CORSFilter;
import com.officemanagement.util.HibernateUtil;
import io.restassured.RestAssured;
import io.restassured.config.ObjectMapperConfig;
import io.restassured.config.RestAssuredConfig;
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

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import java.lang.reflect.Method;

import javax.servlet.ServletException;
import io.undertow.server.HttpHandler;
import io.undertow.servlet.Servlets;
import io.undertow.servlet.api.DeploymentManager;
import io.undertow.servlet.api.SecurityConstraint;
import io.undertow.servlet.api.WebResourceCollection;

import java.util.List;
import org.jboss.resteasy.plugins.server.servlet.HttpServletDispatcher;
import javax.servlet.DispatcherType;
import java.util.stream.Collectors;
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
    protected static Undertow server;
    protected static final int PORT = 8081;
    protected static final String CONTEXT_PATH = "/officemanagement";
    protected static DeploymentInfo deploymentInfo;

    @BeforeAll
    public static void setupClass() throws ServletException {
        synchronized (LOCK) {
            if (!initialized) {
                try {
                    logger.info("Starting test server initialization...");

                    // Initialize Hibernate
                    HibernateUtil.setTestEnvironment(true);
                    sessionFactory = HibernateUtil.getSessionFactory();
                    logger.info("Hibernate initialized successfully");

                    // Create deployment info with explicit servlet configuration
                    deploymentInfo = Servlets.deployment()
                            .setClassLoader(BaseResourceTest.class.getClassLoader())
                            .setContextPath(CONTEXT_PATH)
                            .setDeploymentName("officemanagement.war")
                            .addServletContextAttribute(ResteasyDeployment.class.getName(), createDeployment())
                            .addServlets(
                                    Servlets.servlet("Resteasy", HttpServletDispatcher.class)
                                            .setLoadOnStartup(1)
                                            .addMapping("/api/*")
                                            .addInitParam("resteasy.servlet.mapping.prefix", "/api"))
                            .addSecurityConstraint(
                                    new SecurityConstraint()
                                            .addWebResourceCollection(new WebResourceCollection()
                                                    .addUrlPattern("/api/*")
                                                    .addHttpMethod("GET")
                                                    .addHttpMethod("POST")
                                                    .addHttpMethod("PUT")
                                                    .addHttpMethod("DELETE")
                                                    .addHttpMethod("OPTIONS")))
                            .addFilter(
                                    Servlets.filter("CORSFilter", CORSFilter.class))
                            .addFilterUrlMapping("CORSFilter", "/api/*", DispatcherType.REQUEST);

                    logger.info(
                            "Created deployment info - Context path: {}, Deployment name: {}, Servlet mappings: {}, Filter mappings: {}",
                            deploymentInfo.getContextPath(),
                            deploymentInfo.getDeploymentName(),
                            deploymentInfo.getServlets().get("Resteasy").getMappings(),
                            deploymentInfo.getFilterMappings().toString());
                    DeploymentManager manager = Servlets.defaultContainer().addDeployment(deploymentInfo);
                    manager.deploy();
                    logger.info("Deployment completed successfully");

                    HttpHandler httpHandler = manager.start();

                    // Start Undertow server
                    server = Undertow.builder()
                            .addHttpListener(PORT, "localhost")
                            .setHandler(httpHandler)
                            .build();
                    server.start();
                    logger.info("Undertow server started on port: {}", PORT);

                    // Configure RestAssured
                    RestAssured.baseURI = "http://localhost";
                    RestAssured.port = PORT;
                    RestAssured.basePath = "/officemanagement/api";
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

    private static ResteasyDeployment createDeployment() {
        ResteasyDeployment deployment = new ResteasyDeployment();
        
        // Set the Application class that has @ApplicationPath
        deployment.setApplicationClass(RestEasyConfig.class.getName());

        // Register resource instances
        List<Object> resources = List.of(
                new SeatResource(),
                new EmployeeResource(),
                new FloorResource(),
                new RoomResource(),
                new StatsResource());

        // Log registered resources
        logger.info("Registering REST resources:");
        resources.forEach(resource -> logger.info("  - {} with path: {}",
                resource.getClass().getSimpleName(),
                resource.getClass().getAnnotation(Path.class).value()));

        for (Object resource : resources) {
            Class<?> resourceClass = resource.getClass();
            Path pathAnnotation = resourceClass.getAnnotation(Path.class);
            String pathValue = (pathAnnotation != null) ? pathAnnotation.value() : "NO PATH ANNOTATION";
            System.out.println("  - " + resourceClass.getName() + " with path: " + pathValue);

            // **NEW: Check for @POST method in SeatResource**
            if (resourceClass.equals(SeatResource.class)) {
                boolean foundPost = false;
                for (Method method : resourceClass.getMethods()) {
                    if (method.isAnnotationPresent(POST.class)) {
                        System.out.println("    - Found @POST method: " + method.getName());
                        foundPost = true;
                    }
                }
                if (!foundPost) {
                    System.err.println("    - ERROR: SeatResource does NOT have a @POST method!"); // CRITICAL ERROR
                }
            }
        }

        deployment.setResources(resources);

        // Configure and register JSON provider
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        JacksonJsonProvider provider = new JacksonJsonProvider(mapper);
        deployment.setProviders(List.of(provider));
        logger.info("Registered JacksonJsonProvider for JSON handling");

        return deployment;
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