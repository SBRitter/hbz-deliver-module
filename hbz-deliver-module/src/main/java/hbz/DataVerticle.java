package hbz;

import org.json.JSONArray;
import org.json.JSONObject;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.templ.ThymeleafTemplateEngine;

public class DataVerticle extends AbstractVerticle {

	private final Logger logger = LoggerFactory.getLogger("hbz-deliver-module");
	private final ThymeleafTemplateEngine engine = ThymeleafTemplateEngine.create();

	private String authorization = "a2VybWl0Omtlcm1pdA";
	private String tenant = "hbz";

	@Override
	public void start(Future<Void> fut) {
		Router router = Router.router(vertx);
		final int port = Integer.parseInt(System.getProperty("port", "8080"));

		router.route("/deliver*").handler(BodyHandler.create());
		router.get("/deliver/sampleData").handler(this::showSampleDataScreen);
		router.post("/deliver/createPatron").handler(this::createPatron);
		router.post("/deliver/createItem").handler(this::createItem);

		vertx.createHttpServer().requestHandler(router::accept).listen(port, result -> {
			if (result.succeeded()) {
				fut.complete();
			} else {
				fut.fail(result.cause());
			}
		});
	}

	private void showSampleDataScreen(RoutingContext routingContext) {
		HttpClient httpClient = vertx.createHttpClient();
		httpClient.get(9130, "localhost", "/apis/patrons/", response -> response.bodyHandler(buffer -> {
			try {
				JSONObject bufferAsJson = new JSONObject(buffer.toString());
				JSONArray patrons = bufferAsJson.getJSONArray("patrons");
				routingContext.put("patrons", patrons.toString(2));

				httpClient
						.get(9130, "localhost", "/apis/items/", itemResponse -> itemResponse.bodyHandler(itemBuffer -> {
							try {
								JSONObject itemBufferAsJson = new JSONObject(itemBuffer.toString());
								JSONArray items = itemBufferAsJson.getJSONArray("items");
								routingContext.put("items", items.toString(2));

								engine.render(routingContext, "templates/sampleData.html",
										engineResponse -> routingContext.response().setStatusCode(200)
												.putHeader(HttpHeaders.CONTENT_TYPE, "text/html")
												.end(engineResponse.result()));

							} catch (Exception e) {
								logger.error(e);
							}
						})).putHeader("content-type", "application/json").putHeader("accept", "application/json")
						.putHeader("authorization", authorization).putHeader("X-Okapi-Tenant", tenant).end();

			} catch (Exception e) {
				logger.error(e);
			}
		})).putHeader("content-type", "application/json").putHeader("accept", "application/json")
				.putHeader("authorization", authorization).putHeader("X-Okapi-Tenant", tenant).end();
	}

	private void createPatron(RoutingContext routingContext) {
		String patron = routingContext.getBodyAsString();
		HttpClient httpClient = vertx.createHttpClient();
		httpClient.post(9130, "localhost", "/apis/patrons/", response -> {
			if (response.statusCode() == 201) {
				routingContext.response().setStatusCode(201).putHeader(HttpHeaders.CONTENT_TYPE, "text/html")
						.end("Patron created");
			} else {
				routingContext.response().setStatusCode(500).putHeader(HttpHeaders.CONTENT_TYPE, "text/html")
						.end("Error creating patron");
			}
		}).putHeader("content-type", "application/json").putHeader("accept", "text/plain")
				.putHeader("authorization", authorization).putHeader("X-Okapi-Tenant", tenant).end(patron);

	}
	
	private void createItem(RoutingContext routingContext) {
		String patron = routingContext.getBodyAsString();
		HttpClient httpClient = vertx.createHttpClient();
		httpClient.post(9130, "localhost", "/apis/items/", response -> {
			if (response.statusCode() == 201) {
				routingContext.response().setStatusCode(201).putHeader(HttpHeaders.CONTENT_TYPE, "text/html")
						.end("Item created");
			} else {
				routingContext.response().setStatusCode(500).putHeader(HttpHeaders.CONTENT_TYPE, "text/html")
						.end("Error creating item");
			}
		}).putHeader("content-type", "application/json").putHeader("accept", "text/plain")
				.putHeader("authorization", authorization).putHeader("X-Okapi-Tenant", tenant).end(patron);

	}

}
