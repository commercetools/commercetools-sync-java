package com.commercetools.sync.inventories.utils;

import io.sphere.sdk.channels.Channel;
import io.sphere.sdk.inventory.InventoryEntry;
import io.sphere.sdk.inventory.InventoryEntryDraft;
import io.sphere.sdk.models.Reference;
import io.sphere.sdk.types.CustomFields;
import org.junit.Test;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import static com.commercetools.sync.inventories.InventorySyncMockUtils.getMockSupplyChannel;
import static com.commercetools.sync.inventories.InventorySyncMockUtils.getMockCustomFields;
import static com.commercetools.sync.inventories.InventorySyncMockUtils.getMockInventoryEntry;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

public class InventoryDraftTransformerUtilsTest {

    private static final String SKU = "10000";
    private static final Long QUANTITY_ON_STCK = 100L;
    private static final Integer RESTOCKABLE_IN_DAYS = 10;
    private static final ZonedDateTime EXPECTED_DELIVERY = ZonedDateTime.of(2017, 5, 1, 10, 0, 0, 0, ZoneId.of("UTC"));

    private static final String CHANNEL_ID = "11111";
    private static final String CHANNEL_KEY = "testKey";

    private static final String CUSTOM_TYPE_ID = "222222";
    private static final String CUSTOM_TYPE_KEY = "testTypeName";
    private static final String CUSTOM_FIELD_NAME = "testField";
    private static final String CUSTOM_FIELD_VALUE = "testValue";

    @Test
    public void transformToDraft_WithNoChannelNeitherCustomType_ShouldReturnDraft() {
        final InventoryEntry entry = getMockInventoryEntry(SKU, QUANTITY_ON_STCK, RESTOCKABLE_IN_DAYS,
            EXPECTED_DELIVERY, null, null);
        final InventoryEntryDraft draft = InventoryDraftTransformerUtils.transformToDraft(entry);

        assertThat(draft.getSku()).isEqualTo(SKU);
        assertThat(draft.getQuantityOnStock()).isEqualTo(QUANTITY_ON_STCK);
        assertThat(draft.getRestockableInDays()).isEqualTo(RESTOCKABLE_IN_DAYS);
        assertThat(draft.getExpectedDelivery()).isEqualTo(EXPECTED_DELIVERY);
        assertThat(draft.getCustom()).isNull();
        assertThat(draft.getSupplyChannel()).isNull();
    }

    @Test
    public void transformToDraft_WithSupplyChannel_ShouldReturnDraft() {
        final Channel channel = getMockSupplyChannel(CHANNEL_ID, CHANNEL_KEY);
        final Reference<Channel> reference = Channel.referenceOfId(CHANNEL_ID).filled(channel);
        final InventoryEntry entry = getMockInventoryEntry(SKU, null, null, null, reference, null);
        final InventoryEntryDraft draft = InventoryDraftTransformerUtils.transformToDraft(entry);

        assertThat(draft.getSupplyChannel()).isNotNull();
        assertThat(draft.getSupplyChannel().getId()).isEqualTo(CHANNEL_ID);
        assertThat(draft.getSupplyChannel().getObj()).isNotNull();
        assertThat(draft.getSupplyChannel().getObj().getKey()).isEqualTo(CHANNEL_KEY);
    }

    @Test
    public void transformToDraft_WithCustomTypeWithoutKey_ShouldReturnDraft() {
        final CustomFields customFields = getMockCustomFields(CUSTOM_TYPE_ID, null, CUSTOM_FIELD_NAME,
                CUSTOM_FIELD_VALUE);
        final InventoryEntry entry = getMockInventoryEntry(SKU, null, null, null, null, customFields);

        final InventoryEntryDraft draft = InventoryDraftTransformerUtils.transformToDraft(entry);

        assertThat(draft.getCustom()).isNotNull();
        assertThat(draft.getCustom().getType()).isNotNull();
        assertThat(draft.getCustom().getType().getId()).isEqualTo(CUSTOM_TYPE_ID);
        assertThat(draft.getCustom().getFields()).isNotNull();
        assertThat(draft.getCustom().getFields().containsKey(CUSTOM_FIELD_NAME)).isTrue();
    }

    @Test
    public void transformToDraft_WithCustomTypeWithKey_ShouldReturnDraft() {
        final CustomFields customFields = getMockCustomFields(CUSTOM_TYPE_ID, CUSTOM_TYPE_KEY, CUSTOM_FIELD_NAME,
                CUSTOM_FIELD_VALUE);
        final InventoryEntry entry = getMockInventoryEntry(SKU, null, null, null, null, customFields);

        final InventoryEntryDraft draft = InventoryDraftTransformerUtils.transformToDraft(entry);

        assertThat(draft.getCustom()).isNotNull();
        assertThat(draft.getCustom().getType()).isNotNull();
        assertThat(draft.getCustom().getType().getKey()).isEqualTo(CUSTOM_TYPE_KEY);
        assertThat(draft.getCustom().getFields()).isNotNull();
        assertThat(draft.getCustom().getFields().containsKey(CUSTOM_FIELD_NAME)).isTrue();
    }

    @Test
    public void transformToDrafts_ShouldReturnDrafts() {
        final InventoryEntry entry = getMockInventoryEntry(SKU, null, null, null, null, null);
        final List<InventoryEntryDraft> drafts = InventoryDraftTransformerUtils.transformToDrafts(asList(entry, entry));

        assertThat(drafts).isNotNull();
        assertThat(drafts).hasSize(2);
        assertThat(drafts.get(0)).isNotNull();
        assertThat(drafts.get(1)).isNotNull();
        assertThat(drafts.get(0).getSku()).isEqualTo(SKU);
        assertThat(drafts.get(1).getSku()).isEqualTo(SKU);
    }

    @Test
    public void transformToDrafts_WithEmptyList_ShouldReturnEmptyList() {
        final List<InventoryEntryDraft> drafts = InventoryDraftTransformerUtils.transformToDrafts(new ArrayList<>());

        assertThat(drafts).isNotNull();
        assertThat(drafts).isEmpty();
    }
}
