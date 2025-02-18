package com.officemanagement.resource;

import com.officemanagement.model.Floor;
import com.officemanagement.model.OfficeRoom;
import com.officemanagement.model.Seat;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import javax.ws.rs.core.Response;

import static io.restassured.RestAssured.given;


public class RoomResourceTest extends BaseResourceTest {

    @Test
    public void testCreateRoomWithInvalidData() {
        // Test with empty room
        OfficeRoom emptyRoom = new OfficeRoom();
        session.save(emptyRoom);

        given()
            .contentType(ContentType.JSON)
            .body(emptyRoom)
        .when()
            .post(getApiPath("/rooms"))
        .then()
            .statusCode(Response.Status.BAD_REQUEST.getStatusCode());

        // Test with null room
        OfficeRoom nullRoom = new OfficeRoom();
        nullRoom.setName(null);
        nullRoom.setRoomNumber(null);
        session.save(nullRoom);

        given()
            .contentType(ContentType.JSON)
            .body(nullRoom)
        .when()
            .post(getApiPath("/rooms"))
        .then()
            .statusCode(Response.Status.BAD_REQUEST.getStatusCode());

        // Test with non-existent floor
        Floor nonExistentFloor = new Floor();
        nonExistentFloor.setId(99999L);

        OfficeRoom roomWithInvalidFloor = new OfficeRoom();
        roomWithInvalidFloor.setName("Test Room");
        roomWithInvalidFloor.setRoomNumber("101");
        session.save(roomWithInvalidFloor);

        given()
            .contentType(ContentType.JSON)
            .body(roomWithInvalidFloor)
        .when()
            .post(getApiPath("/rooms"))
        .then()
            .statusCode(Response.Status.BAD_REQUEST.getStatusCode());

        // Test with invalid content type
        given()
            .contentType(ContentType.TEXT)
            .body("Invalid data")
        .when()
            .post(getApiPath("/rooms"))
        .then()
            .statusCode(Response.Status.UNSUPPORTED_MEDIA_TYPE.getStatusCode());
    }

    @Test
    public void testDuplicateRoomNumber() {
        // Create a floor first
        Floor floor = new Floor();
        floor.setName("First Floor");
        floor.setFloorNumber(1);
        session.save(floor);
        commitAndStartNewTransaction();

        // Create first room
        OfficeRoom room1 = new OfficeRoom();
        room1.setName("Room 101");
        room1.setRoomNumber("101");
        room1.setFloor(floor);

        given()
            .contentType(ContentType.JSON)
            .body(room1)
        .when()
            .post(getApiPath("/rooms"))
        .then()
            .statusCode(Response.Status.CREATED.getStatusCode());

        // Try to create another room with same number on same floor
        OfficeRoom room2 = new OfficeRoom();
        room2.setName("Another Room 101");
        room2.setRoomNumber("101");  // Same room number
        room2.setFloor(floor);

        given()
            .contentType(ContentType.JSON)
            .body(room2)
        .when()
            .post(getApiPath("/rooms"))
        .then()
            .statusCode(Response.Status.CONFLICT.getStatusCode());
    }

    @Test
    public void testDeleteRoomWithSeats() {
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
        
        commitAndStartNewTransaction();

        // Attempt to delete room with seats (should fail)
        given()
        .when()
            .delete(getApiPath("/rooms/" + room.getId()))
        .then()
            .statusCode(Response.Status.BAD_REQUEST.getStatusCode());
    }
} 