package com.officemanagement;

import com.officemanagement.resource.*;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;
import java.util.HashSet;
import java.util.Set;

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
        return classes;
    }
} 