package com.commercetools.sync.taxcategories.helpers;

import com.commercetools.sync.taxcategories.TaxCategorySyncOptions;
import com.commercetools.sync.taxcategories.TaxCategorySyncOptionsBuilder;
import com.neovisionaries.i18n.CountryCode;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.taxcategories.TaxCategoryDraft;
import io.sphere.sdk.taxcategories.TaxCategoryDraftBuilder;
import io.sphere.sdk.taxcategories.TaxRateDraftBuilder;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static com.commercetools.sync.taxcategories.helpers.TaxCategoryBatchValidator.TAX_CATEGORY_DRAFT_IS_NULL;
import static com.commercetools.sync.taxcategories.helpers.TaxCategoryBatchValidator.TAX_CATEGORY_DRAFT_KEY_NOT_SET;
import static com.commercetools.sync.taxcategories.helpers.TaxCategoryBatchValidator.TAX_CATEGORY_DUPLICATED_COUNTRY;
import static com.commercetools.sync.taxcategories.helpers.TaxCategoryBatchValidator.TAX_CATEGORY_DUPLICATED_COUNTRY_AND_STATE;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TaxCategoryBatchValidatorTest {
    private List<String> errorCallBackMessages;
    private TaxCategorySyncOptions syncOptions;
    private TaxCategorySyncStatistics syncStatistics;

    @BeforeEach
    void setup() {
        errorCallBackMessages = new ArrayList<>();
        final SphereClient ctpClient = mock(SphereClient.class);
        syncOptions = TaxCategorySyncOptionsBuilder
            .of(ctpClient)
            .errorCallback((exception, oldResource, newResource, actions) -> {
                errorCallBackMessages.add(exception.getMessage());
            })
            .build();
        syncStatistics = new TaxCategorySyncStatistics();
    }

    @Test
    void validateAndCollectReferencedKeys_WithEmptyDraft_ShouldHaveEmptyResult() {
        final Set<TaxCategoryDraft> validDrafts = getValidDrafts(emptyList());

        assertThat(validDrafts).isEmpty();
        assertThat(errorCallBackMessages).isEmpty();
    }

    @Test
    void validateAndCollectReferencedKeys_WithNullTaxCategoryDraft_ShouldHaveValidationErrorAndEmptyResult() {
        final Set<TaxCategoryDraft> validDrafts = getValidDrafts(Collections.singletonList(null));

        assertThat(errorCallBackMessages).hasSize(1);
        assertThat(errorCallBackMessages.get(0)).isEqualTo(TAX_CATEGORY_DRAFT_IS_NULL);
        assertThat(validDrafts).isEmpty();
    }

    @Test
    void validateAndCollectReferencedKeys_WithTaxCategoryDraftWithNullKey_ShouldHaveValidationErrorAndEmptyResult() {
        final TaxCategoryDraft taxCategoryDraft = mock(TaxCategoryDraft.class);
        final Set<TaxCategoryDraft> validDrafts = getValidDrafts(Collections.singletonList(taxCategoryDraft));

        assertThat(errorCallBackMessages).hasSize(1);
        assertThat(errorCallBackMessages.get(0))
            .isEqualTo(format(TAX_CATEGORY_DRAFT_KEY_NOT_SET, taxCategoryDraft.getName()));
        assertThat(validDrafts).isEmpty();
    }

    @Test
    void validateAndCollectReferencedKeys_WithTaxCategoryDraftWithEmptyKey_ShouldHaveValidationErrorAndEmptyResult() {
        final TaxCategoryDraft taxCategoryDraft = mock(TaxCategoryDraft.class);
        when(taxCategoryDraft.getKey()).thenReturn(EMPTY);
        final Set<TaxCategoryDraft> validDrafts = getValidDrafts(Collections.singletonList(taxCategoryDraft));

        assertThat(errorCallBackMessages).hasSize(1);
        assertThat(errorCallBackMessages.get(0))
            .isEqualTo(format(TAX_CATEGORY_DRAFT_KEY_NOT_SET, taxCategoryDraft.getName()));
        assertThat(validDrafts).isEmpty();
    }

    @Test
    void validateAndCollectReferencedKeys_WithMixOfValidAndInvalidDrafts_ShouldValidateCorrectly() {
        final TaxCategoryDraft validDraft = TaxCategoryDraftBuilder
            .of("foo", singletonList(
                TaxRateDraftBuilder.of("foo", 2.0, false, CountryCode.FR).state("PARIS").build()
            ), "desc")
            .key("foo").build();

        final TaxCategoryDraft withEmptyKey = TaxCategoryDraftBuilder
            .of("foo", emptyList(), null)
            .key("")
            .build();

        final TaxCategoryDraft withNullKey = TaxCategoryDraftBuilder
            .of("foo", emptyList(), null)
            .key(null)
            .build();

        final TaxCategoryDraft withDuplicatedState = TaxCategoryDraftBuilder.of("foo", asList(
            TaxRateDraftBuilder.of("foo", 2.0, false, CountryCode.FR).state("NYON").build(),
            TaxRateDraftBuilder.of("foo", 2.0, false, CountryCode.FR).state("PARIS").build(),
            TaxRateDraftBuilder.of("foo", 3.0, false, CountryCode.DE).state("BERLIN").build(),
            TaxRateDraftBuilder.of("foo", 3.0, false, CountryCode.DE).state("BERLIN").build()
        ), "desc").key("duplicatedState").build();

        final TaxCategoryDraft withDuplicatedCountry = TaxCategoryDraftBuilder.of("foo", asList(
            TaxRateDraftBuilder.of("foo", 2.0, false, CountryCode.FR).build(),
            TaxRateDraftBuilder.of("foo", 2.0, false, CountryCode.FR).build()

        ), "desc").key("duplicatedCountry").build();

        final TaxCategoryBatchValidator batchValidator = new TaxCategoryBatchValidator(syncOptions, syncStatistics);
        final ImmutablePair<Set<TaxCategoryDraft>, Set<String>> pair = batchValidator.validateAndCollectReferencedKeys(
            Arrays.asList(null, validDraft, withEmptyKey, withNullKey, withDuplicatedState, withDuplicatedCountry));

        assertThat(pair.getLeft()).containsExactlyInAnyOrder(validDraft);
        assertThat(pair.getRight()).containsExactlyInAnyOrder("foo");

        assertThat(errorCallBackMessages).hasSize(5);
        assertThat(errorCallBackMessages.get(0))
            .isEqualTo(TAX_CATEGORY_DRAFT_IS_NULL);
        assertThat(errorCallBackMessages.get(1))
            .isEqualTo(format(TAX_CATEGORY_DRAFT_KEY_NOT_SET, "foo"));
        assertThat(errorCallBackMessages.get(2))
            .isEqualTo(format(TAX_CATEGORY_DRAFT_KEY_NOT_SET, "foo"));
        assertThat(errorCallBackMessages.get(3))
            .isEqualTo(format(TAX_CATEGORY_DUPLICATED_COUNTRY_AND_STATE, CountryCode.DE, "BERLIN"));
        assertThat(errorCallBackMessages.get(4))
            .isEqualTo(format(TAX_CATEGORY_DUPLICATED_COUNTRY, CountryCode.FR));

    }

    @Nonnull
    private Set<TaxCategoryDraft> getValidDrafts(@Nonnull final List<TaxCategoryDraft> taxCategoryDrafts) {
        final TaxCategoryBatchValidator batchValidator = new TaxCategoryBatchValidator(syncOptions, syncStatistics);
        final ImmutablePair<Set<TaxCategoryDraft>, Set<String>> pair =
            batchValidator.validateAndCollectReferencedKeys(taxCategoryDrafts);
        return pair.getLeft();
    }
}
