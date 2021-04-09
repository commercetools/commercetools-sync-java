package com.commercetools.sync.services.impl;

import com.commercetools.sync.customobjects.CustomObjectSync;
import com.commercetools.sync.customobjects.CustomObjectSyncOptions;
import com.commercetools.sync.customobjects.helpers.CustomObjectCompositeIdentifier;
import com.commercetools.sync.services.CustomObjectService;
import com.fasterxml.jackson.databind.JsonNode;
import io.sphere.sdk.commands.DraftBasedCreateCommand;
import io.sphere.sdk.customobjects.CustomObject;
import io.sphere.sdk.customobjects.CustomObjectDraft;
import io.sphere.sdk.customobjects.commands.CustomObjectUpsertCommand;
import io.sphere.sdk.customobjects.expansion.CustomObjectExpansionModel;
import io.sphere.sdk.customobjects.queries.CustomObjectQuery;
import io.sphere.sdk.customobjects.queries.CustomObjectQueryBuilder;
import io.sphere.sdk.customobjects.queries.CustomObjectQueryModel;
import io.sphere.sdk.queries.QueryPredicate;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;

/** Implementation of CustomObjectService interface. */
public class CustomObjectServiceImpl
    extends BaseService<
        CustomObjectDraft<JsonNode>,
        CustomObject<JsonNode>,
        CustomObject<JsonNode>,
        CustomObjectSyncOptions,
        CustomObjectQuery<JsonNode>,
        CustomObjectQueryModel<CustomObject<JsonNode>>,
        CustomObjectExpansionModel<CustomObject<JsonNode>>>
    implements CustomObjectService {

  public CustomObjectServiceImpl(@Nonnull final CustomObjectSyncOptions syncOptions) {
    super(syncOptions);
  }

  @Nonnull
  @Override
  public CompletionStage<Map<String, String>> cacheKeysToIds(
      @Nonnull final Set<CustomObjectCompositeIdentifier> identifiers) {

    /*
     * one example representation of the cache:
     *
     * [
     *  "container_1|key_2" : "7fcd15ca-666e-4639-b25a-0c9f76a66efb"
     *  "container_2|key_1" : "ad54192c-86cd-4453-a139-85829e2dd891"
     *  "container_1|key_1" : "33213df2-c09a-426d-8c28-ccc52fdf9744"
     * ]
     */
    return cacheKeysToIds(
        getKeys(identifiers),
        this::keyMapper,
        keysNotCached -> queryIdentifiers(getIdentifiers(keysNotCached)));
  }

  @Nonnull
  @Override
  public CompletionStage<Optional<String>> fetchCachedCustomObjectId(
      @Nonnull final CustomObjectCompositeIdentifier identifier) {

    return fetchCachedResourceId(
        identifier.toString(), this::keyMapper, () -> queryOneIdentifier(identifier));
  }

  @Nonnull
  @Override
  public CompletionStage<Set<CustomObject<JsonNode>>> fetchMatchingCustomObjects(
      @Nonnull final Set<CustomObjectCompositeIdentifier> identifiers) {

    return fetchMatchingResources(
        getKeys(identifiers), this::keyMapper, (keysNotCached) -> queryIdentifiers(identifiers));
  }

  @Nonnull
  private QueryPredicate<CustomObject<JsonNode>> createQuery(
      @Nonnull final CustomObjectQueryModel<CustomObject<JsonNode>> queryModel,
      @Nonnull final Set<CustomObjectCompositeIdentifier> identifiers) {

    QueryPredicate<CustomObject<JsonNode>> queryPredicate = QueryPredicate.of(null);
    boolean firstAttempt = true;
    for (CustomObjectCompositeIdentifier identifier : identifiers) {
      String key = identifier.getKey();
      String container = identifier.getContainer();
      if (firstAttempt) {
        queryPredicate = queryModel.container().is(container).and(queryModel.key().is(key));
        firstAttempt = false;
      } else {
        queryPredicate =
            queryPredicate.or(queryModel.container().is(container).and(queryModel.key().is(key)));
      }
    }
    return queryPredicate;
  }

  @Nonnull
  @Override
  public CompletionStage<Optional<CustomObject<JsonNode>>> fetchCustomObject(
      @Nonnull final CustomObjectCompositeIdentifier identifier) {

    return fetchResource(identifier.toString(), () -> queryOneIdentifier(identifier));
  }

  @Nonnull
  @Override
  public CompletionStage<Optional<CustomObject<JsonNode>>> upsertCustomObject(
      @Nonnull final CustomObjectDraft<JsonNode> customObjectDraft) {

    return createResource(customObjectDraft, this::keyMapper, CustomObjectUpsertCommand::of);
  }

  /**
   * Custom object has special behaviour that it only performs upsert operation. That means both
   * update and create custom object operations in the end called {@link
   * BaseService#createResource}, which is different from other resources.
   *
   * <p>This method provides a specific exception handling after execution of create command for
   * custom objects. Any exception that occurs inside executeCreateCommand method is thrown to the
   * caller method in {@link CustomObjectSync}, which is necessary to trigger retry on error
   * behaviour.
   *
   * @param draft the custom object draft to create a custom object in target CTP project.
   * @param keyMapper a function to get the key from the supplied custom object draft.
   * @param createCommand a function to get the create command using the supplied custom object
   *     draft.
   * @return a {@link CompletionStage} containing an optional with the created resource if
   *     successful otherwise an exception.
   */
  @Nonnull
  @Override
  CompletionStage<Optional<CustomObject<JsonNode>>> executeCreateCommand(
      @Nonnull final CustomObjectDraft<JsonNode> draft,
      @Nonnull final Function<CustomObjectDraft<JsonNode>, String> keyMapper,
      @Nonnull
          final Function<
                  CustomObjectDraft<JsonNode>,
                  DraftBasedCreateCommand<CustomObject<JsonNode>, CustomObjectDraft<JsonNode>>>
              createCommand) {

    final String draftKey = keyMapper.apply(draft);

    return syncOptions
        .getCtpClient()
        .execute(createCommand.apply(draft))
        .thenApply(
            resource -> {
              keyToIdCache.put(draftKey, resource.getId());
              return Optional.of(resource);
            });
  }

  @Nonnull
  private Set<String> getKeys(@Nonnull final Set<CustomObjectCompositeIdentifier> identifiers) {
    return identifiers.stream()
        .map(CustomObjectCompositeIdentifier::toString)
        .collect(Collectors.toSet());
  }

  @Nonnull
  private String keyMapper(@Nonnull final CustomObjectDraft<JsonNode> customObjectDraft) {
    return CustomObjectCompositeIdentifier.of(customObjectDraft).toString();
  }

  @Nonnull
  private String keyMapper(@Nonnull final CustomObject<JsonNode> customObject) {
    return CustomObjectCompositeIdentifier.of(customObject).toString();
  }

  @Nonnull
  private Set<CustomObjectCompositeIdentifier> getIdentifiers(@Nonnull final Set<String> keys) {
    return keys.stream().map(CustomObjectCompositeIdentifier::of).collect(Collectors.toSet());
  }

  @Nonnull
  private CustomObjectQuery<JsonNode> queryIdentifiers(
      @Nonnull final Set<CustomObjectCompositeIdentifier> identifiers) {

    return CustomObjectQueryBuilder.ofJsonNode()
        .plusPredicates(q -> createQuery(q, identifiers))
        .build();
  }

  @Nonnull
  private CustomObjectQuery<JsonNode> queryOneIdentifier(
      @Nonnull final CustomObjectCompositeIdentifier identifier) {

    return CustomObjectQueryBuilder.ofJsonNode()
        .plusPredicates(
            q -> q.container().is(identifier.getContainer()).and(q.key().is(identifier.getKey())))
        .build();
  }
}
