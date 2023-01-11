package com.commercetools.sync.sdk2.services.impl;

import com.commercetools.api.models.DomainResource;
import com.commercetools.api.models.PagedQueryResourceRequest;
import com.commercetools.api.models.WithKey;
import com.commercetools.sync.sdk2.commons.BaseSyncOptions;
import io.vrap.rmf.base.client.ApiMethod;
import io.vrap.rmf.base.client.BodyApiMethod;
import io.vrap.rmf.base.client.Draft;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

abstract class BaseServiceWithKey<
        SyncOptionsT extends BaseSyncOptions,
        ResourceT extends DomainResource<ResourceT> & WithKey,
        ResourceDraftT extends Draft<ResourceDraftT>,
        PagedQueryT extends PagedQueryResourceRequest,
        GetOneResourceQueryT extends ApiMethod<GetOneResourceQueryT, ResourceT>,
        QueryResultT,
        PostRequestT extends BodyApiMethod<PostRequestT, QueryResultT, ResourceDraftT>>
    extends BaseService<
        SyncOptionsT,
        ResourceT,
        ResourceDraftT,
        PagedQueryT,
        GetOneResourceQueryT,
        QueryResultT,
        PostRequestT> {

  BaseServiceWithKey(@Nonnull final SyncOptionsT syncOptions) {
    super(syncOptions);
  }

  /**
   * Given a resource draft of type {@code T}, this method attempts to create a resource {@code U}
   * based on it in the CTP project defined by the sync options.
   *
   * <p>A completion stage containing an empty option and the error callback will be triggered in
   * those cases:
   *
   * <ul>
   *   <li>the draft has a blank key
   *   <li>the create request fails on CTP
   * </ul>
   *
   * <p>On the other hand, if the resource gets created successfully on CTP, then the created
   * resource's id and key are cached and the method returns a {@link
   * java.util.concurrent.CompletionStage} in which the result of it's completion contains an
   * instance {@link java.util.Optional} of the resource which was created.
   *
   * @param draft the resource draft to create a resource based off of.
   * @param createCommand a function to get the create command using the supplied draft.
   * @return a {@link java.util.concurrent.CompletionStage} containing an optional with the created
   *     resource if successful otherwise an empty optional.
   */
  @Nonnull
  CompletionStage<Optional<ResourceT>> createResource(
      @Nonnull final ResourceDraftT draft,
      @Nonnull final Function<QueryResultT, String> idMapper,
      @Nonnull final Function<QueryResultT, ResourceT> resourceMapper,
      @Nonnull final PostRequestT createCommand) {

    return super.createResource(
        draft, resourceDraftT -> resourceDraftT.getKey(), idMapper, resourceMapper, createCommand);
  }

  @Nonnull
  CompletionStage<Optional<String>> fetchCachedResourceId(
      @Nullable final String key, @Nonnull final PagedQueryT query) {

    // Why method reference is not used:
    // http://mail.openjdk.java.net/pipermail/compiler-dev/2015-November/009824.html
    return super.fetchCachedResourceId(key, resource -> resource.getKey(), query);
  }

  @Nonnull
  CompletionStage<Set<ResourceT>> fetchMatchingResources(
      @Nonnull final Set<String> keys,
      @Nonnull final Function<Set<String>, PagedQueryT> keysQueryMapper) {

    // Why method reference is not used:
    // http://mail.openjdk.java.net/pipermail/compiler-dev/2015-November/009824.html
    return super.fetchMatchingResources(keys, resource -> resource.getKey(), keysQueryMapper);
  }
}
