package com.commercetools.sync.customers.helpers;

import com.commercetools.sync.commons.helpers.BaseBatchValidator;
import com.commercetools.sync.commons.helpers.BaseSyncStatistics;
import com.commercetools.sync.customers.CustomerSyncOptions;
import io.sphere.sdk.customers.CustomerDraft;
import org.apache.commons.lang3.tuple.ImmutablePair;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isBlank;

public class CustomerBatchValidator
    extends BaseBatchValidator<CustomerDraft, CustomerSyncOptions, BaseSyncStatistics> {

    static final String CUSTOMER_DRAFT_EMAIL_NOT_SET = "CustomerDraft with key: %s doesn't have an email. "
        + "Please make sure all customer drafts have emails.";
    static final String CUSTOMER_DRAFT_KEY_NOT_SET = "CustomerDraft with email: %s doesn't have a key. "
        + "Please make sure all customer drafts have keys.";
    static final String CUSTOMER_DRAFT_IS_NULL = "CustomerDraft is null.";

    public CustomerBatchValidator(@Nonnull final CustomerSyncOptions syncOptions,
                                  @Nonnull final BaseSyncStatistics syncStatistics) {
        super(syncOptions, syncStatistics);
    }

    /**
     * Given the {@link List}&lt;{@link CustomerDraft}&gt; of drafts this method attempts to validate
     * drafts and return an {@link ImmutablePair}&lt;{@link Set}&lt;{@link CustomerDraft}&gt;,
     * {@link CustomerBatchValidator.ReferencedKeys}&gt; which contains the {@link Set} of valid drafts and
     * referenced keys within a wrapper.
     *
     * <p>A valid customer draft is one which satisfies the following conditions:
     * <ol>
     * <li>It is not null</li>
     * <li>It has a key which is not blank (null/empty)</li>
     * <li>It has a email which is not blank (null/empty)</li>
     * </ol>
     *
     * @param customerDrafts the customer drafts to validate and collect referenced keys.
     * @return {@link ImmutablePair}&lt;{@link Set}&lt;{@link CustomerDraft}&gt;,
     *      {@link CustomerBatchValidator.ReferencedKeys}&gt; which contains the {@link Set} of valid drafts and
     *      referenced keys within a wrapper.
     */
    @Override
    public ImmutablePair<Set<CustomerDraft>, ReferencedKeys> validateAndCollectReferencedKeys(
        @Nonnull final List<CustomerDraft> customerDrafts) {

        final ReferencedKeys referencedKeys = new ReferencedKeys();
        final Set<CustomerDraft> validDrafts = customerDrafts
            .stream()
            .filter(this::isValidCustomerDraft)
            .peek(customerDraft -> collectReferencedKeys(referencedKeys, customerDraft))
            .collect(Collectors.toSet());

        return ImmutablePair.of(validDrafts, referencedKeys);
    }

    private boolean isValidCustomerDraft(
        @Nullable final CustomerDraft customerDraft) {

        if (customerDraft == null) {
            handleError(CUSTOMER_DRAFT_IS_NULL);
        } else if (isBlank(customerDraft.getKey())) {
            handleError(format(CUSTOMER_DRAFT_KEY_NOT_SET, customerDraft.getEmail()));
        } else if (isBlank(customerDraft.getEmail())) {
            handleError(format(CUSTOMER_DRAFT_EMAIL_NOT_SET, customerDraft.getKey()));
        } else {
            // todo (ahmetoz)
            // address: Sets the key of each address to be unique in the addresses list.
            // check if billing and shipping addresses has the right index.
            // check if the indexes are not negative.
            // what to do for password ?
            return true;
        }
        return false;
    }

    private void collectReferencedKeys(
        @Nonnull final CustomerBatchValidator.ReferencedKeys referencedKeys,
        @Nonnull final CustomerDraft customerDraft) {

        collectReferencedKeyFromResourceIdentifier(customerDraft.getCustomerGroup(),
            referencedKeys.customerGroupKeys::add);
        collectReferencedKeyFromCustomFieldsDraft(customerDraft.getCustom(),
            referencedKeys.typeKeys::add);
    }

    public static class ReferencedKeys {
        private final Set<String> customerGroupKeys = new HashSet<>();
        private final Set<String> typeKeys = new HashSet<>();

        public Set<String> getTypeKeys() {
            return typeKeys;
        }

        public Set<String> getCustomerGroupKeys() {
            return customerGroupKeys;
        }
    }
}
