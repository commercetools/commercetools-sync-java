package com.commercetools.sync.inventory.impl;

import io.sphere.sdk.channels.Channel;
import io.sphere.sdk.inventory.InventoryEntry;
import io.sphere.sdk.inventory.InventoryEntryDraft;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SkuKeyTupleTest {

    private static final String SKU = "123";
    private static final String SKU_2 = "321";
    private static final String KEY = "testKey";
    private static final String KEY_2 = "differentKey";
    private static final String REF_ID = "456";

    @Test
    public void ofInventoryEntryDraftReturnsTupleForDraftWithoutSupplyChannel() {
        final InventoryEntryDraft draft = InventoryEntryDraft.of(SKU, 1l);
        final SkuKeyTuple tuple = SkuKeyTuple.of(draft);
        assertThat(tuple).isNotNull();
        assertThat(tuple.getSku()).isEqualTo(SKU);
        assertThat(tuple.getKey()).isNull();
    }

    @Test
    public void ofInventoryEntryDraftReturnsTupleForDraftWithNotExpandedReference() {
        final InventoryEntryDraft draft = InventoryEntryDraft.of(SKU, 1l, null, null, Channel.referenceOfId(KEY));
        final SkuKeyTuple tuple = SkuKeyTuple.of(draft);
        assertThat(tuple).isNotNull();
        assertThat(tuple.getSku()).isEqualTo(SKU);
        assertThat(tuple.getKey()).isEqualTo(KEY);
    }

    @Test
    public void ofInventoryEntryDraftReturnsTupleForDraftWithExpandedReference() {
        final Channel channel = mock(Channel.class);
        when(channel.getKey()).thenReturn(KEY);
        final InventoryEntryDraft draft = InventoryEntryDraft.of(SKU, 1l, null, null,
                Channel.referenceOfId(REF_ID).filled(channel));
        final SkuKeyTuple tuple = SkuKeyTuple.of(draft);

        assertThat(tuple).isNotNull();
        assertThat(tuple.getSku()).isEqualTo(SKU);
        assertThat(tuple.getKey()).isEqualTo(KEY);
    }

    @Test
    public void ofInventoryEntryReturnsTupleForEntryWithoutReference() {
        final SkuKeyTuple tuple = SkuKeyTuple.of(mockInventoryEntry(SKU));
        assertThat(tuple).isNotNull();
        assertThat(tuple.getSku()).isEqualTo(SKU);
        assertThat(tuple.getKey()).isNull();
    }

    @Test
    public void ofInventoryEntryReturnsTupleForEntryWithNotExpandedReference() {
        final SkuKeyTuple tuple = SkuKeyTuple.of(mockInventoryEntryWithReference(SKU, REF_ID));
        assertThat(tuple).isNotNull();
        assertThat(tuple.getSku()).isEqualTo(SKU);
        assertThat(tuple.getKey()).isNull();
    }

    @Test
    public void ofInventoryEntryReturnsTupleForEntryWithExpandedReference() {
        final SkuKeyTuple tuple = SkuKeyTuple.of(mockInventoryEntryWithExpandedReference(SKU, REF_ID, KEY));
        assertThat(tuple).isNotNull();
        assertThat(tuple.getSku()).isEqualTo(SKU);
        assertThat(tuple.getKey()).isEqualTo(KEY);
    }

    @Test
    public void thatSkuKeyTuplesOfSimilarDraftAndEntryAreEquals() {
        final SkuKeyTuple entryTuple = SkuKeyTuple.of(mockInventoryEntryWithExpandedReference(SKU, REF_ID, KEY));
        final SkuKeyTuple draftTuple = SkuKeyTuple.of(InventoryEntryDraft.of(SKU, 1l, null, null, Channel.referenceOfId(KEY)));

        assertThat(entryTuple).isEqualTo(draftTuple);
        assertThat(entryTuple.hashCode()).isEqualTo(draftTuple.hashCode());
    }

    @Test
    public void thatSkuKeyTuplesOfNotSimilarDraftAndEntryAreNotEquals() {
        final SkuKeyTuple entryTuple = SkuKeyTuple.of(mockInventoryEntryWithExpandedReference(SKU_2, REF_ID, KEY_2));
        final SkuKeyTuple draftTuple = SkuKeyTuple.of(InventoryEntryDraft.of(SKU, 1l, null, null, Channel.referenceOfId(KEY)));

        assertThat(entryTuple).isNotEqualTo(draftTuple);
    }

    private InventoryEntry mockInventoryEntry(String sku){
        final InventoryEntry entry = mock(InventoryEntry.class);
        when(entry.getSku()).thenReturn(sku);
        return entry;
    }

    private InventoryEntry mockInventoryEntryWithReference(String sku, String refId) {
        final InventoryEntry entry = mockInventoryEntry(sku);
        when(entry.getSupplyChannel()).thenReturn(Channel.referenceOfId(refId));
        return entry;
    }

    private InventoryEntry mockInventoryEntryWithExpandedReference(String sku, String refId, String key) {
        final InventoryEntry entry = mockInventoryEntry(sku);
        final Channel channel = mock(Channel.class);
        when(channel.getKey()).thenReturn(key);
        when(entry.getSupplyChannel()).thenReturn(Channel.referenceOfId(refId).filled(channel));
        return entry;
    }
}
