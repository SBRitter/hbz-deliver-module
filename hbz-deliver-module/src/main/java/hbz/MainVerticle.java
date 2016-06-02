package hbz;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.json.Json;
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

public class MainVerticle extends AbstractVerticle {

    String patronId;
    String itemId;
    Patron patron;
    Item item;

    @Override
    public void start(Future<Void> fut) {
        Router router = Router.router(vertx);
        final int port = Integer.parseInt(System.getProperty("port", "8080"));

        router.route("/deliver/loan*").handler(BodyHandler.create());
        router.post("/deliver/loan").handler(this::loan);

        vertx.createHttpServer().requestHandler(router::accept).listen(port, result -> {
            if (result.succeeded()) {
                fut.complete();
            } else {
                fut.fail(result.cause());
            }
        });
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
                System.out.println("Found patron: " + patron.getPatronName());
                retrieveItem(routingContext);
            });
        }).putHeader("content-type", "application/json").end();
    }

    private void retrieveItem(RoutingContext routingContext) {
        HttpClient httpClient = vertx.createHttpClient();
        httpClient.get(8081, "localhost", "/apis/items/" + itemId, response -> {
            response.bodyHandler(buffer -> {
                item = Json.decodeValue(buffer.toString(), Item.class);
                System.out.println("Found item: " + item.getItemId());
                processLoan(routingContext);
            });
        }).putHeader("content-type", "application/json").end();
    }

    private void processLoan(RoutingContext routingContext) {
        // 1. Create loan for patron
        System.out.println("Processing loan");
        HttpClient httpClient = vertx.createHttpClient();
        Loan loan = createLoanObject();
        String loanAsJson = Json.encode(loan);
        httpClient.post(8081, "localhost", "/apis/patrons/" + patronId + "/loans/", response -> {
            System.out.println("Received response with status code " + response.statusCode());
        }).putHeader("content-type", "text/plain").putHeader("Accept", "application/json").end(loanAsJson);

        // 2. Update total loans
        // 3. Update item status

        routingContext.response().setStatusCode(200).putHeader("content-type", "application/json; charset=utf-8")
                .end("loaned " + itemId + " to " + patronId);
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

}
