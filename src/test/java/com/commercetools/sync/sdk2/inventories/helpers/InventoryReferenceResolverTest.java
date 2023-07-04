package com.commercetools.sync.sdk2.inventories.helpers;

import com.commercetools.api.client.error.BadRequestException;
import com.commercetools.api.models.channel.*;
import com.commercetools.api.models.type.CustomFieldsDraftBuilder;
import com.commercetools.api.models.type.TypeResourceIdentifierBuilder;
import com.commercetools.sync.sdk2.commons.exceptions.ReferenceResolutionException;
import com.commercetools.sync.sdk2.inventories.InventorySyncMockUtils;
import com.commercetools.sync.sdk2.inventories.InventorySyncOptions;
import com.commercetools.sync.sdk2.inventories.InventorySyncOptionsBuilder;
import com.commercetools.sync.sdk2.services.ChannelService;
import com.commercetools.sync.sdk2.services.TypeService;
import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.inventory.InventoryEntryDraft;
import com.commercetools.api.models.inventory.InventoryEntryDraftBuilder;
import com.commercetools.api.models.type.CustomFieldsDraft;
import io.sphere.sdk.models.ResourceIdentifier;
import io.vrap.rmf.base.client.ApiHttpResponse;
import io.vrap.rmf.base.client.utils.CompletableFutureUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import static com.commercetools.sync.sdk2.commons.MockUtils.getMockTypeService;
import static com.commercetools.sync.sdk2.commons.helpers.BaseReferenceResolver.BLANK_KEY_VALUE_ON_RESOURCE_IDENTIFIER;
import static com.commercetools.sync.sdk2.commons.helpers.CustomReferenceResolver.TYPE_DOES_NOT_EXIST;
import static com.commercetools.sync.sdk2.inventories.InventorySyncMockUtils.getMockSupplyChannel;
import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class InventoryReferenceResolverTest {
  private TypeService typeService;
  private ChannelService channelService;
  private InventorySyncOptions syncOptions;

  private static final String SKU = "1000";
  private static final String CHANNEL_KEY = "channel-key_1";
  private static final String UUID_KEY = UUID.randomUUID().toString();
  private static final String CUSTOM_TYPE_KEY = "customType-key_1";
  private static final String CHANNEL_ID = "1";
  private static final Long QUANTITY = 10L;
  private static final Long RESTOCKABLE_IN_DAYS = 10L;
  private static final ZonedDateTime DATE_1 =
      ZonedDateTime.of(2017, 4, 1, 10, 0, 0, 0, ZoneId.of("UTC"));
  private static final CustomFieldsDraft CUSTOM_FIELDS_DRAFT = CustomFieldsDraftBuilder.of().type(TypeResourceIdentifierBuilder.of().key(CUSTOM_TYPE_KEY).build()).build();


    /** Sets up the services and the options needed for reference resolution. */
  @BeforeEach
  void setup() {
    typeService = getMockTypeService();
    channelService =
        InventorySyncMockUtils.getMockChannelService(getMockSupplyChannel(CHANNEL_ID, CHANNEL_KEY));
    syncOptions = InventorySyncOptionsBuilder.of(mock(ProjectApiRoot.class)).build();
  }

  @Test
  void
      resolveSupplyChannelReference_WithNonExistingChannelAndNotEnsureChannel_ShouldNotResolveChannelReference() {
    when(channelService.fetchCachedChannelId(anyString()))
        .thenReturn(CompletableFuture.completedFuture(Optional.empty()));

      final InventoryEntryDraft draft =
            InventoryEntryDraftBuilder.of()
                    .sku(SKU)
                    .quantityOnStock(QUANTITY)
                    .expectedDelivery(DATE_1)
                    .restockableInDays(RESTOCKABLE_IN_DAYS)
                    .supplyChannel(ChannelResourceIdentifierBuilder.of().key(CHANNEL_KEY).build())
                    .custom(CUSTOM_FIELDS_DRAFT)
            .build();

    final InventoryReferenceResolver referenceResolver =
        new InventoryReferenceResolver(syncOptions, typeService, channelService);

    referenceResolver
        .resolveSupplyChannelReference(InventoryEntryDraftBuilder.of(draft))
        .exceptionally(
            exception -> {
              assertThat(exception).isExactlyInstanceOf(ReferenceResolutionException.class);
              assertThat(exception.getCause()).isExactlyInstanceOf(CompletionException.class);
              assertThat(exception.getCause().getCause())
                  .isExactlyInstanceOf(ReferenceResolutionException.class);
              assertThat(exception.getCause().getCause().getMessage())
                  .isEqualTo("Channel with key 'channel-key_1' does not exist.");
              return null;
            })
        .toCompletableFuture()
        .join();
  }

  @Test
  void
      resolveSupplyChannelReference_WithNonExistingChannelAndEnsureChannel_ShouldResolveSupplyChannelReference() {
    final InventorySyncOptions optionsWithEnsureChannels =
        InventorySyncOptionsBuilder.of(mock(ProjectApiRoot.class)).ensureChannels(true).build();
    when(channelService.fetchCachedChannelId(anyString()))
        .thenReturn(CompletableFuture.completedFuture(Optional.empty()));

      final InventoryEntryDraft draft =
              InventoryEntryDraftBuilder.of()
                      .sku(SKU)
                      .quantityOnStock(QUANTITY)
                      .expectedDelivery(DATE_1)
                      .restockableInDays(RESTOCKABLE_IN_DAYS)
                      .supplyChannel(ChannelResourceIdentifierBuilder.of().key(CHANNEL_KEY).build())
                      .custom(CUSTOM_FIELDS_DRAFT)
                      .build();

    final InventoryReferenceResolver referenceResolver =
        new InventoryReferenceResolver(optionsWithEnsureChannels, typeService, channelService);

    referenceResolver
        .resolveSupplyChannelReference(InventoryEntryDraftBuilder.of(draft))
        .thenApply(InventoryEntryDraftBuilder::build)
        .thenAccept(
            resolvedDraft -> {
              assertThat(resolvedDraft.getSupplyChannel()).isNotNull();
              assertThat(resolvedDraft.getSupplyChannel().getId()).isEqualTo(CHANNEL_ID);
            })
        .toCompletableFuture()
        .join();
  }

  @Test
  void resolveCustomTypeReference_WithExceptionOnCustomTypeFetch_ShouldNotResolveReferences() {
      final InventoryEntryDraftBuilder draftBuilder =
              InventoryEntryDraftBuilder.of()
                      .sku(SKU)
                      .quantityOnStock(QUANTITY)
                      .expectedDelivery(DATE_1)
                      .restockableInDays(RESTOCKABLE_IN_DAYS)
                      .supplyChannel(ChannelResourceIdentifierBuilder.of().id(UUID_KEY).build())
                      .custom(CUSTOM_FIELDS_DRAFT);

    when(typeService.fetchCachedTypeId(anyString()))
        .thenReturn(CompletableFutureUtils.failed(new BadRequestException(500, "", null, "bad request", new ApiHttpResponse<>(500, null, null))));

    final InventoryReferenceResolver referenceResolver =
        new InventoryReferenceResolver(syncOptions, typeService, channelService);

    referenceResolver
        .resolveCustomTypeReference(draftBuilder)
        .exceptionally(
            exception -> {
              assertThat(exception).isExactlyInstanceOf(CompletionException.class);
              assertThat(exception.getCause()).isExactlyInstanceOf(BadRequestException.class);
              assertThat(exception.getCause().getMessage()).contains("bad request");
              return null;
            })
        .toCompletableFuture()
        .join();
  }

  @Test
  void resolveCustomTypeReference_WithNonExistentCustomType_ShouldNotResolveCustomTypeReference() {
    when(typeService.fetchCachedTypeId(anyString()))
        .thenReturn(CompletableFuture.completedFuture(Optional.empty()));

    final InventoryEntryDraftBuilder draftBuilder =
              InventoryEntryDraftBuilder.of()
                      .sku(SKU)
                      .quantityOnStock(QUANTITY)
                      .expectedDelivery(DATE_1)
                      .restockableInDays(RESTOCKABLE_IN_DAYS)
                      .supplyChannel(ChannelResourceIdentifierBuilder.of().key(CHANNEL_KEY).build())
                      .custom(CUSTOM_FIELDS_DRAFT);

    final InventoryReferenceResolver referenceResolver =
        new InventoryReferenceResolver(syncOptions, typeService, channelService);

    final String expectedExceptionMessage =
        format(InventoryReferenceResolver.FAILED_TO_RESOLVE_CUSTOM_TYPE, SKU);
    final String expectedMessageWithCause =
        format(
            "%s Reason: %s",
            expectedExceptionMessage, format(TYPE_DOES_NOT_EXIST, CUSTOM_TYPE_KEY));

    referenceResolver
        .resolveCustomTypeReference(draftBuilder)
        .exceptionally(
            exception -> {
              assertThat(exception).hasCauseExactlyInstanceOf(ReferenceResolutionException.class);
              assertThat(exception.getCause().getMessage()).isEqualTo(expectedMessageWithCause);
              return null;
            })
        .toCompletableFuture()
        .join();
  }

  @Test
  void
      resolveSupplyChannelReference_WithEmptyIdOnSupplyChannelReference_ShouldNotResolveChannelReference() {
      final InventoryEntryDraft draft =
              InventoryEntryDraftBuilder.of()
                      .sku(SKU)
                      .quantityOnStock(QUANTITY)
                      .expectedDelivery(DATE_1)
                      .restockableInDays(RESTOCKABLE_IN_DAYS)
                      .supplyChannel(ChannelResourceIdentifierBuilder.of().key("").build())
                      .custom(CUSTOM_FIELDS_DRAFT)
                      .build();

    final InventoryReferenceResolver referenceResolver =
        new InventoryReferenceResolver(syncOptions, typeService, channelService);

    referenceResolver
        .resolveSupplyChannelReference(InventoryEntryDraftBuilder.of(draft))
        .exceptionally(
            exception -> {
              assertThat(exception).isExactlyInstanceOf(ReferenceResolutionException.class);
              assertThat(exception.getMessage())
                  .isEqualTo(
                      format(
                          "Failed to resolve supply channel resource identifier on "
                              + "InventoryEntryDraft with SKU:'1000'. Reason: %s",
                          BLANK_KEY_VALUE_ON_RESOURCE_IDENTIFIER));
              return null;
            })
        .toCompletableFuture()
        .join();
  }

  @Test
  void
      resolveSupplyChannelReference_WithNullIdOnChannelReference_ShouldNotResolveSupplyChannelReference() {
    final InventoryEntryDraft draft = mock(InventoryEntryDraft.class);
    final ChannelResourceIdentifier supplyChannelReference = ChannelResourceIdentifierBuilder.of().id(null).build();
    when(draft.getSupplyChannel()).thenReturn(supplyChannelReference);

    final InventoryReferenceResolver referenceResolver =
        new InventoryReferenceResolver(syncOptions, typeService, channelService);

    referenceResolver
        .resolveSupplyChannelReference(InventoryEntryDraftBuilder.of(draft))
        .exceptionally(
            exception -> {
              assertThat(exception).isExactlyInstanceOf(ReferenceResolutionException.class);
              assertThat(exception.getMessage())
                  .isEqualTo(
                      format(
                          "Failed to resolve supply channel resource identifier on "
                              + "InventoryEntryDraft with SKU:'null'. Reason: %s",
                          BLANK_KEY_VALUE_ON_RESOURCE_IDENTIFIER));
              return null;
            })
        .toCompletableFuture()
        .join();
  }

  @Test
  void
      resolveSupplyChannelReference_WithResolvedSupplyChannelReference_ShouldNotResolveChannelReference() {
      final InventoryEntryDraft draft =
              InventoryEntryDraftBuilder.of()
                      .sku(SKU)
                      .quantityOnStock(QUANTITY)
                      .expectedDelivery(DATE_1)
                      .restockableInDays(RESTOCKABLE_IN_DAYS)
                      .supplyChannel(ChannelResourceIdentifierBuilder.of().id(CHANNEL_ID).build())
                      .build();

    final InventoryReferenceResolver referenceResolver =
        new InventoryReferenceResolver(syncOptions, typeService, channelService);

    referenceResolver
        .resolveSupplyChannelReference(InventoryEntryDraftBuilder.of(draft))
        .thenApply(InventoryEntryDraftBuilder::build)
        .thenAccept(
            resolvedDraft -> {
              assertThat(resolvedDraft.getSupplyChannel()).isNotNull();
              assertThat(resolvedDraft.getSupplyChannel().getId()).isEqualTo(CHANNEL_ID);
            })
        .toCompletableFuture()
        .join();
  }
}
