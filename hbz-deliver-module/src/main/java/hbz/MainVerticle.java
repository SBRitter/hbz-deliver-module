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
import io.vertx.core.http.HttpHeaders;

import org.json.JSONArray;
import org.json.JSONObject;
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

import io.vertx.ext.web.templ.ThymeleafTemplateEngine;

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
  private final ThymeleafTemplateEngine engine = ThymeleafTemplateEngine.create();

  @Override
  public void start(Future<Void> fut) {
    Router router = Router.router(vertx);
    final int port = Integer.parseInt(System.getProperty("port", "8080"));
    router.route("/deliver*").handler(BodyHandler.create());
    router.get("/deliver/loan").handler(this::showLoanScreen);
    router.post("/deliver/loan").handler(this::loan);
    router.post("/deliver/return").handler(this::returnItem);
    router.get("/deliver/loans/:patronId").handler(this::getLoansForPatron);
    router.get("/deliver/listLoans").handler(this::showLoanListScreen);
    vertx.createHttpServer().requestHandler(router::accept).listen(port, result -> {
      if (result.succeeded()) {
        fut.complete();
      } else {
        fut.fail(result.cause());
      }
    });
  }

  private void showLoanScreen(RoutingContext routingContext) {
    engine.render(routingContext, "templates/loan.html", response -> {
      routingContext.response().putHeader(HttpHeaders.CONTENT_TYPE, "text/html").end(response.result());
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
      if (response.statusCode() == 200) {
        response.bodyHandler(buffer -> {
          patron = Json.decodeValue(buffer.toString(), Patron.class);
          logger.info("Found patron: " + patron.getPatronName() + " with id " + patronId);
          retrieveItem(routingContext);
        });
      } else {
        routingContext.response().putHeader(HttpHeaders.CONTENT_TYPE, "text/html")
            .end("Could not find patron with id " + patronId);
      }
    }).putHeader("content-type", "application/json")
        .putHeader("accept", "application/json")
        .putHeader("authorization", authorization)
        .end();
  }

  private void retrieveItem(RoutingContext routingContext) {
    HttpClient httpClient = vertx.createHttpClient();
    httpClient.get(8081, "localhost", "/apis/items/" + itemId, response -> {
      if (response.statusCode() == 200) {
        response.bodyHandler(buffer -> {
          item = Json.decodeValue(buffer.toString(), Item.class);
          logger.info("Found item: " + item.getId());
          processLoan(routingContext);
        });
      } else {
        routingContext.response().putHeader(HttpHeaders.CONTENT_TYPE, "text/html")
            .end("Could not find item with id " + itemId);
      }
    }).putHeader("content-type", "application/json")
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
      routingContext.response().end("Cannot loan! Either item is loaned or patron is not allowed.");
    }
    kSession.destroy();
  }

  private void createLoanForPatron(RoutingContext routingContext) {
    HttpClient httpClient = vertx.createHttpClient();
    loan = createLoanObject();
    String loanAsJson = Json.encode(loan);
    httpClient.post(8081, "localhost", "/apis/patrons/" + patronId + "/loans/", response -> {
      if (response.statusCode() == 201) {
        response.bodyHandler(buffer -> {
          loan = Json.decodeValue(buffer.toString(), Loan.class);
          logger.info(buffer.toString());
          logger.info("Created loan with id " + loan.getId() + " for patron " + patronId);
        });
        updateItemStatus(item.getId(), "02", "ITEM_STATUS_ON_LOAN", routingContext);
      } else {
        routingContext.response().putHeader(HttpHeaders.CONTENT_TYPE, "text/html")
            .end("Could not create loan");
      }
    }).putHeader("content-type", "application/json")
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
      if (response.statusCode() == 204) {
        logger.info("Updated item status for item " + itemId);
        routingContext.response().setStatusCode(200).end("Updated item status for item " + itemId + " to " + statusDescription);
      } else {
        routingContext.response().putHeader(HttpHeaders.CONTENT_TYPE, "text/html")
            .end("Could not update item status");
      }
    }).putHeader("content-type", "application/json")
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
      if (response.statusCode() == 200) {
        response.bodyHandler(buffer -> {
          Loan loan = Json.decodeValue(buffer.toString(), Loan.class);
          logger.info("Found loan " + loanId + " for patron " + patronId);
          itemId = loan.getItemId();
          deleteLoanForPatron(routingContext);
          updateItemStatus(itemId, "01", "ITEM_STATUS_MISSING", routingContext);
        });
      } else {
        routingContext.response().putHeader(HttpHeaders.CONTENT_TYPE, "text/html")
            .end("Did not find loan " + loanId + " for patron " + patronId);
      }
    }).putHeader("content-type", "application/json")
        .putHeader("accept", "application/json")
        .putHeader("authorization", authorization)
        .end();
  }

  private void deleteLoanForPatron(RoutingContext routingContext) {
    logger.info("Deleting loan " + loanId + " for patron " + patronId);
    HttpClient httpClient = vertx.createHttpClient();
    httpClient.delete(8081, "localhost", "/apis/patrons/" + patronId + "/loans/" + loanId, response -> {
      if (response.statusCode() == 204) {
        logger.info("Deleted loan " + loanId + " for " + patronId);
      } else {
        routingContext.response().putHeader(HttpHeaders.CONTENT_TYPE, "text/html")
            .end("Could not delete loan with id " + loanId);
      }
    }).putHeader("content-type", "application/json")
        .putHeader("accept", "text/plain")
        .putHeader("authorization", authorization)
        .end();

  }

  private void getLoansForPatron(RoutingContext routingContext) {
    final String patronId = routingContext.request().getParam("patronId");
    HttpClient httpClient = vertx.createHttpClient();
    httpClient.get(8081, "localhost", "/apis/patrons/" + patronId + "/loans", response -> {
      response.bodyHandler(buffer -> {
        try {
          JSONObject bufferAsJson = new JSONObject(buffer.toString());
          JSONArray loans = bufferAsJson.getJSONArray("loans");
          if (loans.length() > 0) {
            routingContext.response().end(loans.toString());
          } else {
            routingContext.response().end("Error: No loans found");
          }
        } catch (Exception e) {
          e.printStackTrace();
        }
      });
    }).putHeader("content-type", "application/json")
        .putHeader("accept", "application/json")
        .putHeader("authorization", authorization)
        .end();
  }

  private void showLoanListScreen(RoutingContext routingContext) {
    engine.render(routingContext, "templates/loans.html", response -> {
      routingContext.response().putHeader(HttpHeaders.CONTENT_TYPE, "text/html").end(response.result());
    });
  }
}