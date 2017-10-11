package com.commercetools.sync.integration.commons.utils;

import com.commercetools.sync.commons.utils.CtpQueryUtils;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.taxcategories.TaxCategory;
import io.sphere.sdk.taxcategories.commands.TaxCategoryDeleteCommand;
import io.sphere.sdk.taxcategories.queries.TaxCategoryQuery;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_SOURCE_CLIENT;
import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_TARGET_CLIENT;

public class TaxCategoryITUtils {

    /**
     * Deletes all Tax categories from CTP projects defined by the {@code CTP_SOURCE_CLIENT} and
     * {@code CTP_TARGET_CLIENT}.
     */
    public static void deleteTaxCategoriesFromTargetAndSource() {
        deleteTaxCategories(CTP_TARGET_CLIENT);
        deleteTaxCategories(CTP_SOURCE_CLIENT);
    }

    /**
     * Deletes all tax categories from the CTP project defined by the {@code ctpClient}.
     *
     * @param ctpClient defines the CTP project to delete the tax categories from.
     */
    public static void deleteTaxCategories(@Nonnull final SphereClient ctpClient) {
        final List<CompletableFuture> taxCategoryDeleteFutures = new ArrayList<>();

        final Consumer<List<TaxCategory>> taxCategoryPageDelete = taxCategories ->
            taxCategories.forEach(taxCategory -> {
                final CompletableFuture<TaxCategory> deleteFuture =
                    ctpClient.execute(TaxCategoryDeleteCommand.of(taxCategory)).toCompletableFuture();
                taxCategoryDeleteFutures.add(deleteFuture);
            });

        CtpQueryUtils.queryAll(ctpClient, TaxCategoryQuery.of(), taxCategoryPageDelete)
                     .thenCompose(result -> CompletableFuture
                         .allOf(taxCategoryDeleteFutures
                             .toArray(new CompletableFuture[taxCategoryDeleteFutures.size()])))
                     .toCompletableFuture().join();
    }
}
