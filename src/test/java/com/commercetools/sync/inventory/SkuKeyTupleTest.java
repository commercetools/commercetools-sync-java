package com.commercetools.sync.inventory;

import io.sphere.sdk.channels.Channel;
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
    public void build_WithDraftWithoutSupplyChannel_ShouldReturnTuple() {
        final InventoryEntryDraft draft = InventoryEntryDraft.of(SKU, 1l);
        final SkuKeyTuple tuple = SkuKeyTuple.of(draft);
        assertThat(tuple).isNotNull();
        assertThat(tuple.getSku()).isEqualTo(SKU);
        assertThat(tuple.getKey()).isNull();
    }

    @Test
    public void build_WithDraftWithNotExpandedReference_ShouldReturnTuple() {
        final InventoryEntryDraft draft = InventoryEntryDraft.of(SKU, 1l, null, null, Channel.referenceOfId(KEY));
        final SkuKeyTuple tuple = SkuKeyTuple.of(draft);
        assertThat(tuple).isNotNull();
        assertThat(tuple.getSku()).isEqualTo(SKU);
        assertThat(tuple.getKey()).isEqualTo(KEY);
    }

    @Test
    public void build_WithDraftWithExpandedReference_ShouldReturnTuple() {
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
    public void build_WithEntryWithoutReference_ShouldReturnTuple() {
        final SkuKeyTuple tuple = SkuKeyTuple.of(InventoryEntryMock.of(SKU).build());
        assertThat(tuple).isNotNull();
        assertThat(tuple.getSku()).isEqualTo(SKU);
        assertThat(tuple.getKey()).isNull();
    }

    @Test
    public void build_WithEntryWithNotExpandedReference_ShouldReturnTuple() {
        final SkuKeyTuple tuple = SkuKeyTuple.of(InventoryEntryMock.of(SKU).withChannelRef(REF_ID).build());
        assertThat(tuple).isNotNull();
        assertThat(tuple.getSku()).isEqualTo(SKU);
        assertThat(tuple.getKey()).isNull();
    }

    @Test
    public void build_WithEntryWithExpandedReference_ShouldReturnTuple() {
        final SkuKeyTuple tuple = SkuKeyTuple
                .of(InventoryEntryMock.of(SKU).withChannelRefExpanded(REF_ID, KEY).build());
        assertThat(tuple).isNotNull();
        assertThat(tuple.getSku()).isEqualTo(SKU);
        assertThat(tuple.getKey()).isEqualTo(KEY);
    }

    @Test
    public void skuKeyTuplesCreatedFromSimilarDraftAndEntry_ShouldBeEqual() {
        final SkuKeyTuple entryTuple = SkuKeyTuple
                .of(InventoryEntryMock.of(SKU).withChannelRefExpanded(REF_ID, KEY).build());
        final SkuKeyTuple draftTuple = SkuKeyTuple
                .of(InventoryEntryDraft.of(SKU, 1l, null, null, Channel.referenceOfId(KEY)));

        assertThat(entryTuple).isEqualTo(draftTuple);
        assertThat(entryTuple.hashCode()).isEqualTo(draftTuple.hashCode());
    }

    @Test
    public void skuKeyTuplesCreatedFromVariousDraftAndEntry_ShouldNotBeEqual() {
        final SkuKeyTuple entryTuple = SkuKeyTuple
                .of(InventoryEntryMock.of(SKU_2).withChannelRefExpanded(REF_ID, KEY_2).build());
        final SkuKeyTuple draftTuple = SkuKeyTuple
                .of(InventoryEntryDraft.of(SKU, 1l, null, null, Channel.referenceOfId(KEY)));

        assertThat(entryTuple).isNotEqualTo(draftTuple);
    }
}
