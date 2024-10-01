package rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dk.cphbusiness.persistence.HibernateConfig;
import dk.cphbusiness.persistence.entities.IJPAEntity;
import dk.cphbusiness.rest.ApplicationConfig;
import dk.cphbusiness.rest.RestRoutes;
import dk.cphbusiness.security.SecurityRoutes;
import io.restassured.RestAssured;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.BeforeEach;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

public class PoemRestTest {

    private static ApplicationConfig appConfig;
    private static EntityManagerFactory emfTest;
    private static ObjectMapper jsonMapper = new ObjectMapper();
    Map<String, IJPAEntity> populated;

    @BeforeAll
    static void setUpAll() {
        RestAssured.baseURI = "http://localhost:7777/api";

        HibernateConfig.setTestMode(true); // IMPORTANT leave this at the very top of this method in order to use the test database
        RestRoutes restRoutes = new RestRoutes();

        // Setup test database using docker testcontainers
        emfTest = HibernateConfig.getEntityManagerFactoryForTest();

        // Start server
        appConfig = ApplicationConfig.
                getInstance()
                .initiateServer()
//                .checkSecurityRoles()
//                .setErrorHandling()
//                .setGeneralExceptionHandling()
//                .setRoutes(SecurityRoutes.getSecurityRoutes())
//                .setRoutes(SecurityRoutes.getSecuredRoutes())
                .setRoutes(restRoutes.getPoemRoutes()) // A different way to get the EndpointGroup. Getting data from DB
//                .setCORS()
//                .setApiExceptionHandling()
                .startServer(7777);
    }

    @AfterAll
    static void afterAll() {
        HibernateConfig.setTestMode(false);
        appConfig.stopServer();
    }

    @BeforeEach
    void setUpEach() {
        // Setup test database for each test
        new TestUtils().createUsersAndRoles(emfTest);
        // Setup DB Persons and Addresses
        populated = new TestUtils().createPoemsInDB(emfTest);
        populated.values().forEach(System.out::println);
    }

    @Test
    @DisplayName("Test if server is up")
    public void testServerIsUp() {
        System.out.println("Testing is server UP");
        given().when().get("/poem").then().statusCode(200);
    }

    private static String securityToken;
    private static void login(String username, String password) {
        ObjectNode objectNode = jsonMapper.createObjectNode()
                .put("username", username)
                .put("password", password);
        String loginInput = objectNode.toString();
        securityToken = given()
                .contentType("application/json")
                .body(loginInput)
                //.when().post("/api/login")
                .when().post("/auth/login")
                .then()
                .extract().path("token");
        System.out.println("TOKEN ---> " + securityToken);
    }

    @Test
    @DisplayName("Test login for user")
    public void testRestForUser() {
        login("user", "user123");
        given()
                .contentType("application/json")
                .accept("application/json")
                .header("Authorization", "Bearer "+securityToken)
                .when()
                .get("/poem").then()
                .statusCode(200);
    }

    @Test
    @DisplayName("Test CORS Headers")
    public void testCorsHeaders() {
        given()
                .when()
                .get("/poem")
                .then()
                .header("Access-Control-Allow-Origin", "*")
                .header("Access-Control-Allow-Methods", "GET, POST, PUT, PATCH, DELETE, OPTIONS")
                .header("Access-Control-Allow-Headers", "Content-Type, Authorization")
                .statusCode(200);
    }


    @Test
    @DisplayName("Test Entities from DB")
    public void testEntitiesFromDB() {
        login("admin", "admin123");
        given()
                .contentType("application/json")
                .accept("application/json")
                .header("Authorization", "Bearer "+securityToken)
                .log().all()
                .when()
                .get("/poem").then()
                .log().all()
                .statusCode(200)
                .body("size()", equalTo(2));
    }

    @Test
    @DisplayName("Test Get single poem by id")
    public void testGetById() {
        login("admin", "admin123");
        given()
                .contentType("application/json")
                .accept("application/json")
                .header("Authorization", "Bearer "+securityToken)
                .when()
                .get("/poem/"+populated.get("poem1").getId())
                .then()
                .log().body().statusCode(200);
    }

    @Test
    @DisplayName("Test Log request details")
    public void testLogRequest() {
        System.out.println("Testing logging request details");
        given()
                .log().all()
                .when().get("/poem")
                .then().statusCode(200);
    }

    @Test
    @DisplayName("Test Log response details")
    public void testLogResponse() {
        System.out.println("Testing logging response details");
        given()
                .when().get("/poem")
                .then().log().body().statusCode(200);
    }
}
