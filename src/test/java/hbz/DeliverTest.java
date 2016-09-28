package hbz;

import java.io.IOException;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(VertxUnitRunner.class)
public class DeliverTest {

  private Vertx vertx;

  @Before
  public void setUp(TestContext context) throws IOException {
    vertx = Vertx.vertx();
    DeploymentOptions options = new DeploymentOptions()
        .setConfig(new JsonObject().put("http.port", 8080));
    vertx.deployVerticle(MainVerticle.class.getName(), options, context.asyncAssertSuccess());
  }

  @After
  public void tearDown(TestContext context) {
    vertx.close(context.asyncAssertSuccess());
  }

  @Test
  public void testFailingLoan(TestContext context) {
    Delivery failingDelivery = new Delivery("ABC123", "DEF456");
    String failingDeliveryAsJson = Json.encode(failingDelivery);
    final Async async = context.async();
    vertx.createHttpClient().post(8080, "localhost", "/deliver/loan/", response -> response.handler(body -> {
      context.assertTrue(body.toString().contains("Could not find patron with id"));
      async.complete();
    })).end(failingDeliveryAsJson);
  }

  @Test
  public void testFailingReturn(TestContext context) {
    ReturnRenewal failingReturn = new ReturnRenewal("XYZ789", "QQQ000");
    String failingReturnAsJson = Json.encode(failingReturn);
    final Async async = context.async();
    vertx.createHttpClient().post(8080, "localhost", "/deliver/return/", response -> response.handler(body -> {
      context.assertTrue(body.toString().contains("Did not find loan"));
      async.complete();
    })).end(failingReturnAsJson);
  }

  @Test
  public void dummyTest() {
    System.out.println("I am a dummy test");
  }

}