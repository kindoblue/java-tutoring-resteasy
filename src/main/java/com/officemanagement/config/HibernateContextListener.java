package com.officemanagement.config;

import com.officemanagement.util.HibernateUtil;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

@WebListener
public class HibernateContextListener implements ServletContextListener {
    
    @Override
    public void contextInitialized(ServletContextEvent sce) {
        // Initialize Hibernate when the web application starts
        HibernateUtil.getSessionFactory();
    }
    
    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        // Clean up Hibernate resources when the web application stops
        HibernateUtil.shutdown();
    }
} 