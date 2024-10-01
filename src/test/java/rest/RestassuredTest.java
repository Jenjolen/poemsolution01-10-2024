package rest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dk.cphbusiness.persistence.HibernateConfig;
import dk.cphbusiness.persistence.daos.PoemDAO;
import dk.cphbusiness.persistence.entities.IJPAEntity;
import dk.cphbusiness.persistence.entities.Poem;
import dk.cphbusiness.rest.ApplicationConfig;
import dk.cphbusiness.rest.RestRoutes;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.*;

import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class RestassuredTest {

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
                .setApi404ExceptionHandling()
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
    void testGetAllPoems() {
        given()// gherkin syntax
                .when()
                .get("/poem/")
                .then()
                .statusCode(200)
                .body("size()", equalTo(2));
    }

    @Test
    void testGetPoemById() {
        given()
//                .log().all() // log request
                .when()
                .get("/poem/" + populated.get("poem1").getId())
                .then()
//                .log().all() // log response
                .statusCode(200)
                .body("id", equalTo(1))
                .body("title", equalTo("Roses"));
    }


    @Test
    void testGetNonExistingPoemById() {
        given()
                .when()
                .get("/poem/100")
                .then()
                .statusCode(404)
                .body("msg", equalTo("Poem with id: " + 100 + " not found"));
    }


    @Test
    void testCreatePoem() {
        ObjectNode node = jsonMapper.createObjectNode();
        node.put("title", "Test title");
        node.put("author", "Test author");
        node.put("text", "Poem text");
        String json  = node.toString();
        given()
                .header("Content-Type", "application/json")
                .accept("application/json")
                .body(json)
                .when()
                .post("/poem")
                .then()
                .statusCode(201)
                .body("title", equalTo("Test title"))
                .body("id", equalTo(3));


    }


    @Test
    void testUpdatePoem() throws JsonProcessingException {
//        ObjectNode node = jsonMapper.createObjectNode();
//        node.put("title", "Updated title");
//        node.put("author", "Updated author");
//        node.put("text", "Updated text");
//        String json = node.toString();
        ObjectMapper objectMapper = new ObjectMapper();
        Poem poem = new Poem("Updated title", "Updated author", "Updated text");
        String json = objectMapper.writeValueAsString(poem);
        given()
                .header("Content-Type", "application/json")
                .accept("application/json")
                .body(json)
                .when()
                .put("/poem/" + populated.get("poem1").getId())
                .then()
                .statusCode(200)
                .body("title", equalTo("Updated title"))
                .body("author", equalTo("Updated author"))
                .body("text", equalTo("Updated text"));

    }


    @Test
    void testDeletePoem() {
        given()
                .when()
                .delete("/poem/" + populated.get("poem1").getId())
                .then()
                .statusCode(200);

        given()
                .when()
                .get("/poem")
                .then()
                .body("size()", equalTo(1));


        given()
                .when()
                .get("/poem/" + populated.get("poem1").getId())
                .then()
                .statusCode(404)
                .body("msg", equalTo("Poem with id: " + populated.get("poem1").getId() + " not found"));

    }




    @Test
    @DisplayName("Test JsonPath") // et custom navn for hvad test metoden vises som i test resultaterne
    void testJsonPath() {
        Response response = given().get("/poem");
        List<String> titles = response.jsonPath().get("title");
        System.out.println("Title: " + titles);
        assertTrue(titles.contains("Roses"));
    }





}
