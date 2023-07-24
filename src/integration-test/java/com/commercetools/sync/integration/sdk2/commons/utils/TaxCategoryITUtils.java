package com.commercetools.sync.integration.sdk2.commons.utils;

import static com.commercetools.sync.integration.sdk2.commons.utils.TestClientUtils.CTP_SOURCE_CLIENT;
import static com.commercetools.sync.integration.sdk2.commons.utils.TestClientUtils.CTP_TARGET_CLIENT;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.client.QueryUtils;
import com.commercetools.api.models.tax_category.TaxCategory;
import com.commercetools.api.models.tax_category.TaxCategoryDraft;
import com.commercetools.api.models.tax_category.TaxCategoryDraftBuilder;
import com.commercetools.api.models.tax_category.TaxRateDraft;
import com.commercetools.api.models.tax_category.TaxRateDraftBuilder;
import io.vrap.rmf.base.client.ApiHttpResponse;
import io.vrap.rmf.base.client.error.NotFoundException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;
import javax.annotation.Nonnull;

public final class TaxCategoryITUtils {
  public static final String TAXCATEGORY_KEY = "old_tax_category_key";
  private static final String TAXCATEGORY_NAME = "old_tax_category_name";
  private static final String TAXCATEGORY_DESCRIPTION = "old_tax_category_desc";
  private static final String TAXCATEGORY_TAXRATE_NAME = "old_tax_rate_name";
  private static final double TAXCATEGORY_TAXRATE_AMOUNT = 0.2;

  public static final String TAXCATEGORY_KEY_1 = "key_1";
  public static final String TAXCATEGORY_NAME_1 = "name_1";
  public static final String TAXCATEGORY_DESCRIPTION_1 = "description_1";

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
  public static void deleteTaxCategories(@Nonnull final ProjectApiRoot ctpClient) {
    final Consumer<List<TaxCategory>> taxCategoryConsumer =
        taxCategories -> {
          CompletableFuture.allOf(
                  taxCategories.stream()
                      .map(taxCategory -> deleteTaxCategoryWithRetry(ctpClient, taxCategory))
                      .map(CompletionStage::toCompletableFuture)
                      .toArray(CompletableFuture[]::new))
              .join();
        };
    QueryUtils.queryAll(ctpClient.taxCategories().get(), taxCategoryConsumer)
        .handle(
            (result, throwable) -> {
              if (throwable != null && !(throwable instanceof NotFoundException)) {
                return throwable;
              }
              return result;
            })
        .toCompletableFuture()
        .join();
  }

  private static CompletionStage<TaxCategory> deleteTaxCategoryWithRetry(
      @Nonnull final ProjectApiRoot ctpClient, @Nonnull final TaxCategory taxCategory) {
    return ctpClient
        .taxCategories()
        .delete(taxCategory)
        .execute()
        .thenApply(ApiHttpResponse::getBody);
  }

  /**
   * Creates a {@link TaxCategory} in the CTP project defined by the {@code ctpClient} in a blocking
   * fashion. The created tax category will have a key with the value {@value TAXCATEGORY_KEY}, a
   * name with the value {@value TAXCATEGORY_NAME}, a description with the value {@value
   * TAXCATEGORY_DESCRIPTION} and a tax rate with the name {@value TAXCATEGORY_TAXRATE_NAME} and
   * amount {@value TAXCATEGORY_TAXRATE_AMOUNT}.
   *
   * @param ctpClient defines the CTP project to create the tax category in.
   * @return the created tax category.
   */
  public static TaxCategory ensureTaxCategory(@Nonnull final ProjectApiRoot ctpClient) {
    return taxCategoryExists(ctpClient, TAXCATEGORY_KEY)
        .thenCompose(
            taxCategory ->
                taxCategory
                    .map(CompletableFuture::completedFuture)
                    .orElseGet(
                        () -> {
                          final TaxCategoryDraft taxCategoryDraft =
                              TaxCategoryDraftBuilder.of()
                                  .name(TAXCATEGORY_NAME)
                                  .rates(mockTaxRateDraft())
                                  .description(TAXCATEGORY_DESCRIPTION)
                                  .key(TAXCATEGORY_KEY)
                                  .build();
                          return createTaxCategory(taxCategoryDraft, ctpClient);
                        }))
        .toCompletableFuture()
        .join();
  }

  private static CompletionStage<Optional<TaxCategory>> taxCategoryExists(
      @Nonnull ProjectApiRoot ctpClient, @Nonnull String taxCategoryKey) {
    return ctpClient
        .taxCategories()
        .withKey(taxCategoryKey)
        .get()
        .execute()
        .handle(
            (typeApiHttpResponse, throwable) -> {
              if (throwable != null) {
                return Optional.empty();
              }
              return Optional.of(typeApiHttpResponse.getBody().get());
            });
  }

  /**
   * Creates a {@link TaxRateDraft} with the name {@value TAXCATEGORY_TAXRATE_NAME} and amount
   * {@value TAXCATEGORY_TAXRATE_AMOUNT}.
   *
   * @return the created tax rate draft.
   */
  public static TaxRateDraft mockTaxRateDraft() {
    return TaxRateDraftBuilder.of()
        .name(TAXCATEGORY_TAXRATE_NAME)
        .amount(TAXCATEGORY_TAXRATE_AMOUNT)
        .country("DE")
        .includedInPrice(true)
        .build();
  }

  static CompletableFuture<TaxCategory> createTaxCategory(
      @Nonnull final TaxCategoryDraft taxCategoryDraft, @Nonnull final ProjectApiRoot ctpClient) {
    return ctpClient
        .taxCategories()
        .post(taxCategoryDraft)
        .execute()
        .thenApply(ApiHttpResponse::getBody);
  }

  public static TaxCategory createTaxCategoryByDraft(
      @Nonnull final TaxCategoryDraft taxCategoryDraft, @Nonnull final ProjectApiRoot ctpClient) {
    return createTaxCategory(taxCategoryDraft, ctpClient).toCompletableFuture().join();
  }

  public static Optional<TaxCategory> getTaxCategoryByKey(
      @Nonnull final ProjectApiRoot ctpClient, @Nonnull final String key) {
    return Optional.of(ctpClient.taxCategories().withKey(key).get().execute().join().getBody());
  }

  private TaxCategoryITUtils() {}
}
