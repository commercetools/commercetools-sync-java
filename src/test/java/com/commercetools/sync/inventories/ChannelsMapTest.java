package com.commercetools.sync.inventories;

import io.sphere.sdk.channels.Channel;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;

import static com.commercetools.sync.inventories.InventorySyncMockUtils.getMockSupplyChannel;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

public class ChannelsMapTest {

    private static final String CHANNEL_KEY_1 = "key_1";
    private static final String CHANNEL_ID_1 = "id_1";
    private static final String CHANNEL_KEY_2 = "key_2";
    private static final String CHANNEL_ID_2 = "id_2";

    private static ChannelsMap channelsMap;

    /**
     * Initialise channelsMap with channels:
     * <ol>
     *     <li>of id {@code CHANNEL_ID_1} and key {@code CHANNEL_KEY_1}</li>
     *     <li>of id {@code CHANNEL_ID_2} and key {@code CHANNEL_KEY_2}</li>
     * </ol>.
     */
    @BeforeClass
    public static void setup() {
        final List<Channel> channels = singletonList(getMockSupplyChannel(CHANNEL_ID_1, CHANNEL_KEY_1));
        channelsMap = ChannelsMap.Builder.of(channels)
            .add(getMockSupplyChannel(CHANNEL_ID_2, CHANNEL_KEY_2))
            .build();
    }

    @Test
    public void getChannelKey_ShouldReturnProperKey() {
        assertThat(channelsMap.getChannelKey(CHANNEL_ID_1)).isNotEmpty();
        assertThat(channelsMap.getChannelKey(CHANNEL_ID_1).get()).isEqualTo(CHANNEL_KEY_1);

        assertThat(channelsMap.getChannelKey(CHANNEL_ID_2)).isNotEmpty();
        assertThat(channelsMap.getChannelKey(CHANNEL_ID_2).get()).isEqualTo(CHANNEL_KEY_2);

        assertThat(channelsMap.getChannelKey("other")).isEmpty();
    }

    @Test
    public void getChannelId_ShouldReturnProperId() {
        assertThat(channelsMap.getChannelId(CHANNEL_KEY_1)).isNotEmpty();
        assertThat(channelsMap.getChannelId(CHANNEL_KEY_1).get()).isEqualTo(CHANNEL_ID_1);
        assertThat(channelsMap.getChannelId(CHANNEL_KEY_2)).isNotEmpty();
        assertThat(channelsMap.getChannelId(CHANNEL_KEY_2).get()).isEqualTo(CHANNEL_ID_2);

        assertThat(channelsMap.getChannelId("other")).isEmpty();
    }

    @Test
    public void build_ShouldNotShareBuildersMapsWithCreatedInstances() {
        final ChannelsMap.Builder builder = ChannelsMap.Builder.of(emptyList());

        builder.add(getMockSupplyChannel(CHANNEL_ID_1, CHANNEL_KEY_1));
        final ChannelsMap mapping = builder.build();
        assertThat(mapping.getChannelId(CHANNEL_KEY_2)).isEmpty();

        builder.add(getMockSupplyChannel(CHANNEL_ID_2, CHANNEL_KEY_2));
        assertThat(mapping.getChannelId(CHANNEL_KEY_2)).isEmpty();
    }
}
