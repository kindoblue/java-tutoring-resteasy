package com.officemanagement.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Filter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.io.IOException;

public class CORSFilter implements Filter {
    private static final Logger logger = LoggerFactory.getLogger(CORSFilter.class);

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        logger.info("Initializing CORS filter");
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        
        // Log request details
        logger.debug("Incoming request: {} {} from {}",
            httpRequest.getMethod(),
            httpRequest.getRequestURI(),
            httpRequest.getRemoteAddr()
        );

        // Add CORS headers
        httpResponse.setHeader("Access-Control-Allow-Origin", "*");
        httpResponse.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        httpResponse.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");
        httpResponse.setHeader("Access-Control-Max-Age", "3600");

        // Handle OPTIONS method
        if ("OPTIONS".equalsIgnoreCase(httpRequest.getMethod())) {
            logger.debug("Handling OPTIONS request for: {}", httpRequest.getRequestURI());
            httpResponse.setStatus(HttpServletResponse.SC_OK);
            return;
        }

        // Continue with the filter chain
        try {
            chain.doFilter(request, response);
        } catch (Exception e) {
            // Log any errors, including 405 responses
            if (httpResponse.getStatus() == HttpServletResponse.SC_METHOD_NOT_ALLOWED) {
                logger.warn("Method {} not allowed for path: {}. Allowed methods: {}",
                    httpRequest.getMethod(),
                    httpRequest.getRequestURI(),
                    httpResponse.getHeader("Allow")
                );
            }
            throw e;
        }
    }

    @Override
    public void destroy() {
        logger.info("Destroying CORS filter");
    }
} 