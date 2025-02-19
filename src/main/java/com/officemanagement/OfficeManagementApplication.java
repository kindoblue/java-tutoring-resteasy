package com.officemanagement;

import com.officemanagement.resource.*;
import com.officemanagement.config.UnsupportedMediaTypeExceptionMapper;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;
import java.util.HashSet;
import java.util.Set;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;

@ApplicationPath("/api")
public class OfficeManagementApplication extends Application {
    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> classes = new HashSet<>();
        classes.add(SeatResource.class);
        classes.add(EmployeeResource.class);
        classes.add(FloorResource.class);
        classes.add(RoomResource.class);
        classes.add(StatsResource.class);
        classes.add(JacksonJsonProvider.class);
        classes.add(UnsupportedMediaTypeExceptionMapper.class);
        return classes;
    }
} 