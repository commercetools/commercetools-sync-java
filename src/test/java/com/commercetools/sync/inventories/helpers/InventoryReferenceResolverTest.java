package com.commercetools.sync.inventories.helpers;


import com.commercetools.sync.commons.exceptions.ReferenceResolutionException;
import com.commercetools.sync.inventories.InventorySyncMockUtils;
import com.commercetools.sync.inventories.InventorySyncOptions;
import com.commercetools.sync.inventories.InventorySyncOptionsBuilder;
import com.commercetools.sync.services.ChannelService;
import com.commercetools.sync.services.TypeService;
import io.sphere.sdk.channels.Channel;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.inventory.InventoryEntryDraft;
import io.sphere.sdk.inventory.InventoryEntryDraftBuilder;
import io.sphere.sdk.models.Reference;
import io.sphere.sdk.models.SphereException;
import io.sphere.sdk.types.CustomFieldsDraft;
import io.sphere.sdk.utils.CompletableFutureUtils;
import org.junit.Before;
import org.junit.Test;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import static com.commercetools.sync.commons.MockUtils.getMockTypeService;
import static com.commercetools.sync.commons.helpers.BaseReferenceResolver.BLANK_ID_VALUE_ON_RESOURCE_IDENTIFIER;
import static com.commercetools.sync.inventories.InventorySyncMockUtils.getMockSupplyChannel;
import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class InventoryReferenceResolverTest {
    private TypeService typeService;
    private ChannelService channelService;
    private InventorySyncOptions syncOptions;

    private static final String SKU = "1000";
    private static final String CHANNEL_KEY = "channel-key_1";
    private static final String UUID_KEY = UUID.randomUUID().toString();
    private static final String CUSTOM_TYPE_KEY = "customType-key_1";
    private static final String CHANNEL_ID = "1";
    private static final Long QUANTITY = 10L;
    private static final Integer RESTOCKABLE_IN_DAYS = 10;
    private static final ZonedDateTime DATE_1 = ZonedDateTime.of(2017, 4, 1, 10, 0, 0, 0, ZoneId.of("UTC"));

    /**
     * Sets up the services and the options needed for reference resolution.
     */
    @Before
    public void setup() {
        typeService = getMockTypeService();
        channelService = InventorySyncMockUtils.getMockChannelService(getMockSupplyChannel(CHANNEL_ID, CHANNEL_KEY));
        syncOptions = InventorySyncOptionsBuilder.of(mock(SphereClient.class)).build();
    }

    @Test
    public void
        resolveSupplyChannelReference_WithNonExistingChannelAndNotEnsureChannel_ShouldNotResolveChannelReference() {
        when(channelService.fetchCachedChannelId(anyString()))
            .thenReturn(CompletableFuture.completedFuture(Optional.empty()));

        final InventoryEntryDraft draft = InventoryEntryDraft
            .of(SKU, QUANTITY, DATE_1, RESTOCKABLE_IN_DAYS, Channel.referenceOfId(CHANNEL_KEY))
            .withCustom(CustomFieldsDraft.ofTypeIdAndJson(CUSTOM_TYPE_KEY, new HashMap<>()));

        final InventoryReferenceResolver referenceResolver =
            new InventoryReferenceResolver(syncOptions, typeService, channelService);

        referenceResolver.resolveSupplyChannelReference(InventoryEntryDraftBuilder.of(draft))
                         .exceptionally(exception -> {
                             assertThat(exception).isExactlyInstanceOf(ReferenceResolutionException.class);
                             assertThat(exception.getCause())
                                 .isExactlyInstanceOf(CompletionException.class);
                             assertThat(exception.getCause().getCause())
                                 .isExactlyInstanceOf(ReferenceResolutionException.class);
                             assertThat(exception.getCause().getCause().getMessage())
                                 .isEqualTo("Channel with key 'channel-key_1' does not exist.");
                             return null;
                         }).toCompletableFuture().join();
    }

    @Test
    public void
        resolveSupplyChannelReference_WithNonExistingChannelAndEnsureChannel_ShouldResolveSupplyChannelReference() {
        final InventorySyncOptions optionsWithEnsureChannels = InventorySyncOptionsBuilder.of(mock(SphereClient.class))
                                                                                          .ensureChannels(true)
                                                                                          .build();
        when(channelService.fetchCachedChannelId(anyString()))
            .thenReturn(CompletableFuture.completedFuture(Optional.empty()));

        final InventoryEntryDraft draft = InventoryEntryDraft
            .of(SKU, QUANTITY, DATE_1, RESTOCKABLE_IN_DAYS, Channel.referenceOfId(CHANNEL_KEY))
            .withCustom(CustomFieldsDraft.ofTypeIdAndJson(CUSTOM_TYPE_KEY, new HashMap<>()));

        final InventoryReferenceResolver referenceResolver =
            new InventoryReferenceResolver(optionsWithEnsureChannels, typeService, channelService);

        referenceResolver.resolveSupplyChannelReference(InventoryEntryDraftBuilder.of(draft))
                         .thenApply(InventoryEntryDraftBuilder::build)
                         .thenAccept(resolvedDraft -> {
                             assertThat(resolvedDraft.getSupplyChannel()).isNotNull();
                             assertThat(resolvedDraft.getSupplyChannel().getId()).isEqualTo(CHANNEL_ID);
                         }).toCompletableFuture().join();
    }

    @Test
    public void resolveCustomTypeReference_WithExceptionOnCustomTypeFetch_ShouldNotResolveReferences() {
        //todo: check this
        final InventoryEntryDraftBuilder draftBuilder = InventoryEntryDraftBuilder
            .of(SKU, QUANTITY, DATE_1, RESTOCKABLE_IN_DAYS, Channel.referenceOfId(UUID_KEY))
            .custom(CustomFieldsDraft.ofTypeIdAndJson(CUSTOM_TYPE_KEY, new HashMap<>()));

        when(typeService.fetchCachedTypeId(anyString()))
            .thenReturn(CompletableFutureUtils.failed(new SphereException("bad request")));

        final InventoryReferenceResolver referenceResolver =
            new InventoryReferenceResolver(syncOptions, typeService, channelService);

        referenceResolver.resolveCustomTypeReference(draftBuilder)
                         .exceptionally(exception -> {
                             assertThat(exception).isExactlyInstanceOf(CompletionException.class);
                             assertThat(exception.getCause()).isExactlyInstanceOf(SphereException.class);
                             assertThat(exception.getCause().getMessage()).contains("bad request");
                             return null;
                         }).toCompletableFuture().join();
    }

    @Test
    public void resolveCustomTypeReference_WithNonExistentCustomType_ShouldNotResolveCustomTypeReference() {
        when(typeService.fetchCachedTypeId(anyString()))
            .thenReturn(CompletableFuture.completedFuture(Optional.empty()));

        final InventoryEntryDraftBuilder draftBuilder = InventoryEntryDraftBuilder
            .of(SKU, QUANTITY, DATE_1, RESTOCKABLE_IN_DAYS, Channel.referenceOfId(CHANNEL_KEY))
            .custom(CustomFieldsDraft.ofTypeIdAndJson(CUSTOM_TYPE_KEY, new HashMap<>()));

        final InventoryReferenceResolver referenceResolver =
            new InventoryReferenceResolver(syncOptions, typeService, channelService);

        referenceResolver.resolveCustomTypeReference(draftBuilder)
                         .thenApply(InventoryEntryDraftBuilder::build)
                         .thenAccept(resolvedDraft -> {
                             assertThat(resolvedDraft.getCustom()).isNotNull();
                             assertThat(resolvedDraft.getCustom().getType()).isNotNull();
                             assertThat(resolvedDraft.getCustom().getType().getId()).isEqualTo(CUSTOM_TYPE_KEY);
                         }).toCompletableFuture().join();
    }

    @Test
    public void resolveSupplyChannelReference_WithEmptyIdOnSupplyChannelReference_ShouldNotResolveChannelReference() {
        final InventoryEntryDraft draft = InventoryEntryDraft
            .of(SKU, QUANTITY, DATE_1, RESTOCKABLE_IN_DAYS, Channel.referenceOfId(""))
            .withCustom(CustomFieldsDraft.ofTypeIdAndJson(CUSTOM_TYPE_KEY, new HashMap<>()));

        final InventoryReferenceResolver referenceResolver =
            new InventoryReferenceResolver(syncOptions, typeService, channelService);

        referenceResolver.resolveSupplyChannelReference(InventoryEntryDraftBuilder.of(draft))
                         .exceptionally(exception -> {
                             assertThat(exception).isExactlyInstanceOf(ReferenceResolutionException.class);
                             assertThat(exception.getMessage())
                                 .isEqualTo(format("Failed to resolve supply channel reference on InventoryEntryDraft"
                                     + " with SKU:'1000'. Reason: %s", BLANK_ID_VALUE_ON_RESOURCE_IDENTIFIER));
                             return null;
                         }).toCompletableFuture().join();
    }

    @Test
    public void resolveSupplyChannelReference_WithNullIdOnChannelReference_ShouldNotResolveSupplyChannelReference() {
        final InventoryEntryDraft draft = mock(InventoryEntryDraft.class);
        final Reference<Channel> supplyChannelReference = Channel.referenceOfId(null);
        when(draft.getSupplyChannel()).thenReturn(supplyChannelReference);

        final InventoryReferenceResolver referenceResolver =
            new InventoryReferenceResolver(syncOptions, typeService, channelService);

        referenceResolver.resolveSupplyChannelReference(InventoryEntryDraftBuilder.of(draft))
                         .exceptionally(exception -> {
                             assertThat(exception).isExactlyInstanceOf(ReferenceResolutionException.class);
                             assertThat(exception.getMessage())
                                 .isEqualTo(format("Failed to resolve supply channel reference on InventoryEntryDraft"
                                     + " with SKU:'null'. Reason: %s", BLANK_ID_VALUE_ON_RESOURCE_IDENTIFIER));
                             return null;
                         }).toCompletableFuture().join();
    }

    @Test
    public void resolveCustomTypeReference_WithEmptyIdOnCustomTypeReference_ShouldNotResolveCustomTypeReference() {
        final InventoryEntryDraftBuilder draftBuilder = InventoryEntryDraftBuilder
            .of(SKU, QUANTITY, DATE_1, RESTOCKABLE_IN_DAYS, Channel.referenceOfId(CHANNEL_KEY))
            .custom(CustomFieldsDraft.ofTypeIdAndJson("", new HashMap<>()));

        final InventoryReferenceResolver referenceResolver =
            new InventoryReferenceResolver(syncOptions, typeService, channelService);

        referenceResolver.resolveCustomTypeReference(draftBuilder)
                         .exceptionally(exception -> {
                             assertThat(exception).isExactlyInstanceOf(CompletionException.class);
                             assertThat(exception.getCause())
                                 .isExactlyInstanceOf(ReferenceResolutionException.class);
                             assertThat(exception.getCause().getMessage())
                                 .isEqualTo(format("Failed to resolve custom type reference on InventoryEntryDraft"
                                     + " with SKU:'1000'. Reason: %s", BLANK_ID_VALUE_ON_RESOURCE_IDENTIFIER));
                             return null;
                         }).toCompletableFuture().join();
    }
}
