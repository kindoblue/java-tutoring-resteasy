package com.officemanagement.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.jakarta.rs.json.JacksonJsonProvider;
import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;
import java.util.HashSet;
import java.util.Set;
import com.fasterxml.jackson.databind.SerializationFeature;

@ApplicationPath("/api")
public class RestEasyConfig extends Application {
    
    @Override
    public Set<Object> getSingletons() {
        Set<Object> singletons = new HashSet<>();
        
        // Configure JSON serialization with JavaTimeModule
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        // Configure to write dates as ISO-8601 strings instead of timestamps
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        mapper.configure(SerializationFeature.WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS, false);
        
        JacksonJsonProvider provider = new JacksonJsonProvider(mapper);
        singletons.add(provider);
        
        return singletons;
    }

    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> classes = new HashSet<>();
        // Register resources package
        classes.add(com.officemanagement.resource.EmployeeResource.class);
        classes.add(com.officemanagement.resource.FloorResource.class);
        classes.add(com.officemanagement.resource.RoomResource.class);
        classes.add(com.officemanagement.resource.SeatResource.class);
        classes.add(com.officemanagement.resource.StatsResource.class);
        return classes;
    }
} 