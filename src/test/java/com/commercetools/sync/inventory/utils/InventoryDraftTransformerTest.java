package com.commercetools.sync.inventory.utils;

import com.commercetools.sync.inventory.InventoryEntryMock;
import io.sphere.sdk.inventory.InventoryEntry;
import io.sphere.sdk.inventory.InventoryEntryDraft;
import org.junit.Test;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

public class InventoryDraftTransformerTest {

    private static final String SKU = "10000";
    private static final Long QUANTITY_ON_STCK = 100l;
    private static final Integer RESTOCKABLE_IN_DAYS = 10;
    private static final ZonedDateTime EXPECTED_DELIVERY = ZonedDateTime.of(2017, 5, 1, 10, 0, 0, 0, ZoneId.of("UTC"));

    private static final String CHANNEL_ID = "11111";
    private static final String CHANNEL_KEY = "testKey";

    private static final String CUSTOM_TYPE_ID= "222222";
    private static final String CUSTOM_TYPE_KEY = "testTypeName";
    private static final String CUSTOM_FIELD_NAME = "testField";
    private static final String CUSTOM_FIELD_VALUE = "testValue";

    @Test
    public void transformToDraft_WithNoChannelNeitherCustomType_ShouldReturnDraft() {
        final InventoryEntry entry = InventoryEntryMock
                .of(SKU, QUANTITY_ON_STCK, RESTOCKABLE_IN_DAYS, EXPECTED_DELIVERY).build();
        final InventoryEntryDraft draft = InventoryDraftTransformer.transformToDraft(entry);

        assertThat(draft.getSku()).isEqualTo(SKU);
        assertThat(draft.getQuantityOnStock()).isEqualTo(QUANTITY_ON_STCK);
        assertThat(draft.getRestockableInDays()).isEqualTo(RESTOCKABLE_IN_DAYS);
        assertThat(draft.getExpectedDelivery()).isEqualTo(EXPECTED_DELIVERY);
        assertThat(draft.getCustom()).isNull();
        assertThat(draft.getSupplyChannel()).isNull();
    }

    @Test
    public void transformToDraft_WithSupplyChannel_ShouldReturnDraft() {
        final InventoryEntry entry = InventoryEntryMock.of(SKU)
                .withChannelRefExpanded(CHANNEL_ID, CHANNEL_KEY).build();
        final InventoryEntryDraft draft = InventoryDraftTransformer.transformToDraft(entry);

        assertThat(draft.getSupplyChannel()).isNotNull();
        assertThat(draft.getSupplyChannel().getId()).isEqualTo(CHANNEL_ID);
        assertThat(draft.getSupplyChannel().getObj()).isNotNull();
        assertThat(draft.getSupplyChannel().getObj().getKey()).isEqualTo(CHANNEL_KEY);
    }

    @Test
    public void transformToDraft_WithCustomTypeWithoutKey_ShouldReturnDraft() {
        final InventoryEntry entry = InventoryEntryMock.of(SKU)
                .withCustomField(CUSTOM_TYPE_ID, CUSTOM_FIELD_NAME, CUSTOM_FIELD_VALUE).build();

        final InventoryEntryDraft draft = InventoryDraftTransformer.transformToDraft(entry);

        assertThat(draft.getCustom()).isNotNull();
        assertThat(draft.getCustom().getType()).isNotNull();
        assertThat(draft.getCustom().getType().getId()).isEqualTo(CUSTOM_TYPE_ID);
        assertThat(draft.getCustom().getFields()).isNotNull();
        assertThat(draft.getCustom().getFields().containsKey(CUSTOM_FIELD_NAME)).isTrue();
    }

    @Test
    public void transformToDraft_WithCustomTypeWithKey_ShouldReturnDraft () {
        final InventoryEntry entry = InventoryEntryMock.of(SKU)
                .withCustomFieldExpanded(CUSTOM_TYPE_ID, CUSTOM_TYPE_KEY, CUSTOM_FIELD_NAME, CUSTOM_FIELD_VALUE)
                .build();

        final InventoryEntryDraft draft = InventoryDraftTransformer.transformToDraft(entry);

        assertThat(draft.getCustom()).isNotNull();
        assertThat(draft.getCustom().getType()).isNotNull();
        assertThat(draft.getCustom().getType().getKey()).isEqualTo(CUSTOM_TYPE_KEY);
        assertThat(draft.getCustom().getFields()).isNotNull();
        assertThat(draft.getCustom().getFields().containsKey(CUSTOM_FIELD_NAME)).isTrue();
    }

    @Test
    public void transformToDrafts_ShouldReturnDrafts() {
        final InventoryEntry entry = InventoryEntryMock.of(SKU).build();
        final List<InventoryEntryDraft> drafts = InventoryDraftTransformer.transformToDrafts(asList(entry, entry));

        assertThat(drafts).isNotNull();
        assertThat(drafts).hasSize(2);
    }
}
