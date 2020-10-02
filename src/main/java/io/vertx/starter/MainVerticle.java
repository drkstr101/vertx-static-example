package io.vertx.starter;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.net.JksOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.LoggerHandler;
import io.vertx.ext.web.handler.SessionHandler;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.ext.web.sstore.LocalSessionStore;

public class MainVerticle extends AbstractVerticle {

	static final String CONFIG_ADDRESS = "server.address";

	static final String CONFIG_CA_PATH = "server.ca.path";

	static final String CONFIG_CA_KEY = "server.ca.key";

	static final String CONFIG_HTTP_PORT = "server.http.port";

	static final String CONFIG_HTTPS_PORT = "server.https.port";

	static final Logger logger = LoggerFactory.getLogger(MainVerticle.class);

	// Convenience method so you can run it in your IDE
	public static void main(final String[] args) {
		final VertxOptions options = new VertxOptions();
		final Vertx vertx = Vertx.vertx(options);
		vertx.deployVerticle(new MainVerticle());
	}

	@Override
	public void start(final Promise<Void> promise) {

		// start servers for http/https handling
		CompositeFuture.all(startHttpServer(), startHttpsServer())
				.onComplete(ar -> {
					if (ar.succeeded()) {
						promise.complete();
					} else {
						promise.fail(ar.cause());
					}
				});

	}

	private Future<Void> startHttpServer() {
		final Promise<Void> promise = Promise.promise();

		final Router router = Router.router(vertx);

		router.get("/greeting").handler(req -> req.response().end("Hello Vert.x!"));
		router.route().handler(LoggerHandler.create());
		router.post().handler(BodyHandler.create());
		router.route().handler(StaticHandler.create());

		final int port = config().getInteger(CONFIG_HTTP_PORT, 80);
		final String address = config().getString(CONFIG_ADDRESS, "0.0.0.0");
		vertx.createHttpServer()
				.requestHandler(router)
				.listen(port, address, ar -> {
					if (ar.succeeded()) {
						promise.complete();
					} else {
						promise.fail(ar.cause());
					}
				});
		return promise.future();
	}

	private Future<Void> startHttpsServer() {
		final Promise<Void> promise = Promise.promise();
		final Router router = Router.router(vertx);

		final SessionHandler sessionHandler = SessionHandler.create(LocalSessionStore.create(vertx))
				.setCookieHttpOnlyFlag(true);
		sessionHandler.setCookieSecureFlag(true);

		router.route().handler(StaticHandler.create());
		router.route().handler(LoggerHandler.create());
		router.post().handler(BodyHandler.create());
		router.get("/greeting").handler(req -> req.response().end("Hello Vert.x!"));

		final int port = config().getInteger(CONFIG_HTTPS_PORT, 443);
		final String address = config().getString(CONFIG_ADDRESS, "0.0.0.0");
		final String certPath = config().getString(CONFIG_CA_PATH, "keystore/test-keystore.jks");
		final String certKey = config().getString(CONFIG_CA_KEY, "secret");

		final JksOptions jksConfig = new JksOptions()
				.setPath(certPath)
				.setPassword(certKey);

		final HttpServerOptions httpOpts = new HttpServerOptions()
				.setSsl(true)
				.setKeyStoreOptions(jksConfig);

		vertx.createHttpServer(httpOpts)
				.requestHandler(router)
				.listen(port, address, ar -> {
					if (ar.succeeded()) {
						promise.complete();
					} else {
						promise.fail(ar.cause());
					}
				});
		return promise.future();
	}

}
