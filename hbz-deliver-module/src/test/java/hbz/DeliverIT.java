package hbz;

import static com.jayway.restassured.RestAssured.given;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.jayway.restassured.RestAssured;

public class DeliverIT {

  static private String patronId;
  static private String itemId;

  @BeforeClass
  public static void configureRestAssured() throws IOException {
    RestAssured.baseURI = "http://localhost";
    RestAssured.port = 8081;
    createPatron();
    createItem();

  }

  @AfterClass
  public static void unconfigureRestAssured() {
    RestAssured.reset();
  }

  public static void createPatron() throws IOException {
    String patronData = new String(Files.readAllBytes(Paths.get("patron_sample.json")));
    patronId = given()
        .header("accept", "application/json")
        .header("content-type", "text/plain")
        .header("authorization", "Bearer a2VybWl0Omtlcm1pdA==")
        .body(patronData)
        .request()
        .post("/apis/patrons")
        .andReturn()
        .getHeader("location");
  }

  public static void createItem() throws IOException {
    String itemData = new String(Files.readAllBytes(Paths.get("item_sample.json")));
    itemId = given()
        .header("accept", "application/json")
        .header("content-type", "text/plain")
        .header("authorization", "Bearer a2VybWl0Omtlcm1pdA==")
        .body(itemData)
        .request()
        .post("/apis/items")
        .andReturn()
        .getHeader("location");
  }

  @Test
  public void test() {
    RestAssured.port = 8080;
    String loanResponse = given()
        .body("{\"patron\":\"" + patronId + "\", \"item\":\"" + itemId + "\"}")
        .request()
        .post("/deliver/loan")
        .andReturn()
        .asString();
    System.out.println(loanResponse);
  }

}
