package com.officemanagement.resource;

import com.officemanagement.model.Floor;
import com.officemanagement.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.time.LocalDateTime;
import java.util.List;

@Path("/floors")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class FloorResource {
    private final SessionFactory sessionFactory;

    public FloorResource() {
        this.sessionFactory = HibernateUtil.getSessionFactory();
    }

    @GET
    public Response getAllFloors() {
        try (Session session = sessionFactory.openSession()) {
            List<Floor> floors = session.createQuery(
                "select new Floor(f.id, f.name, f.floorNumber) from Floor f", 
                Floor.class).list();
            return Response.ok(floors).build();
        }
    }

    @GET
    @Path("/{id}")
    public Response getFloor(@PathParam("id") Long id) {
        try (Session session = sessionFactory.openSession()) {
            // Using criteria to fetch the floor and its associations
            Floor floor = session.createQuery(
                "select distinct f from Floor f " +
                "left join fetch f.rooms r " +
                "left join fetch r.seats " +
                "where f.id = :id", Floor.class)
                .setParameter("id", id)
                .uniqueResult();
                
            if (floor == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            return Response.ok(floor).build();
        }
    }

    @POST
    public Response createFloor(Floor floor) {
        // Validate input
        if (floor == null || floor.getName() == null || floor.getName().trim().isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity("Floor name is required")
                .build();
        }

        if (floor.getFloorNumber() == null || floor.getFloorNumber() < 0) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity("Valid floor number is required (must be 0 or greater)")
                .build();
        }

        try (Session session = sessionFactory.openSession()) {
            // Check for duplicate floor number
            Long count = session.createQuery(
                "SELECT COUNT(f) FROM Floor f WHERE f.floorNumber = :floorNumber", Long.class)
                .setParameter("floorNumber", floor.getFloorNumber())
                .uniqueResult();

            if (count > 0) {
                return Response.status(Response.Status.CONFLICT)
                    .entity("A floor with number " + floor.getFloorNumber() + " already exists")
                    .build();
            }

            session.beginTransaction();
            session.save(floor);
            session.getTransaction().commit();
            return Response.status(Response.Status.CREATED).entity(floor).build();
        }
    }

    @PUT
    @Path("/{id}")
    public Response updateFloor(@PathParam("id") Long id, Floor floor) {
        // Validate input
        if (floor == null || floor.getName() == null || floor.getName().trim().isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity("Floor name is required")
                .build();
        }

        if (floor.getFloorNumber() == null || floor.getFloorNumber() < 0) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity("Valid floor number is required (must be 0 or greater)")
                .build();
        }

        try (Session session = sessionFactory.openSession()) {
            Floor existingFloor = session.get(Floor.class, id);
            if (existingFloor == null) {
                return Response.status(Response.Status.NOT_FOUND)
                    .entity("Floor not found")
                    .build();
            }

            // Check for duplicate floor number (excluding current floor)
            Long count = session.createQuery(
                "SELECT COUNT(f) FROM Floor f WHERE f.floorNumber = :floorNumber AND f.id != :id", Long.class)
                .setParameter("floorNumber", floor.getFloorNumber())
                .setParameter("id", id)
                .uniqueResult();

            if (count > 0) {
                return Response.status(Response.Status.CONFLICT)
                    .entity("A floor with number " + floor.getFloorNumber() + " already exists")
                    .build();
            }

            session.beginTransaction();
            existingFloor.setName(floor.getName());
            existingFloor.setFloorNumber(floor.getFloorNumber());
            session.update(existingFloor);
            session.getTransaction().commit();
            return Response.ok(existingFloor).build();
        }
    }

    @DELETE
    @Path("/{id}")
    public Response deleteFloor(@PathParam("id") Long id) {
        try (Session session = sessionFactory.openSession()) {
            session.beginTransaction();
            Floor floor = session.get(Floor.class, id);
            
            if (floor == null) {
                return Response.status(Response.Status.NOT_FOUND)
                    .entity("Floor not found")
                    .build();
            }

            // Check if floor has rooms
            Long roomCount = session.createQuery(
                "SELECT COUNT(r) FROM OfficeRoom r WHERE r.floor.id = :floorId", Long.class)
                .setParameter("floorId", id)
                .uniqueResult();

            if (roomCount > 0) {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Cannot delete floor that has rooms")
                    .build();
            }

            session.delete(floor);
            session.getTransaction().commit();
            return Response.noContent().build();
        }
    }
} 