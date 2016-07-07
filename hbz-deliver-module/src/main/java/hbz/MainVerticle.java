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

import org.kie.api.KieServices;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;

import com.sling.rest.jaxrs.model.CircDesk;
import com.sling.rest.jaxrs.model.Item;
import com.sling.rest.jaxrs.model.ItemPolicy;
import com.sling.rest.jaxrs.model.Library;
import com.sling.rest.jaxrs.model.Loan;
import com.sling.rest.jaxrs.model.LocationCode;
import com.sling.rest.jaxrs.model.Patron;
import com.sling.rest.jaxrs.model.Status;

public class MainVerticle extends AbstractVerticle {

  Delivery delivery;
  String patronId;
  Patron patron;
  String itemId;
  Item item;
  String loanId;
  Loan loan;
  String authorization = "a2VybWl0Omtlcm1pdA";

  KieServices kieServices = KieServices.Factory.get();
  KieContainer kContainer = kieServices.getKieClasspathContainer();

  private final Logger logger = LoggerFactory.getLogger("hbz-deliver-module");

  @Override
  public void start(Future<Void> fut) {
    Router router = Router.router(vertx);
    final int port = Integer.parseInt(System.getProperty("port", "8080"));

    router.route("/deliver*").handler(BodyHandler.create());
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

  private void loan(RoutingContext routingContext) {
    delivery = Json.decodeValue(routingContext.getBodyAsString(), Delivery.class);
    patronId = delivery.getPatron();
    itemId = delivery.getItem();
    retrievePatron(routingContext);
  }

  private void retrievePatron(RoutingContext routingContext) {
    HttpClient httpClient = vertx.createHttpClient();
    httpClient.get(8081, "localhost", "/apis/patrons/" + patronId, response -> {
      response.bodyHandler(buffer -> {
        patron = Json.decodeValue(buffer.toString(), Patron.class);
        logger.info("Found patron: " + patron.getPatronName() + " with id " + patronId);
        retrieveItem(routingContext);
      });
    })
    .putHeader("content-type", "application/json")
    .putHeader("accept", "application/json")
    .putHeader("authorization", authorization)
    .end();
  }

  private void retrieveItem(RoutingContext routingContext) {
    HttpClient httpClient = vertx.createHttpClient();
    httpClient.get(8081, "localhost", "/apis/items/" + itemId, response -> {
      response.bodyHandler(buffer -> {
        item = Json.decodeValue(buffer.toString(), Item.class);
        logger.info("Found item: " + item.getId());
        processLoan(routingContext);
      });
    })
    .putHeader("content-type", "application/json")
    .putHeader("accept", "application/json")
    .putHeader("authorization", authorization)
    .end();
  }

  private void processLoan(RoutingContext routingContext) {
    logger.info("Processing loan...");
    logger.info("Checking rules...");
    LoanPermission loanPermission = new LoanPermission();
    KieSession kSession = kContainer.newKieSession("ksession-rules");
    kSession.insert(patron);
    kSession.insert(item);
    kSession.insert(loanPermission);
    kSession.fireAllRules();
    if (loanPermission.isPermitted() == true) {
      createLoanForPatron(routingContext);
    } else {
      routingContext.response().setStatusCode(200).putHeader("content-type", "application/json; charset=utf-8")
          .end("Cannot loan! Either item is loaned or patron is not allowed.");
    }
    kSession.destroy();
  }

  private void createLoanForPatron(RoutingContext routingContext) {
    HttpClient httpClient = vertx.createHttpClient();
    loan = createLoanObject();
    String loanAsJson = Json.encode(loan);
    httpClient.post(8081, "localhost", "/apis/patrons/" + patronId + "/loans/", response -> {
      response.bodyHandler(buffer -> {
        loan = Json.decodeValue(buffer.toString(), Loan.class);
        logger.info(buffer.toString());
        logger.info("Created loan with id " + loan.getId() + " for patron " + patronId);
      });
      updateItemStatus(item.getId(), "02", "ITEM_STATUS_ON_LOAN", routingContext);
    })
    .putHeader("content-type", "application/json")
    .putHeader("accept", "text/plain")
    .putHeader("authorization", authorization)
    .end(loanAsJson);
  }

  private void updateItemStatus(String itemId, String statusValue, String statusDescription,
      RoutingContext routingContext) {
    HttpClient httpClient = vertx.createHttpClient();
    Status status = new Status();
    status.setValue(statusValue);
    status.setDesc(statusDescription);
    item.setStatus(status);
    String itemAsJson = Json.encode(item);
    httpClient.put(8081, "localhost", "/apis/items/" + itemId, response -> {
      logger.info("Updated item status for item " + itemId);
      routingContext.response().setStatusCode(200).putHeader("content-type", "application/json; charset=utf-8")
          .end("Updated item status for item " + itemId + " to " + statusDescription);
    })
    .putHeader("content-type", "application/json")
    .putHeader("accept", "text/plain")
    .putHeader("authorization", authorization)
    .end(itemAsJson);
  }

  private Loan createLoanObject() {
    Loan loan = new Loan();
    loan.setPatronId(patronId);
    loan.setItemBarcode(item.getBarcode());
    loan.setItemId(item.getId());
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
        Loan loan = Json.decodeValue(buffer.toString(), Loan.class);
        logger.info("Found loan " + loanId + " for patron " + patronId);
        itemId = loan.getItemId();
        deleteLoanForPatron(routingContext);
        updateItemStatus(itemId, "01", "ITEM_STATUS_MISSING", routingContext);
      });
    })
    .putHeader("content-type", "application/json")
    .putHeader("accept", "application/json")
    .putHeader("authorization", authorization)
    .end();
  }

  private void deleteLoanForPatron(RoutingContext routingContext) {
    logger.info("Deleting loan " + loanId + " for patron " + patronId);
    HttpClient httpClient = vertx.createHttpClient();
    httpClient.delete(8081, "localhost", "/apis/patrons/" + patronId + "/loans/" + loanId, response -> {
      logger.info("Deleted loan " + loanId + " for " + patronId);
    })
    .putHeader("content-type", "application/json")
    .putHeader("accept", "text/plain")
    .putHeader("authorization", authorization)
    .end();
  }
}