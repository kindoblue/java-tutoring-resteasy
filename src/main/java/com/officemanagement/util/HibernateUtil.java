package com.officemanagement.util;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HibernateUtil {
    private static final Logger logger = LoggerFactory.getLogger(HibernateUtil.class);
    private static volatile SessionFactory sessionFactory;
    private static final Object LOCK = new Object();
    private static final String TEST_CONFIG = "/hibernate.cfg.xml";
    private static final String PROD_CONFIG = "/hibernate.cfg.xml";
    private static volatile boolean isTestEnvironment = false;

    public static void setTestEnvironment(boolean isTest) {
        synchronized (LOCK) {
            if (sessionFactory != null) {
                shutdown();
            }
            isTestEnvironment = isTest;
            logger.info("Environment set to: {}", isTest ? "TEST" : "PRODUCTION");
        }
    }

    private static Configuration createConfiguration() {
        Configuration configuration = new Configuration();
        String configFile = isTestEnvironment ? TEST_CONFIG : PROD_CONFIG;
        
        try {
            // First try to load from classpath root
            configuration.configure(configFile);
            String env = isTestEnvironment ? "test" : "production";
            logger.info("Loaded Hibernate configuration for {} environment from {}", env, configFile);
            return configuration;
        } catch (Exception e) {
            // If that fails, try to load from the current class's package
            String fallbackPath = isTestEnvironment ? 
                "/src/test/resources/hibernate.cfg.xml" : 
                "/src/main/resources/hibernate.cfg.xml";
            logger.warn("Failed to load {} from classpath root, trying {}", configFile, fallbackPath);
            configuration.configure(fallbackPath);
            return configuration;
        }
    }

    public static SessionFactory getSessionFactory() {
        if (sessionFactory == null) {
            synchronized (LOCK) {
                if (sessionFactory == null) {
                    try {
                        Configuration configuration = createConfiguration();
                        sessionFactory = configuration.buildSessionFactory();
                        logger.info("Hibernate SessionFactory initialized successfully for {} environment",
                                  isTestEnvironment ? "test" : "production");
                    } catch (Throwable ex) {
                        logger.error("Initial SessionFactory creation failed", ex);
                        throw new ExceptionInInitializerError(ex);
                    }
                }
            }
        }
        return sessionFactory;
    }

    public static void shutdown() {
        synchronized (LOCK) {
            try {
                if (sessionFactory != null && !sessionFactory.isClosed()) {
                    logger.info("Closing Hibernate SessionFactory");
                    sessionFactory.close();
                    sessionFactory = null;
                }
            } catch (Exception e) {
                logger.error("Error closing SessionFactory", e);
                throw new RuntimeException("Failed to close SessionFactory", e);
            }
        }
    }
}
