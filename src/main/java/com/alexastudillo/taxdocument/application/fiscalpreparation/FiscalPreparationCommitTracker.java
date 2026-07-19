package com.alexastudillo.taxdocument.application.fiscalpreparation;

import java.util.concurrent.atomic.AtomicReference;

/** Request-local commit knowledge shared without coupling application or persistence to HTTP. */
public final class FiscalPreparationCommitTracker {
  private final AtomicReference<Knowledge> knowledge = new AtomicReference<>(Knowledge.NOT_STARTED);

  public void possibleCommit() {
    knowledge.compareAndSet(Knowledge.NOT_STARTED, Knowledge.POSSIBLE_COMMIT);
  }

  public void confirmedRollback() {
    knowledge.updateAndGet(
        current ->
            current == Knowledge.COMMITTED ? Knowledge.COMMITTED : Knowledge.CONFIRMED_ROLLBACK);
  }

  public void committed() {
    knowledge.set(Knowledge.COMMITTED);
  }

  public Knowledge knowledge() {
    return knowledge.get();
  }

  public enum Knowledge {
    NOT_STARTED,
    CONFIRMED_ROLLBACK,
    POSSIBLE_COMMIT,
    COMMITTED
  }
}
