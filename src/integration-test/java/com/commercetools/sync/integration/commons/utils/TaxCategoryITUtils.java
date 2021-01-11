package com.commercetools.sync.integration.commons.utils;

import static com.commercetools.sync.integration.commons.utils.ITUtils.queryAndExecute;
import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_SOURCE_CLIENT;
import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_TARGET_CLIENT;
import static com.commercetools.tests.utils.CompletionStageUtil.executeBlocking;
import static java.util.Collections.singletonList;

import com.neovisionaries.i18n.CountryCode;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.taxcategories.TaxCategory;
import io.sphere.sdk.taxcategories.TaxCategoryDraft;
import io.sphere.sdk.taxcategories.TaxCategoryDraftBuilder;
import io.sphere.sdk.taxcategories.TaxRateDraft;
import io.sphere.sdk.taxcategories.TaxRateDraftBuilder;
import io.sphere.sdk.taxcategories.commands.TaxCategoryCreateCommand;
import io.sphere.sdk.taxcategories.commands.TaxCategoryDeleteCommand;
import io.sphere.sdk.taxcategories.queries.TaxCategoryQuery;
import io.sphere.sdk.taxcategories.queries.TaxCategoryQueryBuilder;
import java.util.Optional;
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
  public static void deleteTaxCategories(@Nonnull final SphereClient ctpClient) {
    queryAndExecute(ctpClient, TaxCategoryQuery.of(), TaxCategoryDeleteCommand::of);
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
  public static TaxCategory createTaxCategory(@Nonnull final SphereClient ctpClient) {
    final TaxCategoryDraft taxCategoryDraft =
        TaxCategoryDraftBuilder.of(
                TAXCATEGORY_NAME, singletonList(createTaxRateDraft()), TAXCATEGORY_DESCRIPTION)
            .key(TAXCATEGORY_KEY)
            .build();
    return executeBlocking(ctpClient.execute(TaxCategoryCreateCommand.of(taxCategoryDraft)));
  }

  /**
   * Creates a {@link TaxRateDraft} with the name {@value TAXCATEGORY_TAXRATE_NAME} and amount
   * {@value TAXCATEGORY_TAXRATE_AMOUNT}.
   *
   * @return the created tax rate draft.
   */
  public static TaxRateDraft createTaxRateDraft() {
    return TaxRateDraftBuilder.of(
            TAXCATEGORY_TAXRATE_NAME, TAXCATEGORY_TAXRATE_AMOUNT, true, CountryCode.DE)
        .build();
  }

  public static Optional<TaxCategory> getTaxCategoryByKey(
      @Nonnull final SphereClient sphereClient, @Nonnull final String key) {
    final TaxCategoryQuery query =
        TaxCategoryQueryBuilder.of().plusPredicates(queryModel -> queryModel.key().is(key)).build();
    return sphereClient.execute(query).toCompletableFuture().join().head();
  }

  private TaxCategoryITUtils() {}
}
