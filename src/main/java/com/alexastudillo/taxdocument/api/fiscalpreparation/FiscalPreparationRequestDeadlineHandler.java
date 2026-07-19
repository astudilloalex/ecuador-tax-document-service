package com.alexastudillo.taxdocument.api.fiscalpreparation;

import com.alexastudillo.taxdocument.application.fiscalpreparation.FiscalPreparationApplicationException;
import com.alexastudillo.taxdocument.application.fiscalpreparation.FiscalPreparationCommitTracker;
import com.alexastudillo.taxdocument.application.fiscalpreparation.FiscalPreparationFailure;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.Duration;
import java.util.Objects;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.NullMarked;

/** Races one application outcome against the already-fixed request deadline. */
@ApplicationScoped
@NullMarked
public final class FiscalPreparationRequestDeadlineHandler {
  public <T extends @NonNull Object> Uni<@NonNull T> race(
      Uni<@NonNull T> application, FiscalPreparationRequestState state) {
    Objects.requireNonNull(application, "application");
    Objects.requireNonNull(state, "state");
    Duration remaining = state.requestContext().deadline().remaining();
    if (remaining.isZero() || remaining.isNegative()) {
      state.acceptTerminal();
      return Objects.requireNonNull(
          Uni.createFrom().failure(timeoutFailure(state.commitTracker())));
    }
    Uni<@NonNull T> timeout =
        Objects.requireNonNull(
            Uni.createFrom()
                .voidItem()
                .onItem()
                .delayIt()
                .by(remaining)
                .onItem()
                .transformToUni(
                    _ -> Uni.createFrom().failure(timeoutFailure(state.commitTracker()))));
    return Objects.requireNonNull(
        Uni.join()
            .first(application, timeout)
            .toTerminate()
            .onItemOrFailure()
            .transformToUni(
                (item, failure) -> {
                  if (!state.acceptTerminal()) {
                    return Uni.createFrom().nothing();
                  }
                  if (failure != null) {
                    return Uni.createFrom().failure(failure);
                  }
                  return Uni.createFrom().item(item);
                }));
  }

  private static FiscalPreparationApplicationException timeoutFailure(
      FiscalPreparationCommitTracker tracker) {
    FiscalPreparationFailure.CommitKnowledge knowledge =
        switch (tracker.knowledge()) {
          case NOT_STARTED -> FiscalPreparationFailure.CommitKnowledge.NOT_STARTED;
          case CONFIRMED_ROLLBACK -> FiscalPreparationFailure.CommitKnowledge.CONFIRMED_ROLLBACK;
          case POSSIBLE_COMMIT, COMMITTED ->
              FiscalPreparationFailure.CommitKnowledge.POSSIBLE_COMMIT;
        };
    return new FiscalPreparationApplicationException(
        FiscalPreparationFailure.of(FiscalPreparationFailure.Code.REQUEST_TIMEOUT)
            .withCommitKnowledge(knowledge));
  }
}
