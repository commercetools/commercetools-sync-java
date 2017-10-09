package com.commercetools.sync.commons.utils;

import com.commercetools.sync.products.SyncFilter;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.products.Image;
import io.sphere.sdk.products.ImageDimensions;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.commands.updateactions.AddExternalImage;
import io.sphere.sdk.products.commands.updateactions.RemoveImage;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static com.commercetools.sync.commons.utils.FilterUtils.executeSupplierIfPassesFilter;
import static com.commercetools.sync.products.ActionGroup.CATEGORIES;
import static com.commercetools.sync.products.ActionGroup.IMAGES;
import static com.commercetools.sync.products.ActionGroup.PRICES;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

public class FilterUtilsTest {

    private  final  List<UpdateAction<Product>> passingUpdateActions = Arrays.asList(
        AddExternalImage.of(Image.of("url2", ImageDimensions.of(10, 10)), 0),
        AddExternalImage.of(Image.of("url2", ImageDimensions.of(10, 10)), 0)
    );

    private static final List<UpdateAction<Product>>  defaultActions =
        singletonList(RemoveImage.of("anyUrl", 0));

    @Test
    public void executeSupplierIfPassesFilter_WithGroupInBlackList_ShouldFilterOutOnlyThisGroup() {
        final SyncFilter syncFilter = SyncFilter.ofBlackList(IMAGES);

        final List<UpdateAction<Product>> updateActionsAfterImagesFilter =
            executeSupplierIfPassesFilter(syncFilter, IMAGES, () -> passingUpdateActions , () -> defaultActions);
        assertThat(updateActionsAfterImagesFilter).hasSize(1);
        assertThat(updateActionsAfterImagesFilter).isSameAs(defaultActions);

        final List<UpdateAction<Product>> updateActionsAfterPricesFilter =
            executeSupplierIfPassesFilter(syncFilter, PRICES, () -> passingUpdateActions, () -> defaultActions);
        assertThat(updateActionsAfterPricesFilter).hasSize(2);
        assertThat(updateActionsAfterPricesFilter).isSameAs(passingUpdateActions);

        final List<UpdateAction<Product>> updateActionsAfterCategoriesFilter =
            executeSupplierIfPassesFilter(syncFilter, CATEGORIES, () -> passingUpdateActions, () -> defaultActions);
        assertThat(updateActionsAfterCategoriesFilter).hasSize(2);
        assertThat(updateActionsAfterCategoriesFilter).isSameAs(passingUpdateActions);
    }

    @Test
    public void executeSupplierIfPassesFilter_WithGroupNotInBlackList_ShouldFilterInThisGroup() {
        final SyncFilter syncFilter = SyncFilter.ofBlackList(PRICES);
        final List<UpdateAction<Product>> updateActionsAfterImagesFilter =
            executeSupplierIfPassesFilter(syncFilter, IMAGES, () -> passingUpdateActions , () -> defaultActions);
        assertThat(updateActionsAfterImagesFilter).hasSize(2);
        assertThat(updateActionsAfterImagesFilter).isSameAs(passingUpdateActions);

        final List<UpdateAction<Product>> updateActionsAfterPricesFilter =
            executeSupplierIfPassesFilter(syncFilter, PRICES, () -> passingUpdateActions, () -> defaultActions);
        assertThat(updateActionsAfterPricesFilter).hasSize(1);
        assertThat(updateActionsAfterPricesFilter).isSameAs(defaultActions);
    }

    @Test
    public void executeSupplierIfPassesFilter_WithGroupInWhiteList_ShouldFilterInOnlyThisGroup() {
        final SyncFilter syncFilter = SyncFilter.ofWhiteList(PRICES);

        final List<UpdateAction<Product>> updateActionsAfterPricesFilter =
            executeSupplierIfPassesFilter(syncFilter, PRICES, () -> passingUpdateActions, () -> defaultActions);
        assertThat(updateActionsAfterPricesFilter).hasSize(2);
        assertThat(updateActionsAfterPricesFilter).isSameAs(passingUpdateActions);

        final List<UpdateAction<Product>> updateActionsAfterImagesFilter =
            executeSupplierIfPassesFilter(syncFilter, IMAGES, () -> passingUpdateActions , () -> defaultActions);
        assertThat(updateActionsAfterImagesFilter).hasSize(1);
        assertThat(updateActionsAfterImagesFilter).isSameAs(defaultActions);

        final List<UpdateAction<Product>> updateActionsAfterCategoriesFilter =
            executeSupplierIfPassesFilter(syncFilter, CATEGORIES, () -> passingUpdateActions, () -> defaultActions);
        assertThat(updateActionsAfterCategoriesFilter).hasSize(1);
        assertThat(updateActionsAfterCategoriesFilter).isSameAs(defaultActions);
    }

    @Test
    public void executeSupplierIfPassesFilter_WithDefault_ShouldFilterInEveryThing() {
        final SyncFilter syncFilter = SyncFilter.of();

        final List<UpdateAction<Product>> updateActionsAfterPricesFilter =
            executeSupplierIfPassesFilter(syncFilter, PRICES, () -> passingUpdateActions, () -> defaultActions);
        assertThat(updateActionsAfterPricesFilter).hasSize(2);
        assertThat(updateActionsAfterPricesFilter).isSameAs(passingUpdateActions);

        final List<UpdateAction<Product>> updateActionsAfterImagesFilter =
            executeSupplierIfPassesFilter(syncFilter, IMAGES, () -> passingUpdateActions , () -> defaultActions);
        assertThat(updateActionsAfterImagesFilter).hasSize(2);
        assertThat(updateActionsAfterImagesFilter).isSameAs(passingUpdateActions);

        final List<UpdateAction<Product>> updateActionsAfterCategoriesFilter =
            executeSupplierIfPassesFilter(syncFilter, CATEGORIES, () -> passingUpdateActions, () -> defaultActions);
        assertThat(updateActionsAfterCategoriesFilter).hasSize(2);
        assertThat(updateActionsAfterCategoriesFilter).isSameAs(passingUpdateActions);
    }
}
