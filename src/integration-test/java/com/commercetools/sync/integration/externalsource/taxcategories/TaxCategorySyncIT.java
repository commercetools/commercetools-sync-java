package com.commercetools.sync.integration.externalsource.taxcategories;

import static com.commercetools.sync.commons.asserts.statistics.AssertionsForStatistics.assertThat;
import static com.commercetools.sync.integration.commons.utils.ITUtils.*;
import static com.commercetools.sync.integration.commons.utils.TaxCategoryITUtils.*;
import static com.commercetools.sync.integration.commons.utils.TestClientUtils.CTP_TARGET_CLIENT;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.defaultconfig.ApiRootBuilder;
import com.commercetools.api.models.tax_category.*;
import com.commercetools.sync.taxcategories.TaxCategorySync;
import com.commercetools.sync.taxcategories.TaxCategorySyncOptions;
import com.commercetools.sync.taxcategories.TaxCategorySyncOptionsBuilder;
import com.commercetools.sync.taxcategories.helpers.TaxCategorySyncStatistics;
import com.neovisionaries.i18n.CountryCode;
import io.vrap.rmf.base.client.ApiHttpMethod;
import io.vrap.rmf.base.client.ApiHttpResponse;
import io.vrap.rmf.base.client.error.BadGatewayException;
import io.vrap.rmf.base.client.utils.CompletableFutureUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import javax.annotation.Nonnull;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TaxCategorySyncIT {

  @BeforeEach
  void setup() {
    deleteTaxCategories(CTP_TARGET_CLIENT);
    final SubRate subRate1 = SubRateBuilder.of().name("subRate-1").amount(0.08).build();
    final SubRate subRate2 = SubRateBuilder.of().name("subRate-2").amount(0.11).build();

    final TaxRateDraft taxRateDraft =
        TaxRateDraftBuilder.of()
            .name("%19 VAT DE")
            .amount(0.19)
            .includedInPrice(false)
            .country(CountryCode.DE.getAlpha2())
            .subRates(asList(subRate1, subRate2))
            .build();

    final TaxCategoryDraft taxCategoryDraft =
        TaxCategoryDraftBuilder.of()
            .name("tax-category-name")
            .rates(taxRateDraft)
            .description("tax-category-description")
            .key("tax-category-key")
            .build();

    createTaxCategoryByDraft(taxCategoryDraft, CTP_TARGET_CLIENT);
  }

  @AfterAll
  static void tearDown() {
    deleteTaxCategories(CTP_TARGET_CLIENT);
  }

  @Test
  void sync_withNewTaxCategory_shouldCreateTaxCategory() {
    final SubRate subRate1 = SubRateBuilder.of().name("subRate-1").amount(0.05).build();
    final SubRate subRate2 = SubRateBuilder.of().name("subRate-2").amount(0.06).build();

    final TaxRateDraft taxRateDraft =
        TaxRateDraftBuilder.of()
            .name("11% US")
            .amount(0.11)
            .includedInPrice(false)
            .country(CountryCode.US.getAlpha2())
            .subRates(asList(subRate1, subRate2))
            .build();

    final TaxCategoryDraft taxCategoryDraft =
        TaxCategoryDraftBuilder.of()
            .name("tax-category-name-new")
            .rates(taxRateDraft)
            .description("tax-category-description-new")
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
    final SubRate subRate1 = SubRateBuilder.of().name("subRate-1").amount(0.07).build();
    final SubRate subRate2 = SubRateBuilder.of().name("subRate-2").amount(0.09).build();

    final TaxRateDraft taxRateDraft =
        TaxRateDraftBuilder.of()
            .name("%16 VAT")
            .amount(0.16)
            .includedInPrice(true)
            .country(CountryCode.DE.getAlpha2())
            .subRates(asList(subRate1, subRate2))
            .build();

    final TaxCategoryDraft taxCategoryDraft =
        TaxCategoryDraftBuilder.of()
            .name("name-updated")
            .rates(taxRateDraft)
            .description("description-updated")
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
              Assertions.assertThat(taxCategory.getName()).isEqualTo("name-updated");
              Assertions.assertThat(taxCategory.getDescription()).isEqualTo("description-updated");
              final TaxRate taxRate = taxCategory.getRates().get(0);
              Assertions.assertThat(taxRate.getName()).isEqualTo("%16 VAT");
              Assertions.assertThat(taxRate.getAmount()).isEqualTo(0.16);
              Assertions.assertThat(taxRate.getCountry()).isEqualTo(CountryCode.DE.getAlpha2());
              Assertions.assertThat(taxRate.getIncludedInPrice()).isEqualTo(true);
              Assertions.assertThat(taxRate.getSubRates()).isEqualTo(asList(subRate1, subRate2));
            });
  }

  @Test
  void sync_withEqualTaxCategory_shouldNotUpdateTaxCategory() {
    final SubRate subRate1 = SubRateBuilder.of().name("subRate-1").amount(0.08).build();
    final SubRate subRate2 = SubRateBuilder.of().name("subRate-2").amount(0.11).build();

    final TaxRateDraft taxRateDraft =
        TaxRateDraftBuilder.of()
            .name("%19 VAT DE")
            .amount(0.19)
            .includedInPrice(false)
            .country(CountryCode.DE.getAlpha2())
            .subRates(asList(subRate1, subRate2))
            .build();

    final TaxCategoryDraft taxCategoryDraft =
        TaxCategoryDraftBuilder.of()
            .name("tax-category-name")
            .rates(taxRateDraft)
            .description("tax-category-description")
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
    final ProjectApiRoot spyClient = buildClientWithConcurrentModificationUpdate();

    final List<String> errorCallBackMessages = new ArrayList<>();
    final List<String> warningCallBackMessages = new ArrayList<>();
    final List<Throwable> errorCallBackExceptions = new ArrayList<>();
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
        TaxCategoryDraftBuilder.of()
            .name("name")
            .description("desc")
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
  private ProjectApiRoot buildClientWithConcurrentModificationUpdate() {
    // Helps to count invocation of a request and used to decide execution or mocking response
    final AtomicInteger postRequestInvocationCounter = new AtomicInteger(0);
    return withTestClient(
        (uri, method) -> {
          if (uri.contains("tax-categories/")
              && ApiHttpMethod.POST.equals(method)
              && postRequestInvocationCounter.getAndIncrement() == 0) {
            return CompletableFutureUtils.exceptionallyCompletedFuture(
                createConcurrentModificationException());
          }
          return null;
        });
  }

  @Test
  void sync_WithConcurrentModificationExceptionAndFailedFetch_ShouldFailToReFetchAndUpdate() {
    // preparation
    final ProjectApiRoot spyClient =
        buildClientWithConcurrentModificationUpdateAndFailedFetchOnRetry();

    final List<String> errorCallBackMessages = new ArrayList<>();
    final List<Throwable> errorCallBackExceptions = new ArrayList<>();
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
        TaxCategoryDraftBuilder.of()
            .name("name")
            .description("desc")
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
  private ProjectApiRoot buildClientWithConcurrentModificationUpdateAndFailedFetchOnRetry() {
    return withTestClient(
        (uri, method) -> {
          if (uri.contains("tax-categories/key=") && ApiHttpMethod.GET.equals(method)) {
            return CompletableFutureUtils.exceptionallyCompletedFuture(createBadGatewayException());
          } else if (uri.contains("tax-categories/") && ApiHttpMethod.POST.equals(method)) {
            return CompletableFutureUtils.exceptionallyCompletedFuture(
                createConcurrentModificationException());
          }
          return null;
        });
  }

  @Test
  void sync_WithConcurrentModificationExceptionAndUnexpectedDelete_ShouldFailToReFetchAndUpdate() {
    // preparation
    final ProjectApiRoot spyClient =
        buildClientWithConcurrentModificationUpdateAndNotFoundFetchOnRetry();

    final List<String> errorCallBackMessages = new ArrayList<>();
    final List<Throwable> errorCallBackExceptions = new ArrayList<>();
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
        TaxCategoryDraftBuilder.of()
            .name("name")
            .description("desc")
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
  private ProjectApiRoot buildClientWithConcurrentModificationUpdateAndNotFoundFetchOnRetry() {
    return withTestClient(
        (uri, method) -> {
          if (uri.contains("tax-categories/key=") && ApiHttpMethod.GET.equals(method)) {
            return CompletableFutureUtils.exceptionallyCompletedFuture(createNotFoundException());
          }
          if (uri.contains("tax-categories/") && ApiHttpMethod.POST.equals(method)) {
            return CompletableFutureUtils.exceptionallyCompletedFuture(
                createConcurrentModificationException());
          }
          return null;
        });
  }

  private ProjectApiRoot withTestClient(
      BiFunction<String, ApiHttpMethod, CompletableFuture<ApiHttpResponse<byte[]>>> fn) {
    return ApiRootBuilder.of(
            request -> {
              final String uri = request.getUri() != null ? request.getUri().toString() : "";
              final ApiHttpMethod method = request.getMethod();
              final CompletableFuture<ApiHttpResponse<byte[]>> exceptionResponse =
                  fn.apply(uri, method);
              if (exceptionResponse != null) {
                return exceptionResponse;
              }
              return CTP_TARGET_CLIENT.getApiHttpClient().execute(request);
            })
        .withApiBaseUrl(CTP_TARGET_CLIENT.getApiHttpClient().getBaseUri())
        .build(CTP_TARGET_CLIENT.getProjectKey());
  }
}
