package com.commercetools.sync.states.helpers;

import static io.vrap.rmf.base.client.utils.CompletableFutureUtils.exceptionallyCompletedFuture;
import static java.lang.String.format;
import static java.util.stream.Collectors.toList;

import com.commercetools.api.models.state.State;
import com.commercetools.api.models.state.StateDraft;
import com.commercetools.api.models.state.StateDraftBuilder;
import com.commercetools.api.models.state.StateReference;
import com.commercetools.api.models.state.StateResourceIdentifier;
import com.commercetools.api.models.state.StateResourceIdentifierBuilder;
import com.commercetools.sync.commons.exceptions.ReferenceResolutionException;
import com.commercetools.sync.commons.helpers.BaseReferenceResolver;
import com.commercetools.sync.services.StateService;
import com.commercetools.sync.states.StateSyncOptions;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import javax.annotation.Nonnull;

public final class StateReferenceResolver
    extends BaseReferenceResolver<StateDraft, StateSyncOptions> {
  private final StateService stateService;
  private static final String FAILED_TO_RESOLVE_REFERENCE =
      "Failed to resolve 'transition' reference on StateDraft with key:'%s'. Reason: %s";

  /**
   * Takes a {@link StateSyncOptions} instance, a {@link StateService} to instantiate a {@link
   * StateReferenceResolver} instance that could be used to resolve the category drafts in the CTP
   * project specified in the injected {@link StateSyncOptions} instance.
   *
   * @param options the container of all the options of the sync process including the CTP project
   *     client and/or configuration and other sync-specific options.
   * @param stateService the service to fetch the states for reference resolution.
   */
  public StateReferenceResolver(
      @Nonnull final StateSyncOptions options, @Nonnull final StateService stateService) {
    super(options);
    this.stateService = stateService;
  }

  /**
   * Given a {@link StateDraft} this method attempts to resolve the transition state references to
   * return a {@link java.util.concurrent.CompletionStage} which contains a new instance of the
   * draft with the resolved references. The keys of the references are taken from the id field of
   * the references.
   *
   * @param stateDraft the stateDraft to resolve its references.
   * @return a {@link java.util.concurrent.CompletionStage} that contains as a result a new
   *     Statedraft instance with resolved category references or, in case an error occurs during
   *     reference resolution, a {@link ReferenceResolutionException}.
   */
  @Override
  @Nonnull
  public CompletionStage<StateDraft> resolveReferences(@Nonnull final StateDraft stateDraft) {
    return resolveTransitionReferences(StateDraftBuilder.of(stateDraft))
        .thenApply(StateDraftBuilder::build);
  }

  /**
   * Given a {@link StateDraftBuilder} this method attempts to resolve the transitions to return a
   * {@link java.util.concurrent.CompletionStage} which contains a new instance of the builder with
   * the resolved references. The key of the state references is taken from the value of the id
   * fields.
   *
   * @param draftBuilder the state to resolve its transition references.
   * @return a {@link java.util.concurrent.CompletionStage} that contains as a result a new builder
   *     instance with resolved references or, in case an error occurs during reference resolution,
   *     a {@link ReferenceResolutionException}.
   */
  @Nonnull
  private CompletionStage<StateDraftBuilder> resolveTransitionReferences(
      @Nonnull final StateDraftBuilder draftBuilder) {

    final List<StateResourceIdentifier> stateReferences = draftBuilder.getTransitions();
    final Set<String> stateKeys = new HashSet<>();
    if (stateReferences != null) {
      for (StateResourceIdentifier stateReference : stateReferences) {
        if (stateReference != null) {
          try {
            final String stateKey = getKeyFromResourceIdentifier(stateReference);
            stateKeys.add(stateKey);
          } catch (ReferenceResolutionException referenceResolutionException) {
            return exceptionallyCompletedFuture(
                new ReferenceResolutionException(
                    format(
                        FAILED_TO_RESOLVE_REFERENCE,
                        draftBuilder.getKey(),
                        referenceResolutionException.getMessage())));
          }
        }
      }
    }
    return fetchAndResolveStateTransitions(draftBuilder, stateKeys);
  }

  /**
   * Given a {@link StateDraftBuilder} and a {@link java.util.Set} of {@code stateKeys} this method
   * fetches the states corresponding to these keys. Then it sets the state references on the {@code
   * draftBuilder}.
   *
   * @param draftBuilder the state draft builder to resolve it's state references.
   * @param stateKeys the state keys of to resolve their actual id on the draft.
   * @return a {@link java.util.concurrent.CompletionStage} that contains as a result a new
   *     stateDraft instance with resolved state resource identifiers or an exception.
   */
  @Nonnull
  private CompletionStage<StateDraftBuilder> fetchAndResolveStateTransitions(
      @Nonnull final StateDraftBuilder draftBuilder, @Nonnull final Set<String> stateKeys) {

    return stateService
        .fetchMatchingStatesByKeysWithTransitions(stateKeys)
        .thenApply(
            states ->
                states.stream()
                    .map(State::toResourceIdentifier)
                    .filter(Objects::nonNull)
                    .collect(toList()))
        .thenApply(
            references -> {
              if (!references.isEmpty()) {
                draftBuilder.transitions(references);
              }
              return draftBuilder;
            });
  }

  @Nonnull
  private static List<StateResourceIdentifier> toStateResourceIdentifiers(
      final Set<StateReference> references) {
    return references.stream()
        .map(
            stateReference ->
                StateResourceIdentifierBuilder.of().id(stateReference.getId()).build())
        .collect(toList());
  }
}
