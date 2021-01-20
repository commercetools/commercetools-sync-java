package com.commercetools.sync.integration.externalsource.taxcategories;

import static com.commercetools.sync.commons.asserts.statistics.AssertionsForStatistics.assertThat;
import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_TARGET_CLIENT;
import static com.commercetools.sync.integration.commons.utils.TaxCategoryITUtils.deleteTaxCategories;
import static com.commercetools.sync.integration.commons.utils.TaxCategoryITUtils.getTaxCategoryByKey;
import static com.commercetools.tests.utils.CompletionStageUtil.executeBlocking;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import com.commercetools.sync.taxcategories.TaxCategorySync;
import com.commercetools.sync.taxcategories.TaxCategorySyncOptions;
import com.commercetools.sync.taxcategories.TaxCategorySyncOptionsBuilder;
import com.commercetools.sync.taxcategories.helpers.TaxCategorySyncStatistics;
import com.neovisionaries.i18n.CountryCode;
import io.sphere.sdk.client.BadGatewayException;
import io.sphere.sdk.client.ConcurrentModificationException;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.queries.PagedQueryResult;
import io.sphere.sdk.taxcategories.SubRate;
import io.sphere.sdk.taxcategories.TaxCategory;
import io.sphere.sdk.taxcategories.TaxCategoryDraft;
import io.sphere.sdk.taxcategories.TaxCategoryDraftBuilder;
import io.sphere.sdk.taxcategories.TaxRate;
import io.sphere.sdk.taxcategories.TaxRateDraft;
import io.sphere.sdk.taxcategories.TaxRateDraftBuilder;
import io.sphere.sdk.taxcategories.commands.TaxCategoryCreateCommand;
import io.sphere.sdk.taxcategories.commands.TaxCategoryUpdateCommand;
import io.sphere.sdk.taxcategories.queries.TaxCategoryQuery;
import io.sphere.sdk.utils.CompletableFutureUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nonnull;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TaxCategorySyncIT {

  @BeforeEach
  void setup() {
    deleteTaxCategories(CTP_TARGET_CLIENT);

    final SubRate subRate1 = SubRate.of("subRate-1", 0.08);
    final SubRate subRate2 = SubRate.of("subRate-2", 0.11);

    final TaxRateDraft taxRateDraft =
        TaxRateDraftBuilder.of("%19 VAT DE", 0.19, false, CountryCode.DE)
            .subRates(asList(subRate1, subRate2))
            .build();

    final TaxCategoryDraft taxCategoryDraft =
        TaxCategoryDraftBuilder.of(
                "tax-category-name", singletonList(taxRateDraft), "tax-category-description")
            .key("tax-category-key")
            .build();

    executeBlocking(CTP_TARGET_CLIENT.execute(TaxCategoryCreateCommand.of(taxCategoryDraft)));
  }

  @AfterAll
  static void tearDown() {
    deleteTaxCategories(CTP_TARGET_CLIENT);
  }

  @Test
  void sync_withNewTaxCategory_shouldCreateTaxCategory() {
    final SubRate subRate1 = SubRate.of("subRate-1", 0.05);
    final SubRate subRate2 = SubRate.of("subRate-2", 0.06);

    final TaxRateDraft taxRateDraft =
        TaxRateDraftBuilder.of("%11 US", 0.11, false, CountryCode.US)
            .subRates(asList(subRate1, subRate2))
            .build();

    final TaxCategoryDraft taxCategoryDraft =
        TaxCategoryDraftBuilder.of(
                "tax-category-name-new",
                singletonList(taxRateDraft),
                "tax-category-description-new")
            .key("tax-category-key-new")
            .build();

    final TaxCategorySyncOptions taxCategorySyncOptions =
        TaxCategorySyncOptionsBuilder.of(CTP_TARGET_CLIENT).build();

    final TaxCategorySync taxCategorySync = new TaxCategorySync(taxCategorySyncOptions);

    // test
    final TaxCategorySyncStatistics taxCategorySyncStatistics =
        taxCategorySync.sync(singletonList(taxCategoryDraft)).toCompletableFuture().join();

    assertThat(taxCategorySyncStatistics).hasValues(1, 1, 0, 0);
  }

  @Test
  void sync_WithUpdatedTaxCategory_ShouldUpdateTaxCategory() {
    // preparation
    final SubRate subRate1 = SubRate.of("subRate-1", 0.07);
    final SubRate subRate2 = SubRate.of("subRate-2", 0.09);

    final TaxRateDraft taxRateDraft =
        TaxRateDraftBuilder.of("%16 VAT", 0.16, true, CountryCode.DE)
            .subRates(asList(subRate1, subRate2))
            .build();

    final TaxCategoryDraft taxCategoryDraft =
        TaxCategoryDraftBuilder.of(
                "tax-category-name-updated",
                singletonList(taxRateDraft),
                "tax-category-description-updated")
            .key("tax-category-key")
            .build();

    final TaxCategorySyncOptions taxCategorySyncOptions =
        TaxCategorySyncOptionsBuilder.of(CTP_TARGET_CLIENT).build();

    final TaxCategorySync taxCategorySync = new TaxCategorySync(taxCategorySyncOptions);

    // test
    final TaxCategorySyncStatistics taxCategorySyncStatistics =
        taxCategorySync.sync(singletonList(taxCategoryDraft)).toCompletableFuture().join();

    // assertion
    assertThat(taxCategorySyncStatistics).hasValues(1, 0, 1, 0);

    final Optional<TaxCategory> oldTaxCategoryAfter =
        getTaxCategoryByKey(CTP_TARGET_CLIENT, "tax-category-key");

    Assertions.assertThat(oldTaxCategoryAfter)
        .hasValueSatisfying(
            taxCategory -> {
              Assertions.assertThat(taxCategory.getName()).isEqualTo("tax-category-name-updated");
              Assertions.assertThat(taxCategory.getDescription())
                  .isEqualTo("tax-category-description-updated");
              final TaxRate taxRate = taxCategory.getTaxRates().get(0);
              Assertions.assertThat(taxRate.getName()).isEqualTo("%16 VAT");
              Assertions.assertThat(taxRate.getAmount()).isEqualTo(0.16);
              Assertions.assertThat(taxRate.getCountry()).isEqualTo(CountryCode.DE);
              Assertions.assertThat(taxRate.isIncludedInPrice()).isEqualTo(true);
              Assertions.assertThat(taxRate.getSubRates()).isEqualTo(asList(subRate1, subRate2));
            });
  }

  @Test
  void sync_withEqualTaxCategory_shouldNotUpdateTaxCategory() {
    final SubRate subRate1 = SubRate.of("subRate-1", 0.08);
    final SubRate subRate2 = SubRate.of("subRate-2", 0.11);

    final TaxRateDraft taxRateDraft =
        TaxRateDraftBuilder.of("%19 VAT DE", 0.19, false, CountryCode.DE)
            .subRates(asList(subRate1, subRate2))
            .build();

    final TaxCategoryDraft taxCategoryDraft =
        TaxCategoryDraftBuilder.of(
                "tax-category-name", singletonList(taxRateDraft), "tax-category-description")
            .key("tax-category-key")
            .build();

    final TaxCategorySyncOptions taxCategorySyncOptions =
        TaxCategorySyncOptionsBuilder.of(CTP_TARGET_CLIENT).build();

    final TaxCategorySync taxCategorySync = new TaxCategorySync(taxCategorySyncOptions);

    // test
    final TaxCategorySyncStatistics taxCategorySyncStatistics =
        taxCategorySync.sync(singletonList(taxCategoryDraft)).toCompletableFuture().join();

    assertThat(taxCategorySyncStatistics).hasValues(1, 0, 0, 0);
  }

  @Test
  void
      sync_withChangedTaxCategoryButConcurrentModificationException_shouldRetryAndUpdateTaxCategory() {
    // preparation
    final SphereClient spyClient = buildClientWithConcurrentModificationUpdate();

    List<String> errorCallBackMessages = new ArrayList<>();
    List<String> warningCallBackMessages = new ArrayList<>();
    List<Throwable> errorCallBackExceptions = new ArrayList<>();
    final TaxCategorySyncOptions spyOptions =
        TaxCategorySyncOptionsBuilder.of(spyClient)
            .errorCallback(
                (exception, oldResource, newResource, updateActions) -> {
                  errorCallBackMessages.add(exception.getMessage());
                  errorCallBackExceptions.add(exception.getCause());
                })
            .build();

    final TaxCategorySync taxCategorySync = new TaxCategorySync(spyOptions);

    final TaxCategoryDraft taxCategoryDraft =
        TaxCategoryDraftBuilder.of(
                "tax-category-name-updated", null, "tax-category-description-updated")
            .key("tax-category-key")
            .build();

    // test
    final TaxCategorySyncStatistics taxCategorySyncStatistics =
        taxCategorySync.sync(singletonList(taxCategoryDraft)).toCompletableFuture().join();

    assertThat(taxCategorySyncStatistics).hasValues(1, 0, 1, 0);
    Assertions.assertThat(errorCallBackExceptions).isEmpty();
    Assertions.assertThat(errorCallBackMessages).isEmpty();
    Assertions.assertThat(warningCallBackMessages).isEmpty();
  }

  @Nonnull
  private SphereClient buildClientWithConcurrentModificationUpdate() {
    final SphereClient spyClient = spy(CTP_TARGET_CLIENT);

    final TaxCategoryUpdateCommand updateCommand = any(TaxCategoryUpdateCommand.class);
    when(spyClient.execute(updateCommand))
        .thenReturn(
            CompletableFutureUtils.exceptionallyCompletedFuture(
                new ConcurrentModificationException()))
        .thenCallRealMethod();

    return spyClient;
  }

  @Test
  void sync_WithConcurrentModificationExceptionAndFailedFetch_ShouldFailToReFetchAndUpdate() {
    // preparation
    final SphereClient spyClient =
        buildClientWithConcurrentModificationUpdateAndFailedFetchOnRetry();

    List<String> errorCallBackMessages = new ArrayList<>();
    List<Throwable> errorCallBackExceptions = new ArrayList<>();
    final TaxCategorySyncOptions spyOptions =
        TaxCategorySyncOptionsBuilder.of(spyClient)
            .errorCallback(
                (exception, oldResource, newResource, updateActions) -> {
                  errorCallBackMessages.add(exception.getMessage());
                  errorCallBackExceptions.add(exception.getCause());
                })
            // .warningCallback(warningCallBackMessages::add)
            .build();

    final TaxCategorySync taxCategorySync = new TaxCategorySync(spyOptions);

    final TaxCategoryDraft taxCategoryDraft =
        TaxCategoryDraftBuilder.of(
                "tax-category-name-updated", null, "tax-category-description-updated")
            .key("tax-category-key")
            .build();

    // test
    final TaxCategorySyncStatistics taxCategorySyncStatistics =
        taxCategorySync.sync(singletonList(taxCategoryDraft)).toCompletableFuture().join();

    // Test and assertion
    assertThat(taxCategorySyncStatistics).hasValues(1, 0, 0, 1);
    Assertions.assertThat(errorCallBackMessages).hasSize(1);
    Assertions.assertThat(errorCallBackExceptions).hasSize(1);

    Assertions.assertThat(errorCallBackExceptions.get(0).getCause())
        .isExactlyInstanceOf(BadGatewayException.class);
    Assertions.assertThat(errorCallBackMessages.get(0))
        .contains(
            format(
                "Failed to update tax category with key: '%s'. Reason: Failed to fetch from CTP while retrying "
                    + "after concurrency modification.",
                taxCategoryDraft.getKey()));
  }

  @Nonnull
  private SphereClient buildClientWithConcurrentModificationUpdateAndFailedFetchOnRetry() {
    final SphereClient spyClient = spy(CTP_TARGET_CLIENT);

    final TaxCategoryUpdateCommand updateCommand = any(TaxCategoryUpdateCommand.class);
    when(spyClient.execute(updateCommand))
        .thenReturn(
            CompletableFutureUtils.exceptionallyCompletedFuture(
                new ConcurrentModificationException()))
        .thenCallRealMethod();

    final TaxCategoryQuery taxCategoryQuery = any(TaxCategoryQuery.class);
    when(spyClient.execute(taxCategoryQuery))
        .thenCallRealMethod() // Call real fetch on fetching matching tax categories
        .thenReturn(CompletableFutureUtils.exceptionallyCompletedFuture(new BadGatewayException()));

    return spyClient;
  }

  @Test
  void sync_WithConcurrentModificationExceptionAndUnexpectedDelete_ShouldFailToReFetchAndUpdate() {
    // preparation
    final SphereClient spyClient =
        buildClientWithConcurrentModificationUpdateAndNotFoundFetchOnRetry();

    List<String> errorCallBackMessages = new ArrayList<>();
    List<Throwable> errorCallBackExceptions = new ArrayList<>();
    final TaxCategorySyncOptions spyOptions =
        TaxCategorySyncOptionsBuilder.of(spyClient)
            .errorCallback(
                (exception, oldResource, newResource, updateActions) -> {
                  errorCallBackMessages.add(exception.getMessage());
                  errorCallBackExceptions.add(exception.getCause());
                })
            .build();

    final TaxCategorySync taxCategorySync = new TaxCategorySync(spyOptions);

    final TaxCategoryDraft taxCategoryDraft =
        TaxCategoryDraftBuilder.of(
                "tax-category-name-updated", null, "tax-category-description-updated")
            .key("tax-category-key")
            .build();

    final TaxCategorySyncStatistics taxCategorySyncStatistics =
        taxCategorySync.sync(singletonList(taxCategoryDraft)).toCompletableFuture().join();

    // Test and assertion
    assertThat(taxCategorySyncStatistics).hasValues(1, 0, 0, 1);
    Assertions.assertThat(errorCallBackMessages).hasSize(1);
    Assertions.assertThat(errorCallBackExceptions).hasSize(1);

    Assertions.assertThat(errorCallBackMessages.get(0))
        .contains(
            format(
                "Failed to update tax category with key: '%s'. Reason: Not found when attempting to fetch while"
                    + " retrying after concurrency modification.",
                taxCategoryDraft.getKey()));
  }

  @Nonnull
  private SphereClient buildClientWithConcurrentModificationUpdateAndNotFoundFetchOnRetry() {
    final SphereClient spyClient = spy(CTP_TARGET_CLIENT);

    final TaxCategoryUpdateCommand taxCategoryUpdateCommand = any(TaxCategoryUpdateCommand.class);
    when(spyClient.execute(taxCategoryUpdateCommand))
        .thenReturn(
            CompletableFutureUtils.exceptionallyCompletedFuture(
                new ConcurrentModificationException()))
        .thenCallRealMethod();

    final TaxCategoryQuery taxCategoryQuery = any(TaxCategoryQuery.class);

    when(spyClient.execute(taxCategoryQuery))
        .thenCallRealMethod() // Call real fetch on fetching matching tax categories
        .thenReturn(CompletableFuture.completedFuture(PagedQueryResult.empty()));

    return spyClient;
  }
}
