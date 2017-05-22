package com.commercetools.sync.categories;

import io.sphere.sdk.categories.Category;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.commands.UpdateAction;
import org.junit.Test;

import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class CategorySyncOptionsBuilderTest {
    private static final SphereClient CTP_CLIENT = mock(SphereClient.class);
    private CategorySyncOptionsBuilder categorySyncOptionsBuilder = CategorySyncOptionsBuilder.of(CTP_CLIENT);

    @Test
    public void of_WithClient_ShouldCreateCategorySyncOptionsBuilder() {
        final CategorySyncOptionsBuilder builder = CategorySyncOptionsBuilder.of(CTP_CLIENT);
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
        assertThat(categorySyncOptions.getCtpClient()).isEqualTo(CTP_CLIENT);
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
        final CategorySyncOptionsBuilder instance = categorySyncOptionsBuilder.getThis();
        assertThat(instance).isNotNull();
        assertThat(instance).isInstanceOf(CategorySyncOptionsBuilder.class);
        assertThat(instance).isEqualTo(categorySyncOptionsBuilder);
    }

    @Test
    public void categorySyncOptionsBuilderSetters_ShouldBeCallableAfterBaseSyncOptionsBuildSetters() {
        final CategorySyncOptions categorySyncOptions = CategorySyncOptionsBuilder
            .of(CTP_CLIENT)
            .setRemoveOtherLocales(false)
            .setUpdateActionsFilter(updateActions -> Collections.emptyList())
            .build();
        assertThat(categorySyncOptions).isNotNull();
    }
}
