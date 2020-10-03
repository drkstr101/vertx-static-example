package io.vertx.starter;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.net.JksOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.LoggerHandler;
import io.vertx.ext.web.handler.ResponseContentTypeHandler;
import io.vertx.ext.web.handler.StaticHandler;

public class Http2ServerVerticle extends AbstractVerticle {

	static final Logger logger = LoggerFactory.getLogger(Http2ServerVerticle.class);

	private HttpServer http1;

	private HttpServer http2;

	@Override
	public void start(final Promise<Void> promise) {
		CompositeFuture.all(startHttp1Server(), startHttp2Server())
				.onComplete(ar -> {
					if (ar.succeeded()) {
						promise.complete();
					} else {
						promise.fail(ar.cause());
					}
				});
	}

	@Override
	public void stop(final Future<Void> future) {
		http1.close(res -> http2.close(future));
	}

	private HttpServerOptions createOptions(final boolean http2) {

		final String host = config().getString(Config.HOST, "localhost");

		final HttpServerOptions serverOptions = new HttpServerOptions()
				.setPort(http2 ? config().getInteger(Config.HTTP2_PORT, 8443)
						: config().getInteger(Config.HTTP1_PORT, 8080))
				.setHost(host);

		if (http2) {
			final String certPath = config().getString(Config.CA_PATH, "conf/test-keystore.jks");
			final String certKey = config().getString(Config.CA_KEY, "secret");
			final JksOptions jksConfig = new JksOptions()
					.setPath(certPath)
					.setPassword(certKey);

			serverOptions.setSsl(true)
					.setKeyStoreOptions(jksConfig)
					.setUseAlpn(true);
		}

		return serverOptions;
	}

	private Future<Void> startHttp1Server() {
		final Promise<Void> promise = Promise.promise();

		final Router router = Router.router(vertx);
		router.get("/greeting")
				.handler(ResponseContentTypeHandler.create())
				.handler(this::greetingHandler);
		router.route().handler(LoggerHandler.create());
		router.post().handler(BodyHandler.create());
		router.route().handler(StaticHandler.create());

		http1 = vertx.createHttpServer(createOptions(false)).requestHandler(router);
		http1.listen(ar -> {
			if (ar.succeeded()) {
				logger.info("HTTP1 Server Ready: " + ar.result());
				promise.complete();
			} else {
				ar.cause().printStackTrace();
				promise.fail(ar.cause());
			}
		});

		return promise.future();
	}

	private Future<Void> startHttp2Server() {
		final Promise<Void> promise = Promise.promise();
		final Router router = Router.router(vertx);

		router.route().handler(StaticHandler.create());
		router.route().handler(LoggerHandler.create());
		router.post().handler(BodyHandler.create());
		router.get("/greeting")
				.handler(ResponseContentTypeHandler.create())
				.handler(this::greetingHandler);

		http2 = vertx.createHttpServer(createOptions(true)).requestHandler(router);
		http2.listen(ar -> {
			if (ar.succeeded()) {
				logger.info("HTTP2 Server Ready: " + ar.result());
				promise.complete();
			} else {
				ar.cause().printStackTrace();
				promise.fail(ar.cause());
			}
		});

		return promise.future();
	}

	private void greetingHandler(final RoutingContext context) {
		context.response().end("Hello Vert.x!");
	}

}
