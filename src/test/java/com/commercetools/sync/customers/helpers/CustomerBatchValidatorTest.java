package com.commercetools.sync.customers.helpers;

import com.commercetools.sync.customers.CustomerSyncOptions;
import com.commercetools.sync.customers.CustomerSyncOptionsBuilder;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.customers.CustomerDraft;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static com.commercetools.sync.customers.helpers.CustomerBatchValidator.CUSTOMER_DRAFT_EMAIL_NOT_SET;
import static com.commercetools.sync.customers.helpers.CustomerBatchValidator.CUSTOMER_DRAFT_IS_NULL;
import static com.commercetools.sync.customers.helpers.CustomerBatchValidator.CUSTOMER_DRAFT_KEY_NOT_SET;
import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CustomerBatchValidatorTest {

    private CustomerSyncOptions syncOptions;
    private CustomerSyncStatistics syncStatistics;
    private List<String> errorCallBackMessages;

    @BeforeEach
    void setup() {
        errorCallBackMessages = new ArrayList<>();
        final SphereClient ctpClient = mock(SphereClient.class);

        syncOptions = CustomerSyncOptionsBuilder
            .of(ctpClient)
            .errorCallback((exception, oldResource, newResource, updateActions) ->
                errorCallBackMessages.add(exception.getMessage()))
            .build();
        syncStatistics = mock(CustomerSyncStatistics.class);
    }

    @Test
    void validateAndCollectReferencedKeys_WithEmptyDraft_ShouldHaveEmptyResult() {
        final Set<CustomerDraft> validDrafts = getValidDrafts(Collections.emptyList());

        assertThat(errorCallBackMessages).hasSize(0);
        assertThat(validDrafts).isEmpty();
    }

    @Test
    void validateAndCollectReferencedKeys_WithNullCustomerDraft_ShouldHaveValidationErrorAndEmptyResult() {
        final Set<CustomerDraft> validDrafts = getValidDrafts(Collections.singletonList(null));

        assertThat(errorCallBackMessages).hasSize(1);
        assertThat(errorCallBackMessages.get(0)).isEqualTo(CUSTOMER_DRAFT_IS_NULL);
        assertThat(validDrafts).isEmpty();
    }

    @Test
    void validateAndCollectReferencedKeys_WithCustomerDraftWithNullKey_ShouldHaveValidationErrorAndEmptyResult() {
        final CustomerDraft customerDraft = mock(CustomerDraft.class);
        final Set<CustomerDraft> validDrafts = getValidDrafts(Collections.singletonList(customerDraft));

        assertThat(errorCallBackMessages).hasSize(1);
        assertThat(errorCallBackMessages.get(0))
            .isEqualTo(format(CUSTOMER_DRAFT_KEY_NOT_SET, customerDraft.getEmail()));
        assertThat(validDrafts).isEmpty();
    }

    @Test
    void validateAndCollectReferencedKeys_WithCustomerDraftWithEmptyKey_ShouldHaveValidationErrorAndEmptyResult() {
        final CustomerDraft customerDraft = mock(CustomerDraft.class);
        when(customerDraft.getKey()).thenReturn(EMPTY);
        final Set<CustomerDraft> validDrafts = getValidDrafts(Collections.singletonList(customerDraft));

        assertThat(errorCallBackMessages).hasSize(1);
        assertThat(errorCallBackMessages.get(0))
            .isEqualTo(format(CUSTOMER_DRAFT_KEY_NOT_SET, customerDraft.getEmail()));
        assertThat(validDrafts).isEmpty();
    }

    @Test
    void validateAndCollectReferencedKeys_WithCustomerDraftWithNullEmail_ShouldHaveValidationErrorAndEmptyResult() {
        final CustomerDraft customerDraft = mock(CustomerDraft.class);
        when(customerDraft.getKey()).thenReturn("key");
        final Set<CustomerDraft> validDrafts = getValidDrafts(Collections.singletonList(customerDraft));

        assertThat(errorCallBackMessages).hasSize(1);
        assertThat(errorCallBackMessages.get(0))
            .isEqualTo(format(CUSTOMER_DRAFT_EMAIL_NOT_SET, customerDraft.getKey()));
        assertThat(validDrafts).isEmpty();
    }

    @Test
    void validateAndCollectReferencedKeys_WithCustomerDraftWithEmptyEmail_ShouldHaveValidationErrorAndEmptyResult() {
        final CustomerDraft customerDraft = mock(CustomerDraft.class);
        when(customerDraft.getKey()).thenReturn("key");
        when(customerDraft.getEmail()).thenReturn(EMPTY);
        final Set<CustomerDraft> validDrafts = getValidDrafts(Collections.singletonList(customerDraft));

        assertThat(errorCallBackMessages).hasSize(1);
        assertThat(errorCallBackMessages.get(0))
            .isEqualTo(format(CUSTOMER_DRAFT_EMAIL_NOT_SET, customerDraft.getKey()));
        assertThat(validDrafts).isEmpty();
    }


    @Nonnull
    private Set<CustomerDraft> getValidDrafts(@Nonnull final List<CustomerDraft> customerDrafts) {
        final CustomerBatchValidator customerBatchValidator =
            new CustomerBatchValidator(syncOptions, syncStatistics);
        final ImmutablePair<Set<CustomerDraft>, CustomerBatchValidator.ReferencedKeys> pair =
            customerBatchValidator.validateAndCollectReferencedKeys(customerDrafts);
        return pair.getLeft();
    }
}
