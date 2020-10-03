package io.vertx.starter;

import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class MainVerticle extends AbstractVerticle {

	static final Logger logger = LoggerFactory.getLogger(MainVerticle.class);

	// Convenience method so you can run it in your IDE
	public static void main(final String[] args) {
		final VertxOptions options = new VertxOptions();
		final Vertx vertx = Vertx.vertx(options);
		vertx.deployVerticle(new MainVerticle());
	}

	private static ConfigRetriever configRetriever(final Vertx vertx) {

		final String configPath = System.getenv("VERTX_CONFIG_PATH");
		logger.info("VERTX_CONFIG_PATH: " + configPath);

		final ConfigStoreOptions fileStore = new ConfigStoreOptions()
				.setType("file")
				.setConfig(new JsonObject().put("path", (configPath == null) ? "conf/config.json" : configPath));

		final ConfigStoreOptions sysPropsStore = new ConfigStoreOptions().setType("sys");

		final ConfigRetrieverOptions options = new ConfigRetrieverOptions()
				.addStore(fileStore).addStore(sysPropsStore);

		return ConfigRetriever.create(vertx, options);
	}

	@Override
	public void start(final Promise<Void> promise) {
		configRetriever(vertx).getConfig(config -> {
			startHttp2Server(config.result())
					.onComplete(ar -> {
						if (ar.succeeded()) {
							promise.complete();
						} else {
							promise.fail(ar.cause());
						}
					});
		});

	}

	private Future<Void> startHttp2Server(final JsonObject config) {
		final Promise<Void> promise = Promise.promise();
		final DeploymentOptions options = new DeploymentOptions().setConfig(config);
		vertx.deployVerticle(Http2ServerVerticle.class, options, ar -> {
			if (ar.succeeded()) {
				promise.complete();
			} else {
				promise.fail(ar.cause());
			}
		});
		return promise.future();
	}
}
