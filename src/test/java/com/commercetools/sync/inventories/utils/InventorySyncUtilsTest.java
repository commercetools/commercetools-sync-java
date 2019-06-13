package com.commercetools.sync.inventories.utils;

import com.commercetools.sync.inventories.InventorySyncOptionsBuilder;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import io.sphere.sdk.channels.Channel;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.inventory.InventoryEntry;
import io.sphere.sdk.inventory.InventoryEntryDraft;
import io.sphere.sdk.inventory.InventoryEntryDraftBuilder;
import io.sphere.sdk.inventory.commands.updateactions.ChangeQuantity;
import io.sphere.sdk.inventory.commands.updateactions.SetCustomField;
import io.sphere.sdk.inventory.commands.updateactions.SetCustomType;
import io.sphere.sdk.inventory.commands.updateactions.SetExpectedDelivery;
import io.sphere.sdk.inventory.commands.updateactions.SetRestockableInDays;
import io.sphere.sdk.inventory.commands.updateactions.SetSupplyChannel;
import io.sphere.sdk.models.Reference;
import io.sphere.sdk.models.ResourceIdentifier;
import io.sphere.sdk.types.CustomFields;
import io.sphere.sdk.types.CustomFieldsDraft;
import io.sphere.sdk.types.CustomFieldsDraftBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

import static com.commercetools.sync.commons.MockUtils.getMockCustomFields;
import static com.commercetools.sync.inventories.InventorySyncMockUtils.getMockInventoryEntry;
import static com.commercetools.sync.inventories.InventorySyncMockUtils.getMockSupplyChannel;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class InventorySyncUtilsTest {
    private static final String CUSTOM_TYPE_ID = "testId";
    private static final String CUSTOM_FIELD_1_NAME = "testField";
    private static final String CUSTOM_FIELD_2_NAME = "differentField";
    private static final String CUSTOM_FIELD_1_VALUE = "testValue";
    private static final String CUSTOM_FIELD_2_VALUE = "differentValue";

    private static final ZonedDateTime DATE_1 = ZonedDateTime.of(2017, 4, 1, 10, 0, 0, 0, ZoneId.of("UTC"));
    private static final ZonedDateTime DATE_2 = ZonedDateTime.of(2017, 5, 1, 20, 0, 0, 0, ZoneId.of("UTC"));

    private InventoryEntry inventoryEntry;
    private InventoryEntry inventoryEntryWithCustomField1;
    private InventoryEntryDraft similarDraft;
    private InventoryEntryDraft variousDraft;

    /**
     * Initialises test data.
     */
    @BeforeEach
    void setup() {
        final Channel channel = getMockSupplyChannel("111", "key1");
        final Reference<Channel> reference = Channel.referenceOfId("111").filled(channel);
        final CustomFields customFields = getMockCustomFields(CUSTOM_TYPE_ID, CUSTOM_FIELD_1_NAME,
            JsonNodeFactory.instance.textNode(CUSTOM_FIELD_1_VALUE));

        inventoryEntry = getMockInventoryEntry("123", 10L, 10, DATE_1, reference, null);
        inventoryEntryWithCustomField1 = getMockInventoryEntry("123", 10L, 10, DATE_1, reference, customFields);


        similarDraft = InventoryEntryDraftBuilder.of("123", 10L, DATE_1, 10, ResourceIdentifier.ofId("111"))
                                                 .build();
        variousDraft = InventoryEntryDraftBuilder.of("321", 20L, DATE_2, 20, ResourceIdentifier.ofId("222"))
                                                 .build();
    }

    @Test
    void buildActions_WithSimilarEntries_ShouldReturnEmptyList() {
        List<UpdateAction<InventoryEntry>> actions = InventorySyncUtils
            .buildActions(inventoryEntry, similarDraft, InventorySyncOptionsBuilder.of(mock(SphereClient.class))
                                                                                   .build());

        assertThat(actions).isEmpty();
    }

    @Test
    void buildActions_WithVariousEntries_ShouldReturnActions() {
        List<UpdateAction<InventoryEntry>> actions = InventorySyncUtils
            .buildActions(inventoryEntry, variousDraft, InventorySyncOptionsBuilder.of(mock(SphereClient.class))
                                                                                   .build());

        assertThat(actions).hasSize(4);
        assertThat(actions.get(0)).isNotNull();
        assertThat(actions.get(1)).isNotNull();
        assertThat(actions.get(2)).isNotNull();
        assertThat(actions.get(3)).isNotNull();
        assertThat(actions.get(0)).isInstanceOf(ChangeQuantity.class);
        assertThat(actions.get(1)).isInstanceOf(SetRestockableInDays.class);
        assertThat(actions.get(2)).isInstanceOf(SetExpectedDelivery.class);
        assertThat(actions.get(3)).isInstanceOfAny(SetSupplyChannel.class);
    }

    @Test
    void buildActions_WithSimilarEntriesAndSameCustomFields_ShouldReturnEmptyList() {
        final InventoryEntryDraft draft = InventoryEntryDraftBuilder.of(similarDraft)
                                                                    .custom(getDraftOfCustomField(CUSTOM_FIELD_1_NAME,
                                                                        CUSTOM_FIELD_1_VALUE))
                                                                    .build();
        final List<UpdateAction<InventoryEntry>> actions = InventorySyncUtils
            .buildActions(inventoryEntryWithCustomField1,
                draft, InventorySyncOptionsBuilder.of(mock(SphereClient.class)).build());

        assertThat(actions).isEmpty();
    }

    @Test
    void buildActions_WithSimilarEntriesAndNewCustomTypeSet_ShouldReturnActions() {
        final InventoryEntryDraft draft = InventoryEntryDraftBuilder.of(similarDraft)
                                                                    .custom(getDraftOfCustomField(CUSTOM_FIELD_2_NAME,
                                                                        CUSTOM_FIELD_2_VALUE)).build();
        final List<UpdateAction<InventoryEntry>> actions = InventorySyncUtils.buildActions(inventoryEntry, draft,
            InventorySyncOptionsBuilder.of(mock(SphereClient.class)).build());

        assertThat(actions).hasSize(1);
        assertThat(actions.get(0)).isNotNull();
        assertThat(actions.get(0)).isInstanceOf(SetCustomType.class);
    }

    @Test
    void buildActions_WithSimilarEntriesAndRemovedExistingCustomType_ShouldReturnActions() {
        final List<UpdateAction<InventoryEntry>> actions = InventorySyncUtils
            .buildActions(inventoryEntryWithCustomField1,
                similarDraft, InventorySyncOptionsBuilder.of(mock(SphereClient.class)).build());

        assertThat(actions).hasSize(1);
        assertThat(actions.get(0)).isNotNull();
        assertThat(actions.get(0)).isInstanceOf(SetCustomType.class);
    }

    @Test
    void buildActions_WithSimilarEntriesButDifferentCustomFields_ShouldReturnActions() {
        final InventoryEntryDraft draft = InventoryEntryDraftBuilder.of(similarDraft)
                                                                    .custom(getDraftOfCustomField(CUSTOM_FIELD_2_NAME,
                                                                        CUSTOM_FIELD_2_VALUE))
                                                                    .build();
        final List<UpdateAction<InventoryEntry>> actions =
            InventorySyncUtils.buildActions(inventoryEntryWithCustomField1, draft,
                InventorySyncOptionsBuilder.of(mock(SphereClient.class)).build());

        assertThat(actions).hasSize(2);
        assertThat(actions.get(0)).isNotNull();
        assertThat(actions.get(1)).isNotNull();
        assertThat(actions.get(0)).isInstanceOf(SetCustomField.class);
        assertThat(actions.get(1)).isInstanceOf(SetCustomField.class);
    }

    @Test
    void buildActions_WithSimilarEntriesButDifferentCustomFieldValues_ShouldReturnActions() {
        final InventoryEntryDraft draft = InventoryEntryDraftBuilder.of(similarDraft)
                                                                    .custom(getDraftOfCustomField(CUSTOM_FIELD_1_NAME,
                                                                        CUSTOM_FIELD_2_VALUE))
                                                                    .build();
        final List<UpdateAction<InventoryEntry>> actions = InventorySyncUtils
            .buildActions(inventoryEntryWithCustomField1, draft, InventorySyncOptionsBuilder
                .of(mock(SphereClient.class))
                .build());

        assertThat(actions).hasSize(1);
        assertThat(actions.get(0)).isNotNull();
        assertThat(actions.get(0)).isInstanceOf(SetCustomField.class);
    }

    private CustomFieldsDraft getDraftOfCustomField(final String fieldName, final String fieldValue) {
        return CustomFieldsDraftBuilder.ofTypeId(CUSTOM_TYPE_ID)
                                       .addObject(fieldName, fieldValue)
                                       .build();
    }
}
