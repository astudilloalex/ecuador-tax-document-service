package com.alexastudillo.taxdocument.support.fiscalpreparation;

import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.NetServer;
import io.vertx.core.net.NetSocket;
import java.time.Duration;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/** Test-only PostgreSQL TCP proxy that can drop the first COMMIT acknowledgement. */
public final class PostgreSqlCommitFaultProxy implements AutoCloseable {
  private static final Duration WAIT = Duration.ofSeconds(5);

  private final Vertx vertx;
  private final NetServer server;
  private final CommitFaultState state;
  private final Set<NetSocket> connections;

  private PostgreSqlCommitFaultProxy(
      Vertx vertx, NetServer server, CommitFaultState state, Set<NetSocket> connections) {
    this.vertx = vertx;
    this.server = server;
    this.state = state;
    this.connections = connections;
  }

  public static PostgreSqlCommitFaultProxy start(String upstreamHost, int upstreamPort) {
    Objects.requireNonNull(upstreamHost, "upstreamHost");
    Vertx vertx = Vertx.vertx();
    CommitFaultState state = new CommitFaultState();
    Set<NetSocket> connections = ConcurrentHashMap.newKeySet();
    NetServer server =
        vertx
            .createNetServer()
            .connectHandler(
                client -> {
                  connections.add(client);
                  client.pause();
                  client.closeHandler(ignored -> connections.remove(client));
                  vertx
                      .createNetClient()
                      .connect(upstreamPort, upstreamHost)
                      .onSuccess(
                          upstream -> {
                            connections.add(upstream);
                            upstream.closeHandler(ignored -> connections.remove(upstream));
                            bridgeClient(state, client, upstream);
                            bridgeServer(state, upstream, client);
                            client.resume();
                          })
                      .onFailure(ignored -> client.close());
                })
            .listen(0, "127.0.0.1")
            .toCompletionStage()
            .toCompletableFuture()
            .orTimeout(WAIT.toMillis(), TimeUnit.MILLISECONDS)
            .join();
    return new PostgreSqlCommitFaultProxy(vertx, server, state, connections);
  }

  public int port() {
    return server.actualPort();
  }

  public void interruptNextCommitAcknowledgement() {
    state.commitObserved.set(false);
    state.interruptNextCommit.set(true);
  }

  public boolean commitWasObserved() {
    return state.commitObserved.get();
  }

  @Override
  public void close() {
    connections.forEach(NetSocket::close);
    vertx
        .close()
        .toCompletionStage()
        .toCompletableFuture()
        .orTimeout(WAIT.toMillis(), TimeUnit.MILLISECONDS)
        .join();
  }

  private static void bridgeClient(CommitFaultState state, NetSocket client, NetSocket upstream) {
    client.handler(
        data -> {
          if (containsCommit(data)) {
            state.commitObserved.set(true);
          }
          upstream.write(data);
        });
    client.closeHandler(ignored -> upstream.close());
    client.exceptionHandler(ignored -> upstream.close());
  }

  private static void bridgeServer(CommitFaultState state, NetSocket upstream, NetSocket client) {
    upstream.handler(
        data -> {
          if (state.commitObserved.get() && state.interruptNextCommit.compareAndSet(true, false)) {
            client.close();
            upstream.close();
          } else {
            client.write(data);
          }
        });
    upstream.closeHandler(ignored -> client.close());
    upstream.exceptionHandler(ignored -> client.close());
  }

  private static boolean containsCommit(Buffer data) {
    return data.toString(java.nio.charset.StandardCharsets.UTF_8)
        .toUpperCase(Locale.ROOT)
        .contains("COMMIT");
  }

  private static final class CommitFaultState {
    private final AtomicBoolean interruptNextCommit = new AtomicBoolean();
    private final AtomicBoolean commitObserved = new AtomicBoolean();
  }
}
