package com.commercetools.sync.products.helpers;

import static io.vrap.rmf.base.client.utils.CompletableFutureUtils.exceptionallyCompletedFuture;
import static java.lang.String.format;
import static java.util.concurrent.CompletableFuture.completedFuture;

import com.commercetools.api.models.channel.Channel;
import com.commercetools.api.models.channel.ChannelReference;
import com.commercetools.api.models.channel.ChannelResourceIdentifier;
import com.commercetools.api.models.channel.ChannelResourceIdentifierBuilder;
import com.commercetools.api.models.common.PriceDraft;
import com.commercetools.api.models.common.PriceDraftBuilder;
import com.commercetools.api.models.customer_group.CustomerGroup;
import com.commercetools.api.models.customer_group.CustomerGroupResourceIdentifier;
import com.commercetools.api.models.customer_group.CustomerGroupResourceIdentifierBuilder;
import com.commercetools.sync.commons.exceptions.ReferenceResolutionException;
import com.commercetools.sync.commons.helpers.CustomReferenceResolver;
import com.commercetools.sync.products.ProductSyncOptions;
import com.commercetools.sync.services.ChannelService;
import com.commercetools.sync.services.CustomerGroupService;
import com.commercetools.sync.services.TypeService;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import javax.annotation.Nonnull;

public final class PriceReferenceResolver
    extends CustomReferenceResolver<PriceDraft, PriceDraftBuilder, ProductSyncOptions> {

  private final ChannelService channelService;
  private final CustomerGroupService customerGroupService;
  static final String CHANNEL_DOES_NOT_EXIST = "Channel with key '%s' does not exist.";
  static final String FAILED_TO_RESOLVE_CUSTOM_TYPE =
      "Failed to resolve custom type reference on "
          + "PriceDraft with country:'%s' and value: '%s'.";
  static final String FAILED_TO_RESOLVE_REFERENCE =
      "Failed to resolve '%s' reference on PriceDraft with "
          + "country:'%s' and value: '%s'. Reason: %s";
  static final String CUSTOMER_GROUP_DOES_NOT_EXIST =
      "Customer Group with key '%s' does not exist.";

  /**
   * Takes a {@link com.commercetools.sync.products.ProductSyncOptions} instance, {@link
   * com.commercetools.sync.services.TypeService}, a {@link
   * com.commercetools.sync.services.ChannelService} and a {@link
   * com.commercetools.sync.services.CustomerGroupService} to instantiate a {@link
   * PriceReferenceResolver} instance that could be used to resolve the prices of variant drafts in
   * the CTP project specified in the injected {@link
   * com.commercetools.sync.products.ProductSyncOptions} instance.
   *
   * @param options the container of all the options of the sync process including the CTP project
   *     client and/or configuration and other sync-specific options.
   * @param typeService the service to fetch the custom types for reference resolution.
   * @param channelService the service to fetch the channels for reference resolution.
   * @param customerGroupService the service to fetch the customer groups for reference resolution.
   */
  public PriceReferenceResolver(
      @Nonnull final ProductSyncOptions options,
      @Nonnull final TypeService typeService,
      @Nonnull final ChannelService channelService,
      @Nonnull final CustomerGroupService customerGroupService) {
    super(options, typeService);
    this.channelService = channelService;
    this.customerGroupService = customerGroupService;
  }

  /**
   * Given a {@link PriceDraft} this method attempts to resolve the custom type and channel
   * references to return a {@link java.util.concurrent.CompletionStage} which contains a new
   * instance of the draft with the resolved references.
   *
   * <p>The method then tries to fetch the key of the customer group, optimistically from a cache.
   * If the id is not found in cache nor the CTP project the {@link ReferenceResolutionException}
   * will be thrown.
   *
   * @param priceDraft the priceDraft to resolve it's references.
   * @return a {@link java.util.concurrent.CompletionStage} that contains as a result a new
   *     inventoryEntryDraft instance with resolved references or, in case an error occurs during
   *     reference resolution a {@link ReferenceResolutionException}.
   */
  @Override
  public CompletionStage<PriceDraft> resolveReferences(@Nonnull final PriceDraft priceDraft) {
    return resolveCustomTypeReference(PriceDraftBuilder.of(priceDraft))
        .thenCompose(this::resolveChannelReference)
        .thenCompose(this::resolveCustomerGroupReference)
        .thenApply(PriceDraftBuilder::build);
  }

  @Override
  @Nonnull
  protected CompletionStage<PriceDraftBuilder> resolveCustomTypeReference(
      @Nonnull final PriceDraftBuilder draftBuilder) {

    return resolveCustomTypeReference(
        draftBuilder,
        PriceDraftBuilder::getCustom,
        PriceDraftBuilder::custom,
        format(
            FAILED_TO_RESOLVE_CUSTOM_TYPE,
            draftBuilder.getCountry(),
            draftBuilder.getValue().toMonetaryAmount()));
  }

  /**
   * Given a {@link PriceDraftBuilder} this method attempts to resolve the supply channel reference
   * to return a {@link java.util.concurrent.CompletionStage} which contains the same instance of
   * draft builder with the resolved supply channel reference.
   *
   * <p>The method then tries to fetch the key of the supply channel, optimistically from a cache.
   * If the id is not found in cache nor the CTP project and {@code ensureChannel} option is set to
   * true, a new channel will be created with this key and the role {@code "InventorySupply"}.
   * However, if the {@code ensureChannel} is set to false, the future is completed exceptionally
   * with a {@link ReferenceResolutionException}.
   *
   * @param draftBuilder the PriceDraftBuilder to resolve its channel reference.
   * @return a {@link java.util.concurrent.CompletionStage} that contains as a result a new price
   *     draft builder instance with resolved supply channel or, in case an error occurs during
   *     reference resolution, a {@link ReferenceResolutionException}.
   */
  @Nonnull
  CompletionStage<PriceDraftBuilder> resolveChannelReference(
      @Nonnull final PriceDraftBuilder draftBuilder) {
    final ChannelResourceIdentifier channelResourceIdentifier = draftBuilder.getChannel();
    if (channelResourceIdentifier != null && channelResourceIdentifier.getId() == null) {
      String channelKey;
      try {
        channelKey = getKeyFromResourceIdentifier(channelResourceIdentifier);
      } catch (ReferenceResolutionException referenceResolutionException) {
        return exceptionallyCompletedFuture(
            new ReferenceResolutionException(
                format(
                    FAILED_TO_RESOLVE_REFERENCE,
                    ChannelResourceIdentifier.CHANNEL,
                    draftBuilder.getCountry(),
                    draftBuilder.getValue(),
                    referenceResolutionException.getMessage())));
      }

      return fetchAndResolveChannelReference(draftBuilder, channelKey);
    }
    return completedFuture(draftBuilder);
  }

  @Nonnull
  private CompletionStage<PriceDraftBuilder> fetchAndResolveChannelReference(
      @Nonnull final PriceDraftBuilder draftBuilder, @Nonnull final String channelKey) {

    return channelService
        .fetchCachedChannelId(channelKey)
        .thenCompose(
            resolvedChannelIdOptional ->
                resolvedChannelIdOptional
                    .map(
                        resolvedChannelId ->
                            completedFuture(
                                draftBuilder.channel(
                                    ChannelResourceIdentifierBuilder.of()
                                        .id(resolvedChannelId)
                                        .build())))
                    .orElseGet(
                        () -> {
                          final CompletableFuture<PriceDraftBuilder> result =
                              new CompletableFuture<>();
                          createChannelAndSetReference(channelKey, draftBuilder)
                              .whenComplete(
                                  (draftWithCreatedChannel, exception) -> {
                                    if (exception != null) {
                                      result.completeExceptionally(
                                          new ReferenceResolutionException(
                                              format(
                                                  FAILED_TO_RESOLVE_REFERENCE,
                                                  ChannelReference.CHANNEL,
                                                  draftBuilder.getCountry(),
                                                  draftBuilder.getValue(),
                                                  exception.getMessage()),
                                              exception));
                                    } else {
                                      result.complete(draftWithCreatedChannel);
                                    }
                                  });
                          return result;
                        }));
  }

  /**
   * Helper method that creates a new {@link com.commercetools.api.models.channel.Channel} on the
   * CTP project with the specified {@code channelKey} and of the role {@code "InventorySupply"}.
   * Only if the {@code ensureChannels} options is set to {@code true} on the {@code options}
   * instance of {@code this} class. Then it resolves the supply channel reference on the supplied
   * {@code inventoryEntryDraft} by setting the id of it's supply channel reference with the newly
   * created Channel.
   *
   * <p>If the {@code ensureChannels} options is set to {@code false} on the {@code options}
   * instance of {@code this} class, the future is completed exceptionally with a {@link
   * ReferenceResolutionException}.
   *
   * <p>The method then returns a CompletionStage with a resolved channel reference {@link
   * PriceDraftBuilder} object.
   *
   * @param channelKey the key to create the new channel with.
   * @param draftBuilder the inventory entry draft builder where to resolve it's supply channel
   *     reference to the newly created channel.
   * @return a CompletionStage with the same {@code draftBuilder} instance having resolved channel
   *     reference.
   */
  @Nonnull
  private CompletionStage<PriceDraftBuilder> createChannelAndSetReference(
      @Nonnull final String channelKey, @Nonnull final PriceDraftBuilder draftBuilder) {

    if (options.shouldEnsurePriceChannels()) {
      return channelService
          .createAndCacheChannel(channelKey)
          .thenCompose(
              createdChannelOptional -> {
                if (createdChannelOptional.isPresent()) {
                  final Channel channel = createdChannelOptional.get();
                  return completedFuture(
                      draftBuilder.channel(
                          ChannelResourceIdentifierBuilder.of().id(channel.getId()).build()));
                } else {
                  final ReferenceResolutionException referenceResolutionException =
                      new ReferenceResolutionException(format(CHANNEL_DOES_NOT_EXIST, channelKey));
                  return exceptionallyCompletedFuture(referenceResolutionException);
                }
              });
    } else {
      final ReferenceResolutionException referenceResolutionException =
          new ReferenceResolutionException(format(CHANNEL_DOES_NOT_EXIST, channelKey));
      return exceptionallyCompletedFuture(referenceResolutionException);
    }
  }

  /**
   * Given a {@link PriceDraftBuilder} this method attempts to resolve the customer group resource
   * identifier to return a {@link java.util.concurrent.CompletionStage} which contains the same
   * instance of draft builder with the resolved customer group resource identifier.
   *
   * @param draftBuilder the priceDraftBuilder to resolve its customer group reference.
   * @return a {@link java.util.concurrent.CompletionStage} that contains as a result a new price
   *     draft builder instance with resolved customer group resource identifier or no customer
   *     group resource identifier if the customer group doesn't exist or in case an error occurs
   *     during reference resolution a {@link ReferenceResolutionException}.
   */
  @Nonnull
  CompletionStage<PriceDraftBuilder> resolveCustomerGroupReference(
      @Nonnull final PriceDraftBuilder draftBuilder) {
    final CustomerGroupResourceIdentifier customerGroupResourceIdentifier =
        draftBuilder.getCustomerGroup();
    if (customerGroupResourceIdentifier != null
        && customerGroupResourceIdentifier.getId() == null) {
      String customerGroupKey = "";
      try {
        customerGroupKey = getKeyFromResourceIdentifier(customerGroupResourceIdentifier);
      } catch (ReferenceResolutionException referenceResolutionException) {
        return exceptionallyCompletedFuture(
            new ReferenceResolutionException(
                format(
                    FAILED_TO_RESOLVE_REFERENCE,
                    CustomerGroup.referenceTypeId(),
                    draftBuilder.getCountry(),
                    draftBuilder.getValue(),
                    referenceResolutionException.getMessage())));
      }
      return fetchAndResolveCustomerGroupReference(draftBuilder, customerGroupKey);
    }
    return completedFuture(draftBuilder);
  }

  @Nonnull
  private CompletionStage<PriceDraftBuilder> fetchAndResolveCustomerGroupReference(
      @Nonnull final PriceDraftBuilder draftBuilder, @Nonnull final String customerGroupKey) {

    return customerGroupService
        .fetchCachedCustomerGroupId(customerGroupKey)
        .thenCompose(
            resolvedCustomerGroupIdOptional ->
                resolvedCustomerGroupIdOptional
                    .map(
                        resolvedCustomerGroupId ->
                            completedFuture(
                                draftBuilder.customerGroup(
                                    CustomerGroupResourceIdentifierBuilder.of()
                                        .id(resolvedCustomerGroupId)
                                        .build())))
                    .orElseGet(
                        () -> {
                          final String errorMessage =
                              format(CUSTOMER_GROUP_DOES_NOT_EXIST, customerGroupKey);
                          return exceptionallyCompletedFuture(
                              new ReferenceResolutionException(
                                  format(
                                      FAILED_TO_RESOLVE_REFERENCE,
                                      CustomerGroup.referenceTypeId(),
                                      draftBuilder.getCountry(),
                                      draftBuilder.getValue(),
                                      errorMessage)));
                        }));
  }
}
