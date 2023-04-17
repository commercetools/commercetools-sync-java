package com.commercetools.sync.sdk2.services.impl;

import com.commercetools.api.client.ByProjectKeyCustomObjectsByContainerByKeyGet;
import com.commercetools.api.client.ByProjectKeyCustomObjectsGet;
import com.commercetools.api.client.ByProjectKeyCustomObjectsPost;
import com.commercetools.api.models.custom_object.CustomObject;
import com.commercetools.api.models.custom_object.CustomObjectDraft;
import com.commercetools.api.models.custom_object.CustomObjectPagedQueryResponse;
import com.commercetools.sync.sdk2.commons.models.GraphQlQueryResource;
import com.commercetools.sync.sdk2.customobjects.CustomObjectSyncOptions;
import com.commercetools.sync.sdk2.customobjects.helpers.CustomObjectCompositeIdentifier;
import com.commercetools.sync.sdk2.services.CustomObjectService;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.jetbrains.annotations.NotNull;

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
        ByProjectKeyCustomObjectsPost>
    implements CustomObjectService {

  public CustomObjectServiceImpl(@Nonnull final CustomObjectSyncOptions syncOptions) {
    super(syncOptions);
  }

  @NotNull
  @Override
  public CompletionStage<Map<String, String>> cacheKeysToIds(
      @NotNull final Set<CustomObjectCompositeIdentifier> identifiers) {
    /*
     * one example representation of the cache:
     *
     * [
     *  "container_1|key_2" : "7fcd15ca-666e-4639-b25a-0c9f76a66efb"
     *  "container_2|key_1" : "ad54192c-86cd-4453-a139-85829e2dd891"
     *  "container_1|key_1" : "33213df2-c09a-426d-8c28-ccc52fdf9744"
     * ]
     */
    return super.cacheKeysToIdsUsingGraphQl(
        getKeys(identifiers), GraphQlQueryResource.CUSTOM_OBJECTS);
  }

  @NotNull
  @Override
  public CompletionStage<Optional<String>> fetchCachedCustomObjectId(
      @NotNull final CustomObjectCompositeIdentifier identifier) {
    return super.fetchCachedResourceId(
        identifier.toString(), this::keyMapper, queryOneIdentifier(identifier));
  }

  @NotNull
  @Override
  public CompletionStage<Set<CustomObject>> fetchMatchingCustomObjects(
      @NotNull final Set<CustomObjectCompositeIdentifier> identifiers) {
    return super.fetchMatchingResources(
        getKeys(identifiers), this::keyMapper, (keysNotCached) -> createQuery(identifiers));
  }

  @NotNull
  @Override
  public CompletionStage<Optional<CustomObject>> fetchCustomObject(
      @NotNull final CustomObjectCompositeIdentifier identifier) {
    final ByProjectKeyCustomObjectsByContainerByKeyGet query =
        this.syncOptions
            .getCtpClient()
            .customObjects()
            .withContainerAndKey(identifier.getContainer(), identifier.getKey())
            .get();

    return super.fetchResource(identifier.toString(), query);
  }

  @NotNull
  @Override
  public CompletionStage<Optional<CustomObject>> upsertCustomObject(
      @NotNull final CustomObjectDraft customObjectDraft) {
    return super.createResource(
        customObjectDraft,
        this::keyMapper,
        CustomObject::getId,
        customObject -> customObject,
        () -> this.syncOptions.getCtpClient().customObjects().post(customObjectDraft));
  }

  @Nonnull
  private ByProjectKeyCustomObjectsGet createQuery(
      @Nonnull final Set<CustomObjectCompositeIdentifier> identifiers) {
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
