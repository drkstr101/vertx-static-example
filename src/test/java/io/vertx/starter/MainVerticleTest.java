package io.vertx.starter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.vertx.core.Vertx;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.codec.BodyCodec;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;

@ExtendWith(VertxExtension.class)
class MainVerticleTest {

	@BeforeEach
	void prepare(final Vertx vertx, final VertxTestContext testContext) {
		vertx.deployVerticle(MainVerticle.class.getCanonicalName(),
				testContext.succeeding(id -> testContext.completeNow()));
	}

	@Test
	@DisplayName("Check that the server returns greeting")
	void checkServerHasStarted(final Vertx vertx, final VertxTestContext testContext) {
		final WebClient webClient = WebClient.create(vertx);
		webClient.get(8080, "localhost", "/greeting")
				.as(BodyCodec.string())
				.send(testContext.succeeding(response -> testContext.verify(() -> {
					assertEquals(200, response.statusCode());
					assertTrue(response.body().length() > 0);
					assertTrue(response.body().contains("Hello Vert.x!"));
					testContext.completeNow();
				})));
	}

	@Test
	@DisplayName("Check that the server returns hello-webroot.txt")
	void checkServerHasRootResource(final Vertx vertx, final VertxTestContext testContext) {
		final WebClient webClient = WebClient.create(vertx);
		webClient.get(8080, "localhost", "/hello-webroot.txt")
				.as(BodyCodec.string())
				.send(testContext.succeeding(response -> testContext.verify(() -> {
					assertEquals(200, response.statusCode());
					testContext.completeNow();
				})));
	}

}
