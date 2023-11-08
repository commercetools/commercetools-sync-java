package com.commercetools.sync.services.impl;

import com.commercetools.api.client.ByProjectKeyCustomObjectsByContainerByKeyGet;
import com.commercetools.api.client.ByProjectKeyCustomObjectsGet;
import com.commercetools.api.client.ByProjectKeyCustomObjectsPost;
import com.commercetools.api.models.custom_object.CustomObject;
import com.commercetools.api.models.custom_object.CustomObjectDraft;
import com.commercetools.api.models.custom_object.CustomObjectPagedQueryResponse;
import com.commercetools.api.predicates.query.custom_object.CustomObjectQueryBuilderDsl;
import com.commercetools.sync.customobjects.CustomObjectSync;
import com.commercetools.sync.customobjects.CustomObjectSyncOptions;
import com.commercetools.sync.customobjects.helpers.CustomObjectCompositeIdentifier;
import com.commercetools.sync.services.CustomObjectService;
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
        CustomObjectSyncOptions,
        CustomObject,
        CustomObjectDraft,
        ByProjectKeyCustomObjectsGet,
        CustomObjectPagedQueryResponse,
        ByProjectKeyCustomObjectsByContainerByKeyGet,
        CustomObject,
        CustomObjectQueryBuilderDsl,
        ByProjectKeyCustomObjectsPost>
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
    return fetchMatchingCustomObjects(identifiers)
        .thenApply(
            chunk -> {
              chunk.forEach(resource -> keyToIdCache.put(keyMapper(resource), resource.getId()));
              return keyToIdCache.asMap();
            });
  }

  @Nonnull
  @Override
  public CompletionStage<Optional<String>> fetchCachedCustomObjectId(
      @Nonnull final CustomObjectCompositeIdentifier identifier) {
    return super.fetchCachedResourceId(
        identifier.toString(), this::keyMapper, queryOneIdentifier(identifier));
  }

  @Nonnull
  @Override
  public CompletionStage<Set<CustomObject>> fetchMatchingCustomObjects(
      @Nonnull final Set<CustomObjectCompositeIdentifier> identifiers) {
    return super.fetchMatchingResources(getKeys(identifiers), this::keyMapper, this::createQuery);
  }

  @Nonnull
  @Override
  public CompletionStage<Optional<CustomObject>> fetchCustomObject(
      @Nonnull final CustomObjectCompositeIdentifier identifier) {
    final ByProjectKeyCustomObjectsByContainerByKeyGet query =
        this.syncOptions
            .getCtpClient()
            .customObjects()
            .withContainerAndKey(identifier.getContainer(), identifier.getKey())
            .get();

    return super.fetchResource(identifier.toString(), query);
  }

  @Nonnull
  @Override
  public CompletionStage<Optional<CustomObject>> upsertCustomObject(
      @Nonnull final CustomObjectDraft customObjectDraft) {
    return super.createResource(
        customObjectDraft,
        this::keyMapper,
        CustomObject::getId,
        customObject -> customObject,
        () -> this.syncOptions.getCtpClient().customObjects().post(customObjectDraft));
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
   * @param key a function to get the key from the supplied custom object draft.
   * @param idMapper
   * @param resourceMapper
   * @param createCommand a function to get the create command using the supplied custom object
   *     draft.
   * @return a {@link CompletionStage} containing an optional with the created resource if
   *     successful otherwise an exception.
   */
  @Nonnull
  @Override
  CompletionStage<Optional<CustomObject>> executeCreateCommand(
      @Nonnull CustomObjectDraft draft,
      @Nonnull String key,
      @Nonnull Function<CustomObject, String> idMapper,
      @Nonnull Function<CustomObject, CustomObject> resourceMapper,
      @Nonnull ByProjectKeyCustomObjectsPost createCommand) {
    return createCommand
        .execute()
        .thenApply(
            resource -> {
              if (resource != null) {
                final CustomObject customObject = resource.getBody();
                keyToIdCache.put(key, idMapper.apply(customObject));
                return Optional.of(resourceMapper.apply(customObject));
              } else {
                return Optional.empty();
              }
            });
  }

  @Nonnull
  private ByProjectKeyCustomObjectsGet createQuery(@Nonnull final Set<String> keys) {
    final Set<CustomObjectCompositeIdentifier> identifiers =
        keys.stream().map(CustomObjectCompositeIdentifier::of).collect(Collectors.toSet());

    final String whereQuery =
        identifiers.stream()
            .map(
                identifier ->
                    "(container=\""
                        + identifier.getContainer()
                        + "\" AND key=\""
                        + identifier.getKey()
                        + "\")")
            .collect(Collectors.joining(" OR "));

    return this.syncOptions.getCtpClient().customObjects().get().withWhere(whereQuery);
  }

  @Nonnull
  private Set<String> getKeys(@Nonnull final Set<CustomObjectCompositeIdentifier> identifiers) {
    return identifiers.stream()
        .map(CustomObjectCompositeIdentifier::toString)
        .collect(Collectors.toSet());
  }

  @Nonnull
  private String keyMapper(@Nonnull final CustomObjectDraft customObjectDraft) {
    return CustomObjectCompositeIdentifier.of(customObjectDraft).toString();
  }

  @Nonnull
  private String keyMapper(@Nonnull final CustomObject customObject) {
    return CustomObjectCompositeIdentifier.of(customObject).toString();
  }

  @Nonnull
  private ByProjectKeyCustomObjectsGet queryOneIdentifier(
      @Nonnull final CustomObjectCompositeIdentifier identifier) {
    return syncOptions
        .getCtpClient()
        .customObjects()
        .get()
        .withWhere("container=:container AND key=:key")
        .withPredicateVar("container", identifier.getContainer())
        .withPredicateVar("key", identifier.getKey());
  }
}
