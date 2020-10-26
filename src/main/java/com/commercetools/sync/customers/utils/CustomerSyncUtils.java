package com.commercetools.sync.customers.utils;

import com.commercetools.sync.customers.CustomerSyncOptions;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.customers.Customer;
import io.sphere.sdk.customers.CustomerDraft;

import javax.annotation.Nonnull;
import java.util.List;

import static com.commercetools.sync.commons.utils.CustomUpdateActionUtils.buildPrimaryResourceCustomUpdateActions;
import static com.commercetools.sync.commons.utils.OptionalUtils.filterEmptyOptionals;
import static com.commercetools.sync.customers.utils.CustomerUpdateActionUtils.buildAllAddressUpdateActions;
import static com.commercetools.sync.customers.utils.CustomerUpdateActionUtils.buildChangeEmailUpdateAction;
import static com.commercetools.sync.customers.utils.CustomerUpdateActionUtils.buildSetCompanyNameUpdateAction;
import static com.commercetools.sync.customers.utils.CustomerUpdateActionUtils.buildSetCustomerGroupUpdateAction;
import static com.commercetools.sync.customers.utils.CustomerUpdateActionUtils.buildSetCustomerNumberUpdateAction;
import static com.commercetools.sync.customers.utils.CustomerUpdateActionUtils.buildSetDateOfBirthUpdateAction;
import static com.commercetools.sync.customers.utils.CustomerUpdateActionUtils.buildSetExternalIdUpdateAction;
import static com.commercetools.sync.customers.utils.CustomerUpdateActionUtils.buildSetFirstNameUpdateAction;
import static com.commercetools.sync.customers.utils.CustomerUpdateActionUtils.buildSetLastNameUpdateAction;
import static com.commercetools.sync.customers.utils.CustomerUpdateActionUtils.buildSetLocaleUpdateAction;
import static com.commercetools.sync.customers.utils.CustomerUpdateActionUtils.buildSetMiddleNameUpdateAction;
import static com.commercetools.sync.customers.utils.CustomerUpdateActionUtils.buildSetSalutationUpdateAction;
import static com.commercetools.sync.customers.utils.CustomerUpdateActionUtils.buildSetTitleUpdateAction;
import static com.commercetools.sync.customers.utils.CustomerUpdateActionUtils.buildSetVatIdUpdateAction;
import static com.commercetools.sync.customers.utils.CustomerUpdateActionUtils.buildStoreUpdateActions;

public final class CustomerSyncUtils {

    private static final CustomerCustomActionBuilder customerCustomActionBuilder = CustomerCustomActionBuilder.of();

    /**
     * Compares all the fields of a {@link Customer} and a {@link CustomerDraft}. It returns a {@link List} of
     * {@link UpdateAction}&lt;{@link Customer}&gt; as a result. If no update action is needed, for example in
     * case where both the {@link CustomerDraft} and the {@link CustomerDraft} have the same fields, an empty
     * {@link List} is returned.
     *
     * @param oldCustomer the customer which should be updated.
     * @param newCustomer the customer draft where we get the new data.
     * @param syncOptions the sync options wrapper which contains options related to the sync process supplied
     *                    by the user. For example, custom callbacks to call in case of warnings or errors occurring
     *                    on the build update action process. And other options (See {@link CustomerSyncOptions}
     *                    for more info.
     * @return A list of customer specific update actions.
     */
    @Nonnull
    public static List<UpdateAction<Customer>> buildActions(
        @Nonnull final Customer oldCustomer,
        @Nonnull final CustomerDraft newCustomer,
        @Nonnull final CustomerSyncOptions syncOptions) {

        final List<UpdateAction<Customer>> updateActions = filterEmptyOptionals(
            buildChangeEmailUpdateAction(oldCustomer, newCustomer),
            buildSetFirstNameUpdateAction(oldCustomer, newCustomer),
            buildSetLastNameUpdateAction(oldCustomer, newCustomer),
            buildSetMiddleNameUpdateAction(oldCustomer, newCustomer),
            buildSetTitleUpdateAction(oldCustomer, newCustomer),
            buildSetSalutationUpdateAction(oldCustomer, newCustomer),
            buildSetCustomerGroupUpdateAction(oldCustomer, newCustomer),
            buildSetCustomerNumberUpdateAction(oldCustomer, newCustomer, syncOptions),
            buildSetExternalIdUpdateAction(oldCustomer, newCustomer),
            buildSetCompanyNameUpdateAction(oldCustomer, newCustomer),
            buildSetDateOfBirthUpdateAction(oldCustomer, newCustomer),
            buildSetVatIdUpdateAction(oldCustomer, newCustomer),
            buildSetLocaleUpdateAction(oldCustomer, newCustomer)
        );

        final List<UpdateAction<Customer>> addressUpdateActions =
            buildAllAddressUpdateActions(oldCustomer, newCustomer);

        updateActions.addAll(addressUpdateActions);

        final List<UpdateAction<Customer>> customerCustomUpdateActions =
            buildPrimaryResourceCustomUpdateActions(oldCustomer,
                newCustomer,
                customerCustomActionBuilder,
                syncOptions);

        updateActions.addAll(customerCustomUpdateActions);

        final List<UpdateAction<Customer>> buildStoreUpdateActions =
            buildStoreUpdateActions(oldCustomer, newCustomer);

        updateActions.addAll(buildStoreUpdateActions);

        return updateActions;
    }

    private CustomerSyncUtils() {
    }
}
