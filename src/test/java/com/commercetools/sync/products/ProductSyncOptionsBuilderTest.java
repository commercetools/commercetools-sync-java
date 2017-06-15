package com.commercetools.sync.products;

import io.sphere.sdk.client.SphereClient;
import org.junit.Test;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import static java.util.Collections.singletonList;
import static java.util.function.Function.identity;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@SuppressWarnings("unchecked")
public class ProductSyncOptionsBuilderTest {

    @Test
    public void of_default() {
        SphereClient client = mock(SphereClient.class);
        BiConsumer errorCallBack = mock(BiConsumer.class);
        Consumer warningCallBack = mock(Consumer.class);

        ProductSyncOptions options = ProductSyncOptionsBuilder
                .of(client, errorCallBack, warningCallBack)
                .build();

        assertThat(options.getCtpClient()).isSameAs(client);
        assertThat(options.getActionsFilter()).isEqualTo(identity());
        assertThat(options.getBlackList()).isEmpty();
        assertThat(options.getWhiteList()).isEmpty();
        assertThat(options.shouldUpdateStaged()).isTrue();
        assertThat(options.shouldPublish()).isFalse();
        assertThat(options.shouldRevertStagedChanges()).isFalse();
        assertThat(options.shouldRemoveOtherVariants()).isTrue();

        assertThat(options.getErrorCallBack()).isSameAs(errorCallBack);
        assertThat(options.getWarningCallBack()).isSameAs(warningCallBack);
        assertThat(options.shouldRemoveOtherLocales()).isTrue();
        assertThat(options.shouldRemoveOtherSetEntries()).isTrue();
        assertThat(options.shouldRemoveOtherCollectionEntries()).isTrue();
        assertThat(options.shouldRemoveOtherProperties()).isTrue();
    }

    @Test
    public void of() {
        SphereClient client = mock(SphereClient.class);
        BiConsumer errorCallBack = mock(BiConsumer.class);
        Consumer warningCallBack = mock(Consumer.class);
        Function actionsFilter = mock(Function.class);

        ProductSyncOptions options = ProductSyncOptionsBuilder
                .of(client, errorCallBack, warningCallBack)
                .updateStaged(false)
                .publish(true)
                .revertStagedChanges(true)
                .removeOtherVariants(false)
                .whiteList(singletonList("white1"))
                .blackList(singletonList("black1"))
                .actionsFilter(actionsFilter)
                .setRemoveOtherLocales(false)
                .setRemoveOtherSetEntries(false)
                .setRemoveOtherCollectionEntries(false)
                .setRemoveOtherProperties(false)
                .build();

        assertThat(options.getCtpClient()).isSameAs(client);
        assertThat(options.getActionsFilter()).isSameAs(actionsFilter);
        assertThat(options.getWhiteList()).isEqualTo(singletonList("white1"));
        assertThat(options.getBlackList()).isEqualTo(singletonList("black1"));
        assertThat(options.shouldUpdateStaged()).isFalse();
        assertThat(options.shouldPublish()).isTrue();
        assertThat(options.shouldRevertStagedChanges()).isTrue();
        assertThat(options.shouldRemoveOtherVariants()).isFalse();

        assertThat(options.getErrorCallBack()).isSameAs(errorCallBack);
        assertThat(options.getWarningCallBack()).isSameAs(warningCallBack);
        assertThat(options.shouldRemoveOtherLocales()).isFalse();
        assertThat(options.shouldRemoveOtherSetEntries()).isFalse();
        assertThat(options.shouldRemoveOtherCollectionEntries()).isFalse();
        assertThat(options.shouldRemoveOtherProperties()).isFalse();
    }
}
