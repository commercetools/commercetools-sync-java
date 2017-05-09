package com.commercetools.sync.categories;

import com.commercetools.sync.commons.helpers.CtpClient;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.client.SphereClientConfig;
import io.sphere.sdk.commands.UpdateAction;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CategorySyncOptionsBuilderTest {
    private final CtpClient ctpClient = mock(CtpClient.class);
    final CategorySyncOptionsBuilder categorySyncOptionsBuilder = CategorySyncOptionsBuilder.of(ctpClient);

    /**
     * Sets a mock {@code clientConfig} for an instance of {@link CtpClient} to be used across all the unit tests.
     */
    @Before
    public void setup() {
        final SphereClientConfig clientConfig = SphereClientConfig.of("testPK", "testCI", "testCS");
        when(ctpClient.getClientConfig()).thenReturn(clientConfig);
    }

    @Test
    public void of_WithClient_ShouldCreateCategorySyncOptionsBuilder() {
        final CategorySyncOptionsBuilder builder = CategorySyncOptionsBuilder.of(ctpClient);
        assertThat(builder).isNotNull();
    }

    @Test
    public void build_WithClient_ShouldBuildCategorySyncOptions() {
        final CategorySyncOptions categorySyncOptions = categorySyncOptionsBuilder.build();
        assertThat(categorySyncOptions).isNotNull();
        assertThat(categorySyncOptions.shouldRemoveOtherCollectionEntries()).isTrue();
        assertThat(categorySyncOptions.shouldRemoveOtherProperties()).isTrue();
        assertThat(categorySyncOptions.shouldRemoveOtherSetEntries()).isTrue();
        assertThat(categorySyncOptions.shouldRemoveOtherLocales()).isTrue();
        assertThat(categorySyncOptions.getUpdateActionsFilter()).isNull();
        assertThat(categorySyncOptions.getErrorCallBack()).isNull();
        assertThat(categorySyncOptions.getWarningCallBack()).isNull();
        assertThat(categorySyncOptions.getCtpClient()).isEqualTo(ctpClient);
    }

    @Test
    public void setUpdateActionsFilter_WithFilter_ShouldSetFilter() {
        final Function<List<UpdateAction<Category>>,
            List<UpdateAction<Category>>> clearListFilter = (updateActions -> Collections.emptyList());
        categorySyncOptionsBuilder.setUpdateActionsFilter(clearListFilter);

        final CategorySyncOptions categorySyncOptions = categorySyncOptionsBuilder.build();
        assertThat(categorySyncOptions.getUpdateActionsFilter()).isNotNull();
    }

    @Test
    public void setRemoveOtherCollectionEntries_WithFalse_ShouldSetFlag() {
        categorySyncOptionsBuilder.setRemoveOtherCollectionEntries(false);

        final CategorySyncOptions categorySyncOptions = categorySyncOptionsBuilder.build();
        assertThat(categorySyncOptions.shouldRemoveOtherCollectionEntries()).isNotNull();
        assertThat(categorySyncOptions.shouldRemoveOtherCollectionEntries()).isFalse();
    }

    @Test
    public void setRemoveOtherLocales_WithFalse_ShouldSetFlag() {
        categorySyncOptionsBuilder.setRemoveOtherLocales(false);

        final CategorySyncOptions categorySyncOptions = categorySyncOptionsBuilder.build();
        assertThat(categorySyncOptions.shouldRemoveOtherLocales()).isNotNull();
        assertThat(categorySyncOptions.shouldRemoveOtherLocales()).isFalse();
    }

    @Test
    public void setRemoveOtherProperties_WithFalse_ShouldSetFlag() {
        categorySyncOptionsBuilder.setRemoveOtherProperties(false);

        final CategorySyncOptions categorySyncOptions = categorySyncOptionsBuilder.build();
        assertThat(categorySyncOptions.shouldRemoveOtherProperties()).isNotNull();
        assertThat(categorySyncOptions.shouldRemoveOtherProperties()).isFalse();
    }

    @Test
    public void setRemoveOtherSetEntries_WithFalse_ShouldSetFlag() {
        categorySyncOptionsBuilder.setRemoveOtherSetEntries(false);

        final CategorySyncOptions categorySyncOptions = categorySyncOptionsBuilder.build();
        assertThat(categorySyncOptions.shouldRemoveOtherSetEntries()).isNotNull();
        assertThat(categorySyncOptions.shouldRemoveOtherSetEntries()).isFalse();
    }

    @Test
    public void setErrorCallBack_WithCallBack_ShouldSetCallBack() {
        final BiConsumer<String, Throwable> mockErrorCallBack = (errorMessage, errorException) -> {
        };
        categorySyncOptionsBuilder.setErrorCallBack(mockErrorCallBack);

        final CategorySyncOptions categorySyncOptions = categorySyncOptionsBuilder.build();
        assertThat(categorySyncOptions.getErrorCallBack()).isNotNull();
    }

    @Test
    public void setWarningCallBack_WithCallBack_ShouldSetCallBack() {
        final Consumer<String> mockWarningCallBack = (warningMessage) -> {
        };
        categorySyncOptionsBuilder.setWarningCallBack(mockWarningCallBack);

        final CategorySyncOptions categorySyncOptions = categorySyncOptionsBuilder.build();
        assertThat(categorySyncOptions.getWarningCallBack()).isNotNull();
    }

    @Test
    public void getThis_ShouldReturnCorrectInstance() {
        CategorySyncOptionsBuilder instance = categorySyncOptionsBuilder.getThis();
        assertThat(instance).isNotNull();
        assertThat(instance).isInstanceOf(CategorySyncOptionsBuilder.class);
        assertThat(instance).isEqualTo(categorySyncOptionsBuilder);
    }

    @Test
    public void categorySyncOptionsBuilderSetters_ShouldBeCallableAfterBaseSyncOptionsBuildSetters() {
        CategorySyncOptionsBuilder
            .of(ctpClient)
            .setRemoveOtherLocales(false)
            .setUpdateActionsFilter(updateActions -> Collections.emptyList())
            .build();
    }
}
