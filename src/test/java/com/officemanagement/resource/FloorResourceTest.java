package com.officemanagement.resource;

import com.officemanagement.model.Floor;
import com.officemanagement.model.OfficeRoom;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import jakarta.ws.rs.core.Response;
import java.time.LocalDateTime;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

public class FloorResourceTest extends BaseResourceTest {

    @Test
    public void testCreateFloor() {
        Floor floor = new Floor();
        floor.setName("First Floor");
        floor.setFloorNumber(1);
        session.save(floor);

        given()
            .contentType(ContentType.JSON)
            .body(floor)
        .when()
            .post(getApiPath("/floors"))
        .then()
            .statusCode(Response.Status.CREATED.getStatusCode())
            .body("id", notNullValue())
            .body("name", equalTo("First Floor"))
            .body("floorNumber", equalTo(1));
    }

    @Test
    public void testGetAllFloors() {
        // Create test floors
        createTestFloor("Ground Floor", 0);
        createTestFloor("First Floor", 1);
        createTestFloor("Second Floor", 2);

        given()
        .when()
            .get(getApiPath("/floors"))
        .then()
            .statusCode(Response.Status.OK.getStatusCode())
            .body("$", hasSize(greaterThanOrEqualTo(3)))
            .body("find { it.name == 'Ground Floor' }.floorNumber", equalTo(0))
            .body("find { it.name == 'First Floor' }.floorNumber", equalTo(1))
            .body("find { it.name == 'Second Floor' }.floorNumber", equalTo(2));
    }

    @Test
    public void testGetFloor() {
        // First create a floor
        Floor floor = new Floor();
        floor.setName("Test Floor");
        floor.setFloorNumber(3);
        session.save(floor);

        Integer floorIdInt = given()
            .contentType(ContentType.JSON)
            .body(floor)
        .when()
            .post(getApiPath("/floors"))
        .then()
            .statusCode(Response.Status.CREATED.getStatusCode())
            .extract()
            .path("id");
            
        Long floorId = floorIdInt.longValue();

        // Then get the floor
        given()
        .when()
            .get(getApiPath("/floors/" + floorId))
        .then()
            .statusCode(Response.Status.OK.getStatusCode())
            .body("name", equalTo("Test Floor"))
            .body("floorNumber", equalTo(3))
            .body("rooms", notNullValue());
    }

    @Test
    public void testUpdateFloor() {
        // First create a floor
        Floor floor = new Floor();
        floor.setName("Original Name");
        floor.setFloorNumber(4);
        session.save(floor);

        Integer floorIdInt = given()
            .contentType(ContentType.JSON)
            .body(floor)
        .when()
            .post(getApiPath("/floors"))
        .then()
            .statusCode(Response.Status.CREATED.getStatusCode())
            .extract()
            .path("id");
            
        Long floorId = floorIdInt.longValue();

        // Update the floor
        floor.setName("Updated Name");
        
        given()
            .contentType(ContentType.JSON)
            .body(floor)
        .when()
            .put(getApiPath("/floors/" + floorId))
        .then()
            .statusCode(Response.Status.OK.getStatusCode())
            .body("name", equalTo("Updated Name"))
            .body("floorNumber", equalTo(4));
    }

    @Test
    public void testDeleteFloor() {
        // First create a floor
        Floor floor = new Floor();
        floor.setName("Floor to Delete");
        floor.setFloorNumber(5);
        session.save(floor);

        Integer floorIdInt = given()
            .contentType(ContentType.JSON)
            .body(floor)
        .when()
            .post(getApiPath("/floors"))
        .then()
            .statusCode(Response.Status.CREATED.getStatusCode())
            .extract()
            .path("id");
            
        Long floorId = floorIdInt.longValue();

        // Delete the floor
        given()
        .when()
            .delete(getApiPath("/floors/" + floorId))
        .then()
            .statusCode(Response.Status.NO_CONTENT.getStatusCode());

        // Verify floor is deleted
        given()
        .when()
            .get(getApiPath("/floors/" + floorId))
        .then()
            .statusCode(Response.Status.NOT_FOUND.getStatusCode());
    }

    @Test
    public void testCreateFloorWithInvalidData() {
        // Test with empty floor
        Floor emptyFloor = new Floor();
        session.save(emptyFloor);

        given()
            .contentType(ContentType.JSON)
            .body(emptyFloor)
        .when()
            .post(getApiPath("/floors"))
        .then()
            .statusCode(Response.Status.BAD_REQUEST.getStatusCode());

        // Test with null values
        Floor nullFloor = new Floor();
        nullFloor.setName(null);
        nullFloor.setFloorNumber(null);
        session.save(nullFloor);

        given()
            .contentType(ContentType.JSON)
            .body(nullFloor)
        .when()
            .post(getApiPath("/floors"))
        .then()
            .statusCode(Response.Status.BAD_REQUEST.getStatusCode());

        // Test with invalid floor number
        Floor invalidFloor = new Floor();
        invalidFloor.setName("Test Floor");
        invalidFloor.setFloorNumber(-1);  // Negative floor number
        session.save(invalidFloor);

        given()
            .contentType(ContentType.JSON)
            .body(invalidFloor)
        .when()
            .post(getApiPath("/floors"))
        .then()
            .statusCode(Response.Status.BAD_REQUEST.getStatusCode());

        // Test with invalid content type
        given()
            .contentType(ContentType.TEXT)
            .body("Invalid data")
        .when()
            .post(getApiPath("/floors"))
        .then()
            .statusCode(Response.Status.UNSUPPORTED_MEDIA_TYPE.getStatusCode());
    }

    @Test
    public void testUpdateNonExistentFloor() {
        Floor floor = new Floor();
        floor.setName("Test Floor");
        floor.setFloorNumber(1);
        session.save(floor);

        given()
            .contentType(ContentType.JSON)
            .body(floor)
        .when()
            .put(getApiPath("/floors/99999"))
        .then()
            .statusCode(Response.Status.NOT_FOUND.getStatusCode());
    }

    @Test
    public void testDeleteFloorWithRooms() {
        // Create a floor
        Floor floor = new Floor();
        floor.setName("Test Floor");
        floor.setFloorNumber(1);
        session.save(floor);

        // Create a room in the floor
        OfficeRoom room = new OfficeRoom();
        room.setName("Room 101");
        room.setRoomNumber("101");
        room.setFloor(floor);
        session.save(room);
        
        commitAndStartNewTransaction();

        // Attempt to delete floor with rooms (should fail with BAD_REQUEST)
        given()
        .when()
            .delete(getApiPath("/floors/" + floor.getId()))
        .then()
            .statusCode(Response.Status.BAD_REQUEST.getStatusCode());
    }

    @Test
    public void testDuplicateFloorNumber() {
        // Create first floor
        Floor floor1 = new Floor();
        floor1.setName("First Floor");
        floor1.setFloorNumber(1);
        session.save(floor1);

        given()
            .contentType(ContentType.JSON)
            .body(floor1)
        .when()
            .post(getApiPath("/floors"))
        .then()
            .statusCode(Response.Status.CREATED.getStatusCode());

        // Try to create another floor with same floor number
        Floor floor2 = new Floor();
        floor2.setName("Another First Floor");
        floor2.setFloorNumber(1);  // Same floor number
        session.save(floor2);

        given()
            .contentType(ContentType.JSON)
            .body(floor2)
        .when()
            .post(getApiPath("/floors"))
        .then()
            .statusCode(Response.Status.CONFLICT.getStatusCode());
    }

    private void createTestFloor(String name, int floorNumber) {
        Floor floor = new Floor();
        floor.setName(name);
        floor.setFloorNumber(floorNumber);
        session.save(floor);

        given()
            .contentType(ContentType.JSON)
            .body(floor)
        .when()
            .post(getApiPath("/floors"))
        .then()
            .statusCode(Response.Status.CREATED.getStatusCode());
    }
} 