/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package oteldemo;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;


public final class Analytics {

  private static final Logger logger = LogManager.getLogger(Analytics.class);


  private static final CountDownLatch stopLatch = new CountDownLatch(1);


  private void start() throws IOException {

    logger.info("Analytics service started");
    Runtime.getRuntime()
        .addShutdownHook(
            new Thread(
                () -> {
                  // Use stderr here since the logger may have been reset by its JVM shutdown hook.
                  System.err.println(
                      "*** shutting down Analytics since JVM is shutting down");
                  Analytics.this.stop();
                  System.err.println("*** server shut down");
                }));

  }

  private void stop() {
    stopLatch.countDown();
  }


  /** Await termination on the main thread since the grpc library uses daemon threads. */
  private void blockUntilShutdown() throws InterruptedException {
    stopLatch.await();
  }

  /** Main launches the server from the command line. */
  public static void main(String[] args) throws IOException, InterruptedException {
    logger.info("Analytics starting.");
    final Analytics service = new Analytics();
    service.start();
    service.blockUntilShutdown();
  }
}
