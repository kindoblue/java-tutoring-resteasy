package com.officemanagement.util;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class HibernateUtil {
    private static final Logger logger = LoggerFactory.getLogger(HibernateUtil.class);
    private static volatile SessionFactory sessionFactory;
    private static final Object LOCK = new Object();

    private static Configuration createConfiguration() {
        Configuration configuration = new Configuration().configure();
        logger.debug("Created Hibernate configuration from hibernate.cfg.xml");
        return configuration;
    }

    public static SessionFactory getSessionFactory() {
        if (sessionFactory == null) {
            synchronized (LOCK) {
                if (sessionFactory == null) {
                    try {
                        // Create configuration and build SessionFactory
                        Configuration configuration = createConfiguration();
                        sessionFactory = configuration.buildSessionFactory();
                        logger.info("Hibernate SessionFactory initialized successfully");
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
            }
        }
    }
}
