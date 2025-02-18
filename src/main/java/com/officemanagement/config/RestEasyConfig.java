package com.officemanagement.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;
import java.util.HashSet;
import java.util.Set;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.officemanagement.resource.*;

@ApplicationPath("/api")
public class RestEasyConfig extends Application {
    
    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> classes = new HashSet<>();
        
        // Register resource classes
        classes.add(EmployeeResource.class);
        classes.add(SeatResource.class);
        classes.add(FloorResource.class);
        classes.add(RoomResource.class);
        classes.add(StatsResource.class);
        
        // Register providers
        classes.add(JacksonJsonProvider.class);
        
        return classes;
    }

    @Override
    public Set<Object> getSingletons() {
        Set<Object> singletons = new HashSet<>();
        
        // Configure JSON serialization
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        mapper.configure(SerializationFeature.WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS, false);
        
        // Add configured mapper
        singletons.add(new JacksonJsonProvider(mapper));
        
        return singletons;
    }
} 