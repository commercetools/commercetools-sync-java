package com.commercetools.sync.categories;

import io.sphere.sdk.categories.Category;
import io.sphere.sdk.categories.commands.updateactions.ChangeName;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.models.LocalizedString;
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
        assertThat(categorySyncOptions.shouldAllowUuidKeys()).isFalse();
        assertThat(categorySyncOptions.getBeforeUpdateCallback()).isNull();
        assertThat(categorySyncOptions.getErrorCallBack()).isNull();
        assertThat(categorySyncOptions.getWarningCallBack()).isNull();
        assertThat(categorySyncOptions.getCtpClient()).isEqualTo(CTP_CLIENT);
        assertThat(categorySyncOptions.getBatchSize()).isEqualTo(CategorySyncOptionsBuilder.BATCH_SIZE_DEFAULT);
    }

    @Test
    public void beforeUpdateCallback_WithFilterAsCallback_ShouldSetCallback() {
        final Function<List<UpdateAction<Category>>,
            List<UpdateAction<Category>>> clearListFilter = (updateActions -> Collections.emptyList());
        categorySyncOptionsBuilder.beforeUpdateCallback(clearListFilter);

        final CategorySyncOptions categorySyncOptions = categorySyncOptionsBuilder.build();
        assertThat(categorySyncOptions.getBeforeUpdateCallback()).isNotNull();
    }

    @Test
    public void setRemoveOtherCollectionEntries_WithFalse_ShouldSetFlag() {
        categorySyncOptionsBuilder.removeOtherCollectionEntries(false);

        final CategorySyncOptions categorySyncOptions = categorySyncOptionsBuilder.build();
        assertThat(categorySyncOptions.shouldRemoveOtherCollectionEntries()).isNotNull();
        assertThat(categorySyncOptions.shouldRemoveOtherCollectionEntries()).isFalse();
    }

    @Test
    public void setRemoveOtherLocales_WithFalse_ShouldSetFlag() {
        categorySyncOptionsBuilder.removeOtherLocales(false);

        final CategorySyncOptions categorySyncOptions = categorySyncOptionsBuilder.build();
        assertThat(categorySyncOptions.shouldRemoveOtherLocales()).isNotNull();
        assertThat(categorySyncOptions.shouldRemoveOtherLocales()).isFalse();
    }

    @Test
    public void setRemoveOtherProperties_WithFalse_ShouldSetFlag() {
        categorySyncOptionsBuilder.removeOtherProperties(false);

        final CategorySyncOptions categorySyncOptions = categorySyncOptionsBuilder.build();
        assertThat(categorySyncOptions.shouldRemoveOtherProperties()).isNotNull();
        assertThat(categorySyncOptions.shouldRemoveOtherProperties()).isFalse();
    }

    @Test
    public void allowUuid_WithTrue_ShouldSetFlag() {
        categorySyncOptionsBuilder.allowUuidKeys(true);

        final CategorySyncOptions categorySyncOptions = categorySyncOptionsBuilder.build();
        assertThat(categorySyncOptions.shouldAllowUuidKeys()).isNotNull();
        assertThat(categorySyncOptions.shouldAllowUuidKeys()).isTrue();
    }

    @Test
    public void errorCallBack_WithCallBack_ShouldSetCallBack() {
        final BiConsumer<String, Throwable> mockErrorCallBack = (errorMessage, errorException) -> {
        };
        categorySyncOptionsBuilder.errorCallback(mockErrorCallBack);

        final CategorySyncOptions categorySyncOptions = categorySyncOptionsBuilder.build();
        assertThat(categorySyncOptions.getErrorCallBack()).isNotNull();
    }

    @Test
    public void warningCallBack_WithCallBack_ShouldSetCallBack() {
        final Consumer<String> mockWarningCallBack = (warningMessage) -> {
        };
        categorySyncOptionsBuilder.warningCallback(mockWarningCallBack);

        final CategorySyncOptions categorySyncOptions = categorySyncOptionsBuilder.build();
        assertThat(categorySyncOptions.getWarningCallBack()).isNotNull();
    }

    @Test
    public void setRemoveOtherSetEntries_WithFalse_ShouldSetFlag() {
        categorySyncOptionsBuilder.removeOtherSetEntries(false);

        final CategorySyncOptions categorySyncOptions = categorySyncOptionsBuilder.build();
        assertThat(categorySyncOptions.shouldRemoveOtherSetEntries()).isNotNull();
        assertThat(categorySyncOptions.shouldRemoveOtherSetEntries()).isFalse();
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
            .allowUuidKeys(true)
            .removeOtherLocales(false)
            .batchSize(30)
            .beforeUpdateCallback(updateActions -> Collections.emptyList())
            .build();
        assertThat(categorySyncOptions).isNotNull();
    }

    @Test
    public void setBatchSize_WithPositiveValue_ShouldSetBatchSize() {
        final CategorySyncOptions categorySyncOptions = CategorySyncOptionsBuilder.of(CTP_CLIENT)
                                                                                  .batchSize(10)
                                                                                  .build();
        assertThat(categorySyncOptions.getBatchSize()).isEqualTo(10);
    }

    @Test
    public void setBatchSize_WithZeroOrNegativeValue_ShouldFallBackToDefaultValue() {
        final CategorySyncOptions categorySyncOptionsWithZeroBatchSize = CategorySyncOptionsBuilder.of(CTP_CLIENT)
                                                                                  .batchSize(0)
                                                                                  .build();
        assertThat(categorySyncOptionsWithZeroBatchSize.getBatchSize())
            .isEqualTo(CategorySyncOptionsBuilder.BATCH_SIZE_DEFAULT);

        final CategorySyncOptions categorySyncOptionsWithNegativeBatchSize  = CategorySyncOptionsBuilder
            .of(CTP_CLIENT)
            .batchSize(-100)
            .build();
        assertThat(categorySyncOptionsWithNegativeBatchSize.getBatchSize())
            .isEqualTo(CategorySyncOptionsBuilder.BATCH_SIZE_DEFAULT);
    }

    @Test
    public void applyBeforeUpdateCallBack_WithNullCallback_ShouldReturnIdenticalList() {
        final CategorySyncOptions categorySyncOptions = CategorySyncOptionsBuilder.of(CTP_CLIENT)
                                                                                  .build();
        assertThat(categorySyncOptions.getBeforeUpdateCallback()).isNull();

        final List<UpdateAction<Category>> updateActions = Collections
            .singletonList(ChangeName.of(LocalizedString.ofEnglish("name")));
        final List<UpdateAction<Category>> filteredList = categorySyncOptions
            .applyBeforeUpdateCallBack(updateActions);
        assertThat(filteredList).isSameAs(updateActions);
    }

    @Test
    public void applyBeforeUpdateCallBack_WithCallback_ShouldReturnFilteredList() {
        final CategorySyncOptions categorySyncOptions = CategorySyncOptionsBuilder.of(CTP_CLIENT)
                                                                                  .beforeUpdateCallback(list ->
                                                                                      Collections.emptyList())
                                                                                  .build();
        assertThat(categorySyncOptions.getBeforeUpdateCallback()).isNotNull();

        final List<UpdateAction<Category>> updateActions = Collections
            .singletonList(ChangeName.of(LocalizedString.ofEnglish("name")));
        final List<UpdateAction<Category>> filteredList = categorySyncOptions
            .applyBeforeUpdateCallBack(updateActions);
        assertThat(filteredList).isNotEqualTo(updateActions);
        assertThat(filteredList).isEmpty();
    }
}
