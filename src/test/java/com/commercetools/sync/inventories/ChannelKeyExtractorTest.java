package com.commercetools.sync.inventories;

import io.sphere.sdk.channels.Channel;
import io.sphere.sdk.inventory.InventoryEntryDraft;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ChannelKeyExtractorTest {

    private static final String SKU = "123";
    private static final String KEY = "testKey";
    private static final String REF_ID = "456";

    @Test
    public void extractChannelKey_FromDraftWithoutSupplyChannel_ShouldReturnNull() {
        final InventoryEntryDraft draft = InventoryEntryDraft.of(SKU, 1L);
        final String channelKey = ChannelKeyExtractor.extractChannelKey(draft);

        assertThat(channelKey).isNull();
    }

    @Test
    public void extractChannelKey_FromDraftWithNotExpandedReference_ShouldReturnReferenceId() {
        final InventoryEntryDraft draft = InventoryEntryDraft.of(SKU, 1L, null, null, Channel.referenceOfId(KEY));
        final String channelKey = ChannelKeyExtractor.extractChannelKey(draft);

        assertThat(channelKey).isEqualTo(KEY);
    }

    @Test
    public void extractChannelKey_FromDraftWithExpandedReference_ShouldReturnKeyFromObject() {
        final Channel channel = mock(Channel.class);
        when(channel.getKey()).thenReturn(KEY);
        final InventoryEntryDraft draft = InventoryEntryDraft.of(SKU, 1L, null, null,
                Channel.referenceOfId(REF_ID).filled(channel));
        final String channelKey = ChannelKeyExtractor.extractChannelKey(draft);

        assertThat(channelKey).isEqualTo(KEY);
    }
}
