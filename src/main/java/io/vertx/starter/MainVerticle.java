package io.vertx.starter;

import io.vertx.core.AbstractVerticle;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.StaticHandler;


public class MainVerticle extends AbstractVerticle {

  @Override
  public void start() {
    final Router router = Router.router(vertx);

    router.get("/greeting").handler(req -> req.response().end("Hello Vert.x!"));

    router.route("/*").handler(StaticHandler.create());

    vertx.createHttpServer()
        .requestHandler(router)
        .listen(8080);
  }

}
