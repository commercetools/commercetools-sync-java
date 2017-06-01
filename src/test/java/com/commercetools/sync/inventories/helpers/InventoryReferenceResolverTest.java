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
import io.sphere.sdk.models.Reference;
import io.sphere.sdk.models.SphereException;
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

import static com.commercetools.sync.commons.MockUtils.getMockCustomFieldsDraft;
import static com.commercetools.sync.commons.MockUtils.getMockTypeService;
import static com.commercetools.sync.inventories.InventorySyncMockUtils.getMockSupplyChannel;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
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
        channelService = InventorySyncMockUtils
            .getMockChannelService(getMockSupplyChannel(CHANNEL_ID, CHANNEL_KEY), CHANNEL_ID);
        syncOptions = InventorySyncOptionsBuilder.of(mock(SphereClient.class)).build();
    }

    @Test
    public void resolveReferences_WithNoKeysAsUuidSetAndNotAllowed_ShouldResolveReferences() {
        final InventoryEntryDraft draft = InventoryEntryDraft
            .of(SKU, QUANTITY, DATE_1, RESTOCKABLE_IN_DAYS, Reference.of(Channel.referenceTypeId(), CHANNEL_KEY))
            .withCustom(getMockCustomFieldsDraft(CUSTOM_TYPE_KEY, new HashMap<>()));

        final InventoryReferenceResolver referenceResolver =
            new InventoryReferenceResolver(syncOptions, typeService, channelService);
        final InventoryEntryDraft draftWithResolvedReferences = referenceResolver
            .resolveReferences(draft).toCompletableFuture().join();

        assertThat(draftWithResolvedReferences.getSupplyChannel()).isNotNull();
        assertThat(draftWithResolvedReferences.getSupplyChannel().getId()).isEqualTo(CHANNEL_ID);

        assertThat(draftWithResolvedReferences.getCustom()).isNotNull();
        assertThat(draftWithResolvedReferences.getCustom().getType().getId()).isEqualTo("typeId");
    }

    @Test
    public void resolveReferences_WithKeysAsUuidSetAndAllowed_ShouldResolveReferences() {
        final InventorySyncOptions optionsWithAllowedUuid = InventorySyncOptionsBuilder.of(mock(SphereClient.class))
                                                                                    .setAllowUuid(true)
                                                                                    .build();
        final InventoryEntryDraft draft = InventoryEntryDraft
            .of(SKU, QUANTITY, DATE_1, RESTOCKABLE_IN_DAYS, Reference.of(Channel.referenceTypeId(), UUID_KEY))
            .withCustom(getMockCustomFieldsDraft(UUID_KEY, new HashMap<>()));

        final InventoryReferenceResolver referenceResolver =
            new InventoryReferenceResolver(optionsWithAllowedUuid, typeService, channelService);
        final InventoryEntryDraft draftWithResolvedReferences = referenceResolver
            .resolveReferences(draft).toCompletableFuture().join();

        assertThat(draftWithResolvedReferences.getSupplyChannel()).isNotNull();
        assertThat(draftWithResolvedReferences.getSupplyChannel().getId()).isEqualTo(CHANNEL_ID);

        assertThat(draftWithResolvedReferences.getCustom()).isNotNull();
        assertThat(draftWithResolvedReferences.getCustom().getType().getId()).isEqualTo("typeId");
    }

    @Test
    public void resolveReferences_WithSupplyChannelKeyAsUuidSetAndNotAllowed_ShouldNotResolveSupplyChannelReference() {
        final InventoryEntryDraft draft = InventoryEntryDraft
            .of(SKU, QUANTITY, DATE_1, RESTOCKABLE_IN_DAYS, Reference.of(Channel.referenceTypeId(), UUID_KEY))
            .withCustom(getMockCustomFieldsDraft(CUSTOM_TYPE_KEY, new HashMap<>()));

        final InventoryReferenceResolver referenceResolver =
            new InventoryReferenceResolver(syncOptions, typeService, channelService);

        referenceResolver.resolveReferences(draft)
                                 .exceptionally(exception -> {
                                     assertThat(exception).isExactlyInstanceOf(CompletionException.class);
                                     assertThat(exception.getCause())
                                         .isExactlyInstanceOf(ReferenceResolutionException.class);
                                     assertThat(exception.getCause().getMessage())
                                         .isEqualTo("Failed to resolve supply channel reference. Reason: Found a UUID"
                                             + " in the id field. Expecting a key without a UUID value. If you want to"
                                             + " allow UUID values for reference keys, please use the"
                                             + " setAllowUuid(true) option in the sync options.");
                                     return null;
                                 }).toCompletableFuture().join();
    }

    @Test
    public void
    resolveReferences_WithNonExistentSupplyChannelAndNotEnsureChannel_ShouldNotResolveSupplyChannelReference() {
        when(channelService.fetchCachedChannelIdByKeyAndRoles(anyString(), any()))
            .thenReturn(CompletableFuture.completedFuture(Optional.empty()));

        final InventoryEntryDraft draft = InventoryEntryDraft
            .of(SKU, QUANTITY, DATE_1, RESTOCKABLE_IN_DAYS, Reference.of(Channel.referenceTypeId(), CHANNEL_KEY))
            .withCustom(getMockCustomFieldsDraft(CUSTOM_TYPE_KEY, new HashMap<>()));

        final InventoryReferenceResolver referenceResolver =
            new InventoryReferenceResolver(syncOptions, typeService, channelService);

        referenceResolver.resolveReferences(draft)
                         .exceptionally(exception -> {
                             assertThat(exception).isExactlyInstanceOf(CompletionException.class);
                             assertThat(exception.getCause())
                                 .isExactlyInstanceOf(ReferenceResolutionException.class);
                             assertThat(exception.getCause().getMessage())
                                 .isEqualTo("Failed to resolve supply channel reference. Reason: Channel with key "
                                     + "'channel-key_1' does not exist.");
                             return null;
                         }).toCompletableFuture().join();
    }

    @Test
    public void
    resolveReferences_WithNonExistentSupplyChannelAndEnsureChannel_ShouldResolveSupplyChannelReference() {
        final InventorySyncOptions optionsWithEnsureChannels = InventorySyncOptionsBuilder.of(mock(SphereClient.class))
                                                                                          .ensureChannels(true)
                                                                                          .build();
        when(channelService.fetchCachedChannelIdByKeyAndRoles(anyString(), any()))
            .thenReturn(CompletableFuture.completedFuture(Optional.empty()));

        final InventoryEntryDraft draft = InventoryEntryDraft
            .of(SKU, QUANTITY, DATE_1, RESTOCKABLE_IN_DAYS, Reference.of(Channel.referenceTypeId(), CHANNEL_KEY))
            .withCustom(getMockCustomFieldsDraft(CUSTOM_TYPE_KEY, new HashMap<>()));

        final InventoryReferenceResolver referenceResolver =
            new InventoryReferenceResolver(optionsWithEnsureChannels, typeService, channelService);

        referenceResolver.resolveReferences(draft)
                         .thenAccept(resolvedDraft -> {
                             assertThat(resolvedDraft.getSupplyChannel()).isNotNull();
                             assertThat(resolvedDraft.getSupplyChannel().getId()).isEqualTo(CHANNEL_ID);
                         }).toCompletableFuture().join();
    }

    @Test
    public void resolveReferences_WithExceptionOnCustomTypeFetch_ShouldNotResolveReferences() {
        final InventoryEntryDraft draft = InventoryEntryDraft
            .of(SKU, QUANTITY, DATE_1, RESTOCKABLE_IN_DAYS, Reference.of(Channel.referenceTypeId(), UUID_KEY))
            .withCustom(getMockCustomFieldsDraft(CUSTOM_TYPE_KEY, new HashMap<>()));

        when(typeService.fetchCachedTypeId(anyString()))
            .thenReturn(CompletableFutureUtils.failed(new SphereException("bad request")));

        final InventoryReferenceResolver referenceResolver =
            new InventoryReferenceResolver(syncOptions, typeService, channelService);

        referenceResolver.resolveReferences(draft)
                         .exceptionally(exception -> {
                             assertThat(exception).isExactlyInstanceOf(CompletionException.class);
                             assertThat(exception.getCause()).isExactlyInstanceOf(SphereException.class);
                             assertThat(exception.getCause().getMessage()).contains("bad request");
                             return null;
                         }).toCompletableFuture().join();
    }

    @Test
    public void resolveReferences_WithCustomTypeKeyAsUuidSetAndNotAllowed_ShouldNotResolveCustomTypeReference() {
        final InventoryEntryDraft draft = InventoryEntryDraft
            .of(SKU, QUANTITY, DATE_1, RESTOCKABLE_IN_DAYS, Reference.of(Channel.referenceTypeId(), CHANNEL_KEY))
            .withCustom(getMockCustomFieldsDraft(UUID_KEY, new HashMap<>()));

        final InventoryReferenceResolver referenceResolver =
            new InventoryReferenceResolver(syncOptions, typeService, channelService);

        referenceResolver.resolveReferences(draft)
                         .exceptionally(exception -> {
                             assertThat(exception).isExactlyInstanceOf(CompletionException.class);
                             assertThat(exception.getCause())
                                 .isExactlyInstanceOf(ReferenceResolutionException.class);
                             assertThat(exception.getCause().getMessage())
                                 .isEqualTo("Failed to resolve custom type reference. Reason: Found a UUID"
                                     + " in the id field. Expecting a key without a UUID value. If you want to"
                                     + " allow UUID values for reference keys, please use the"
                                     + " setAllowUuid(true) option in the sync options.");
                             return null;
                         }).toCompletableFuture().join();
    }

    @Test
    public void resolveReferences_WithNonExistentCustomType_ShouldNotResolveCustomTypeReference() {
        when(typeService.fetchCachedTypeId(anyString()))
            .thenReturn(CompletableFuture.completedFuture(Optional.empty()));

        final InventoryEntryDraft draft = InventoryEntryDraft
            .of(SKU, QUANTITY, DATE_1, RESTOCKABLE_IN_DAYS, Reference.of(Channel.referenceTypeId(), CHANNEL_KEY))
            .withCustom(getMockCustomFieldsDraft(CUSTOM_TYPE_KEY, new HashMap<>()));

        final InventoryReferenceResolver referenceResolver =
            new InventoryReferenceResolver(syncOptions, typeService, channelService);

        referenceResolver.resolveReferences(draft)
                         .thenAccept(resolvedDraft -> {
                             assertThat(resolvedDraft.getCustom()).isNotNull();
                             assertThat(resolvedDraft.getCustom().getType()).isNotNull();
                             assertThat(resolvedDraft.getCustom().getType().getId()).isEqualTo(CUSTOM_TYPE_KEY);
                         }).toCompletableFuture().join();
    }

    @Test
    public void resolveReferences_WithEmptyIdOnSupplyChannelReference_ShouldNotResolveSupplyChannelReference() {
        final InventoryEntryDraft draft = InventoryEntryDraft
            .of(SKU, QUANTITY, DATE_1, RESTOCKABLE_IN_DAYS, Reference.of(Channel.referenceTypeId(), ""))
            .withCustom(getMockCustomFieldsDraft(CUSTOM_TYPE_KEY, new HashMap<>()));

        final InventoryReferenceResolver referenceResolver =
            new InventoryReferenceResolver(syncOptions, typeService, channelService);

        referenceResolver.resolveReferences(draft)
                         .exceptionally(exception -> {
                             assertThat(exception).isExactlyInstanceOf(CompletionException.class);
                             assertThat(exception.getCause())
                                 .isExactlyInstanceOf(ReferenceResolutionException.class);
                             assertThat(exception.getCause().getMessage())
                                 .isEqualTo("Failed to resolve supply channel reference. Reason: Key is blank "
                                     + "(null/empty) on both expanded reference object and reference id field.");
                             return null;
                         }).toCompletableFuture().join();
    }

    @Test
    public void resolveReferences_WithNullIdOnSupplyChannelReference_ShouldNotResolveSupplyChannelReference() {
        final InventoryEntryDraft draft = mock(InventoryEntryDraft.class);
        final Reference<Channel> supplyChannelReference =
            Reference.ofResourceTypeIdAndId(Channel.referenceTypeId(), null);
        when(draft.getSupplyChannel()).thenReturn(supplyChannelReference);

        final InventoryReferenceResolver referenceResolver =
            new InventoryReferenceResolver(syncOptions, typeService, channelService);

        referenceResolver.resolveReferences(draft)
                         .exceptionally(exception -> {
                             assertThat(exception).isExactlyInstanceOf(CompletionException.class);
                             assertThat(exception.getCause())
                                 .isExactlyInstanceOf(ReferenceResolutionException.class);
                             assertThat(exception.getCause().getMessage())
                                 .isEqualTo("Failed to resolve supply channel reference. Reason: Key is blank "
                                     + "(null/empty) on both expanded reference object and reference id field.");
                             return null;
                         }).toCompletableFuture().join();
    }

    @Test
    public void resolveReferences_WithNullIdOnCustomTypeReference_ShouldNotResolveCustomTypeReference() {
        final InventoryEntryDraft draft = InventoryEntryDraft
            .of(SKU, QUANTITY, DATE_1, RESTOCKABLE_IN_DAYS, Reference.of(Channel.referenceTypeId(), CHANNEL_KEY))
            .withCustom(getMockCustomFieldsDraft(null, new HashMap<>()));

        final InventoryReferenceResolver referenceResolver =
            new InventoryReferenceResolver(syncOptions, typeService, channelService);

        referenceResolver.resolveReferences(draft)
                         .exceptionally(exception -> {
                             assertThat(exception).isExactlyInstanceOf(CompletionException.class);
                             assertThat(exception.getCause())
                                 .isExactlyInstanceOf(ReferenceResolutionException.class);
                             assertThat(exception.getCause().getMessage())
                                 .isEqualTo("Failed to resolve custom type reference. Reason: Reference 'id' field"
                                     + " value is blank (null/empty).");
                             return null;
                         }).toCompletableFuture().join();
    }

    @Test
    public void resolveReferences_WithEmptyIdOnCustomTypeReference_ShouldNotResolveCustomTypeReference() {
        final InventoryEntryDraft draft = InventoryEntryDraft
            .of(SKU, QUANTITY, DATE_1, RESTOCKABLE_IN_DAYS, Reference.of(Channel.referenceTypeId(), CHANNEL_KEY))
            .withCustom(getMockCustomFieldsDraft("", new HashMap<>()));

        final InventoryReferenceResolver referenceResolver =
            new InventoryReferenceResolver(syncOptions, typeService, channelService);

        referenceResolver.resolveReferences(draft)
                         .exceptionally(exception -> {
                             assertThat(exception).isExactlyInstanceOf(CompletionException.class);
                             assertThat(exception.getCause())
                                 .isExactlyInstanceOf(ReferenceResolutionException.class);
                             assertThat(exception.getCause().getMessage())
                                 .isEqualTo("Failed to resolve custom type reference. Reason: Reference 'id' field"
                                     + " value is blank (null/empty).");
                             return null;
                         }).toCompletableFuture().join();
    }
}
