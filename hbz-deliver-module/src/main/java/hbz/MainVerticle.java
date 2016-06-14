package hbz;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.json.Json;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.core.http.HttpClient;

import com.sling.rest.jaxrs.model.CircDesk;
import com.sling.rest.jaxrs.model.Item;
import com.sling.rest.jaxrs.model.ItemPolicy;
import com.sling.rest.jaxrs.model.Library;
import com.sling.rest.jaxrs.model.Loan;
import com.sling.rest.jaxrs.model.LocationCode;
import com.sling.rest.jaxrs.model.Patron;
import com.sling.rest.jaxrs.model.Status;

public class MainVerticle extends AbstractVerticle {

  String patronId;
  String itemId;
  Patron patron;
  Item item;
  String loanId;

  private final Logger logger = LoggerFactory.getLogger("hbz-deliver-module");

  @Override
  public void start(Future<Void> fut) {
    Router router = Router.router(vertx);
    final int port = Integer.parseInt(System.getProperty("port", "8080"));

    router.route("/deliver*").handler(BodyHandler.create());
    router.get("/deliver").handler(this::sayHello);
    router.post("/deliver/loan").handler(this::loan);
    router.post("/deliver/return").handler(this::returnItem);

    vertx.createHttpServer().requestHandler(router::accept).listen(port, result -> {
      if (result.succeeded()) {
        fut.complete();
      } else {
        fut.fail(result.cause());
      }
    });
  }

  private void sayHello(RoutingContext routingContext) {
    routingContext.response().setStatusCode(200).putHeader("content-type", "application/json; charset=utf-8")
        .end("hbz deliver module.");
  }

  private void loan(RoutingContext routingContext) {
    final Delivery delivery = Json.decodeValue(routingContext.getBodyAsString(), Delivery.class);
    patronId = delivery.getPatron();
    itemId = delivery.getItem();
    retrievePatron(routingContext);
  }

  private void retrievePatron(RoutingContext routingContext) {
    HttpClient httpClient = vertx.createHttpClient();
    httpClient.get(8081, "localhost", "/apis/patrons/" + patronId, response -> {
      response.bodyHandler(buffer -> {
        patron = Json.decodeValue(buffer.toString(), Patron.class);
        logger.info("Found patron: " + patron.getPatronName());
        retrieveItem(routingContext);
      });
    }).putHeader("content-type", "application/json").end();
  }

  private void retrieveItem(RoutingContext routingContext) {
    HttpClient httpClient = vertx.createHttpClient();
    httpClient.get(8081, "localhost", "/apis/items/" + itemId, response -> {
      response.bodyHandler(buffer -> {
        item = Json.decodeValue(buffer.toString(), Item.class);
        logger.info("Found item: " + item.getItemId());
        processLoan(routingContext);
      });
    }).putHeader("content-type", "application/json").end();
  }

  private void processLoan(RoutingContext routingContext) {
    logger.info("Processing loan");
    createLoanForPatron();
    updateItemStatus("02", "ITEM_STATUS_ON_LOAN");
    routingContext.response().setStatusCode(200).putHeader("content-type", "application/json; charset=utf-8")
        .end("loaned " + itemId + " to " + patronId);
  }

  private void createLoanForPatron() {
    logger.info("Creating loan for patron");
    HttpClient httpClient = vertx.createHttpClient();
    Loan loan = createLoanObject();
    String loanAsJson = Json.encode(loan);
    httpClient.post(8081, "localhost", "/apis/patrons/" + patronId + "/loans/", response -> {
      logger.info("Received response with status code " + response.statusCode());
    }).putHeader("content-type", "text/plain").putHeader("Accept", "application/json").end(loanAsJson);
  }

  private void updateItemStatus(String statusValue, String statusDescription) {
    logger.info("Updating item status");
    HttpClient httpClient = vertx.createHttpClient();
    Status status = new Status();
    status.setValue(statusValue);
    status.setDesc(statusDescription);
    item.setStatus(status);
    String itemAsJson = Json.encode(item);
    httpClient.put(8081, "localhost", "/apis/items/" + itemId, response -> {
      logger.info("Received response with status code " + response.statusCode());
    }).putHeader("content-type", "text/plain").putHeader("Accept", "application/json").end(itemAsJson);
  }

  private Loan createLoanObject() {
    Loan loan = new Loan();
    loan.setPatronId(patronId);
    loan.setItemBarcode(item.getBarcode());
    loan.setItemId(item.getItemId());
    loan.setDueDate((int) System.currentTimeMillis() + (86400 * 7 * 1000));
    loan.setItemPolicy(new ItemPolicy());
    loan.setCircDesk(new CircDesk());
    loan.setLoanStatus("loanStatus");
    loan.setTitle("title");
    loan.setLocationCode(new LocationCode());
    loan.setLoanFine(123);
    loan.setRenewable(true);
    loan.setLoanDate((int) System.currentTimeMillis());
    loan.setLibrary(new Library());
    return loan;
  }

  private void returnItem(RoutingContext routingContext) {
    final ItemReturn itemReturn = Json.decodeValue(routingContext.getBodyAsString(), ItemReturn.class);
    patronId = itemReturn.getPatron();
    loanId = itemReturn.getLoan();
    HttpClient httpClient = vertx.createHttpClient();
    httpClient.get(8081, "localhost", "/apis/patrons/" + patronId + "/loans/" + loanId, response -> {   
      response.bodyHandler(buffer -> {
        item = Json.decodeValue(buffer.toString(), Item.class);
        itemId = item.getItemId();
        updateItemStatus("01", "ITEM_STATUS_MISSING");
      });
      deleteLoanForPatron(routingContext);
    }).putHeader("content-type", "text/plain").end();
  }

  private void deleteLoanForPatron(RoutingContext routingContext) {
    HttpClient httpClient = vertx.createHttpClient();
    httpClient.delete(8081, "localhost", "/apis/patrons/" + patronId + "/loans/" + loanId, response2 -> {
      logger.info("Received response with status code " + response2.statusCode());
      routingContext.response().setStatusCode(200)
          .putHeader("content-type", "application/json; charset=utf-8")
          .end("returned " + loanId + " for " + patronId);
    }).putHeader("content-type", "text/plain").end();
  }
  
}
