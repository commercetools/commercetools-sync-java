package com.commercetools.sync.inventories;

import io.sphere.sdk.channels.Channel;
import io.sphere.sdk.inventory.InventoryEntry;
import io.sphere.sdk.inventory.InventoryEntryDraft;
import io.sphere.sdk.models.Reference;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static com.commercetools.sync.inventories.InventorySyncMockUtils.getMockInventoryEntry;
import static com.commercetools.sync.inventories.InventorySyncMockUtils.getMockSupplyChannel;
import static com.commercetools.sync.inventories.SkuChannelKeyTuple.SKU_NOT_SET_MESSAGE;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SkuChannelKeyTupleTest {

    private static final String SKU = "123";
    private static final String SKU_2 = "321";
    private static final String KEY = "testKey";
    private static final String KEY_2 = "differentKey";
    private static final String REF_ID = "456";
    private static final String REF_ID_2 = "789";
    private static final String REF_ID_3 = "000";
    private static final String VALUE_1 = "testValue";
    private static final String VALUE_2 = "otherValue";

    private static ChannelsMap channelsMap;

    /**
     * Initialise channelsMap with channels:
     * <ol>
     *     <li>of id {@code REF_ID_1} and key {@code KEY}</li>
     *     <li>of id {@code REF_ID_2} and key {@code KEY_2}</li>
     * </ol>.
     */
    @BeforeClass
    public static void setup() {
        channelsMap = ChannelsMap.Builder.of(
            asList(getMockSupplyChannel(REF_ID, KEY), getMockSupplyChannel(REF_ID_2, KEY_2))
        ).build();
    }

    @Test
    public void build_WithDraftWithoutSupplyChannel_ShouldReturnTuple() {
        final InventoryEntryDraft draft = InventoryEntryDraft.of(SKU, 1L);
        final SkuChannelKeyTuple tuple = SkuChannelKeyTuple.of(draft);
        assertThat(tuple).isNotNull();
        assertThat(tuple.getSku()).isEqualTo(SKU);
        assertThat(tuple.getKey()).isNull();
    }

    @Test
    public void build_WithDraftWithNotExpandedReference_ShouldReturnTuple() {
        final InventoryEntryDraft draft = InventoryEntryDraft.of(SKU, 1L, null, null, Channel.referenceOfId(KEY));
        final SkuChannelKeyTuple tuple = SkuChannelKeyTuple.of(draft);
        assertThat(tuple).isNotNull();
        assertThat(tuple.getSku()).isEqualTo(SKU);
        assertThat(tuple.getKey()).isEqualTo(KEY);
    }

    @Test
    public void build_WithDraftWithExpandedReference_ShouldReturnTuple() {
        final Channel channel = mock(Channel.class);
        when(channel.getKey()).thenReturn(KEY);
        final InventoryEntryDraft draft = InventoryEntryDraft.of(SKU, 1L, null, null,
                Channel.referenceOfId(REF_ID).filled(channel));
        final SkuChannelKeyTuple tuple = SkuChannelKeyTuple.of(draft);

        assertThat(tuple).isNotNull();
        assertThat(tuple.getSku()).isEqualTo(SKU);
        assertThat(tuple.getKey()).isEqualTo(KEY);
    }

    @Test
    public void build_WithEntryWithoutReference_ShouldReturnTuple() {
        final InventoryEntry inventoryEntry = getMockInventoryEntry(SKU, null, null, null, null, null);
        final Optional<SkuChannelKeyTuple> tupleOptional = SkuChannelKeyTuple.of(inventoryEntry, channelsMap);
        assertThat(tupleOptional).isNotEmpty();
        assertThat(tupleOptional.get().getSku()).isEqualTo(SKU);
        assertThat(tupleOptional.get().getKey()).isNull();
    }

    @Test
    public void build_WithEntryWithNotExpandedReference_ShouldReturnTuple() {
        final Reference<Channel> reference = Channel.referenceOfId(REF_ID);
        final InventoryEntry inventoryEntry = getMockInventoryEntry(SKU, null, null, null, reference, null);
        final Optional<SkuChannelKeyTuple> tupleOptional = SkuChannelKeyTuple.of(inventoryEntry, channelsMap);
        assertThat(tupleOptional).isNotEmpty();
        assertThat(tupleOptional.get().getSku()).isEqualTo(SKU);
        assertThat(tupleOptional.get().getKey()).isEqualTo(KEY);
    }

    @Test
    public void build_WithEntryWithExpandedReference_ShouldReturnTuple() {
        final Channel channel = getMockSupplyChannel(REF_ID, KEY);
        final Reference<Channel> reference = Channel.referenceOfId(REF_ID).filled(channel);
        final InventoryEntry inventoryEntry = getMockInventoryEntry(SKU, null, null, null, reference, null);
        final Optional<SkuChannelKeyTuple> tupleOptional = SkuChannelKeyTuple.of(inventoryEntry, channelsMap);
        assertThat(tupleOptional).isNotEmpty();
        assertThat(tupleOptional.get().getSku()).isEqualTo(SKU);
        assertThat(tupleOptional.get().getKey()).isEqualTo(KEY);
    }

    @Test
    public void build_WithEntryWithReferenceOfNotCachedId_ShouldReturnEmptyOptional() {
        final Reference<Channel> reference = Channel.referenceOfId(REF_ID_3);
        final InventoryEntry inventoryEntry = getMockInventoryEntry(SKU, null, null, null, reference, null);
        final Optional<SkuChannelKeyTuple> tupleOptional = SkuChannelKeyTuple.of(inventoryEntry, channelsMap);
        assertThat(tupleOptional).isEmpty();
    }

    @Test
    public void build_WithDraftWithNullSku_ShouldThrowIllegallArgumentException() {
        InventoryEntryDraft mockDraft = mock(InventoryEntryDraft.class);
        when(mockDraft.getSku()).thenReturn(null);
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> SkuChannelKeyTuple.of(mockDraft))
                .withMessage(SKU_NOT_SET_MESSAGE);
    }

    @Test
    public void build_WithDraftWithEmptySku_ShouldThrowIllegallArgumentException() {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> SkuChannelKeyTuple.of(InventoryEntryDraft.of("", 1L)))
                .withMessage(SKU_NOT_SET_MESSAGE);
    }

    @Test
    public void skuKeyTuplesCreatedFromSimilarDraftAndEntry_ShouldBeEqual() {
        final Reference<Channel> reference = Channel.referenceOfId(REF_ID);
        final InventoryEntry inventoryEntry = getMockInventoryEntry(SKU, null, null, null, reference, null);
        final Optional<SkuChannelKeyTuple> entryTupleOptional = SkuChannelKeyTuple.of(inventoryEntry, channelsMap);
        final SkuChannelKeyTuple draftTuple = SkuChannelKeyTuple
                .of(InventoryEntryDraft.of(SKU, 1L, null, null, Channel.referenceOfId(KEY)));

        assertThat(entryTupleOptional).isNotEmpty();
        assertThat(entryTupleOptional.get()).isEqualTo(draftTuple);
    }

    @Test
    public void skuKeyTuplesCreatedFromVariousDraftAndEntry_ShouldNotBeEqual() {
        final Reference<Channel> reference = Channel.referenceOfId(REF_ID_2);
        final InventoryEntry inventoryEntry = getMockInventoryEntry(SKU_2, null, null, null, reference, null);
        final Optional<SkuChannelKeyTuple> entryTupleOptional = SkuChannelKeyTuple.of(inventoryEntry, channelsMap);
        final SkuChannelKeyTuple draftTuple = SkuChannelKeyTuple
                .of(InventoryEntryDraft.of(SKU, 1L, null, null, Channel.referenceOfId(KEY)));

        assertThat(entryTupleOptional).isNotEmpty();
        assertThat(entryTupleOptional.get()).isNotEqualTo(draftTuple);
    }

    @Test
    public void skuKeyTuplesCreatedFromSimilarDraftAndEntry_ShouldHaveSameHashCodes() {
        final Reference<Channel> reference = Channel.referenceOfId(REF_ID);
        final InventoryEntry inventoryEntry = getMockInventoryEntry(SKU, null, null, null, reference, null);
        final Optional<SkuChannelKeyTuple> entryTupleOptional = SkuChannelKeyTuple.of(inventoryEntry, channelsMap);
        final SkuChannelKeyTuple draftTuple = SkuChannelKeyTuple
                .of(InventoryEntryDraft.of(SKU, 1L, null, null, Channel.referenceOfId(KEY)));

        assertThat(entryTupleOptional).isNotEmpty();
        assertThat(entryTupleOptional.get().hashCode()).isEqualTo(draftTuple.hashCode());
    }

    @Test
    public void skuKeyTuple_ShouldWorksAsHashMapKey() {
        final SkuChannelKeyTuple tuple = SkuChannelKeyTuple.of(
                InventoryEntryDraft.of(SKU, 1L, null, null, Channel.referenceOfId(KEY)));
        final Map<SkuChannelKeyTuple, String> map = new HashMap<>();
        map.put(tuple, VALUE_1);

        assertThat(map.containsKey(tuple)).isTrue();
        assertThat(map.get(tuple)).isEqualTo(VALUE_1);
    }

    @Test
    public void skuKeyTuplesWithNullKeyAndEmptyKey_ShouldBeDistinctedInHashMap() {
        final SkuChannelKeyTuple tuple1 = SkuChannelKeyTuple.of(InventoryEntryDraft.of(SKU, 1L));
        final SkuChannelKeyTuple tuple2 = SkuChannelKeyTuple.of(
                InventoryEntryDraft.of(SKU, 1L, null, null, Channel.referenceOfId("")));
        final Map<SkuChannelKeyTuple, String> map = new HashMap<>();
        map.put(tuple1, VALUE_1);
        map.put(tuple2, VALUE_2);

        assertThat(tuple1.getKey()).isNull();
        assertThat(tuple2.getKey()).isEmpty();
        assertThat(map.containsKey(tuple1)).isTrue();
        assertThat(map.containsKey(tuple2)).isTrue();
        assertThat(map.get(tuple1)).isEqualTo(VALUE_1);
        assertThat(map.get(tuple2)).isEqualTo(VALUE_2);
    }
}
