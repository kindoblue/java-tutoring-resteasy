package com.officemanagement.resource;

import com.officemanagement.model.Floor;
import com.officemanagement.model.OfficeRoom;
import com.officemanagement.model.Seat;
import com.officemanagement.model.Employee;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import jakarta.ws.rs.core.Response;
import java.time.LocalDateTime;

import static io.restassured.RestAssured.given;


public class SeatResourceTest extends BaseResourceTest {

    @Test
    public void testCreateSeatWithInvalidData() {
        // Create necessary test data
        Floor floor = new Floor();
        floor.setName("First Floor");
        floor.setFloorNumber(1);
        session.save(floor);

        OfficeRoom room = new OfficeRoom();
        room.setName("Room 101");
        room.setRoomNumber("101");
        room.setFloor(floor);
        session.save(room);

        // Test with empty seat (missing room)
        Seat emptySeat = new Seat();
        emptySeat.setSeatNumber("A1"); // Set a valid seat number to avoid not-null constraint

        given()
            .contentType(ContentType.JSON)
            .body(emptySeat)
        .when()
            .post(getApiPath("/seats"))
        .then()
            .statusCode(Response.Status.BAD_REQUEST.getStatusCode());

        // Test with blank seat number
        Seat blankSeatNumber = new Seat();
        blankSeatNumber.setSeatNumber("");
        blankSeatNumber.setRoom(room);

        given()
            .contentType(ContentType.JSON)
            .body(blankSeatNumber)
        .when()
            .post(getApiPath("/seats"))
        .then()
            .statusCode(Response.Status.BAD_REQUEST.getStatusCode());

        // Test with non-existent room
        OfficeRoom nonExistentRoom = new OfficeRoom();
        nonExistentRoom.setId(99999L);

        Seat seatWithInvalidRoom = new Seat();
        seatWithInvalidRoom.setSeatNumber("A1");
        seatWithInvalidRoom.setRoom(nonExistentRoom);

        given()
            .contentType(ContentType.JSON)
            .body(seatWithInvalidRoom)
        .when()
            .post(getApiPath("/seats"))
        .then()
            .statusCode(Response.Status.BAD_REQUEST.getStatusCode());

        // Test with invalid content type
        given()
            .contentType(ContentType.TEXT)
            .body("Invalid data")
        .when()
            .post(getApiPath("/seats"))
        .then()
            .statusCode(Response.Status.UNSUPPORTED_MEDIA_TYPE.getStatusCode());
    }

    @Test
    public void testDuplicateSeatNumber() {
        // Create necessary test data
        Floor floor = new Floor();
        floor.setName("First Floor");
        floor.setFloorNumber(1);
        session.save(floor);

        OfficeRoom room = new OfficeRoom();
        room.setName("Room 101");
        room.setRoomNumber("101");
        room.setFloor(floor);
        session.save(room);
        
        commitAndStartNewTransaction();

        // Create first seat
        Seat seat1 = new Seat();
        seat1.setSeatNumber("101-1");
        seat1.setRoom(room);
        session.save(seat1);

        given()
            .contentType(ContentType.JSON)
            .body(seat1)
        .when()
            .post(getApiPath("/seats"))
        .then()
            .statusCode(Response.Status.CREATED.getStatusCode());

        // Try to create another seat with same number in same room
        Seat seat2 = new Seat();
        seat2.setSeatNumber("101-1");  // Same seat number
        seat2.setRoom(room);
        session.save(seat2);

        given()
            .contentType(ContentType.JSON)
            .body(seat2)
        .when()
            .post(getApiPath("/seats"))
        .then()
            .statusCode(Response.Status.CONFLICT.getStatusCode());
    }

    @Test
    public void testDeleteSeatWithAssignedEmployee() {
        // Create necessary test data
        Floor floor = new Floor();
        floor.setName("First Floor");
        floor.setFloorNumber(1);
        session.save(floor);

        OfficeRoom room = new OfficeRoom();
        room.setName("Room 101");
        room.setRoomNumber("101");
        room.setFloor(floor);
        session.save(room);

        Seat seat = new Seat();
        seat.setSeatNumber("101-1");
        seat.setRoom(room);
        session.save(seat);

        Employee employee = new Employee();
        employee.setFullName("John Doe");
        employee.setOccupation("Software Engineer");
        session.save(employee);

        // Assign employee to seat
        employee.addSeat(seat);
        session.flush();
        commitAndStartNewTransaction();

        // Attempt to delete seat with assigned employee (should fail)
        given()
        .when()
            .delete(getApiPath("/seats/" + seat.getId()))
        .then()
            .statusCode(Response.Status.BAD_REQUEST.getStatusCode());
    }

    @Test
    public void testUpdateNonExistentSeat() {
        // Create necessary test data
        Floor floor = new Floor();
        floor.setName("First Floor");
        floor.setFloorNumber(1);
        session.save(floor);

        OfficeRoom room = new OfficeRoom();
        room.setName("Room 101");
        room.setRoomNumber("101");
        room.setFloor(floor);
        session.save(room);

        // Create a valid seat for the update request
        Seat seat = new Seat();
        seat.setSeatNumber("101-1");
        seat.setRoom(room);
        session.save(seat);

        given()
            .contentType(ContentType.JSON)
            .body(seat)
        .when()
            .put(getApiPath("/seats/99999"))
        .then()
            .statusCode(Response.Status.NOT_FOUND.getStatusCode());
    }
} 