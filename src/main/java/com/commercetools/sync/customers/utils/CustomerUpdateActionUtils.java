package com.commercetools.sync.customers.utils;

import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.customergroups.CustomerGroup;
import io.sphere.sdk.customers.Customer;
import io.sphere.sdk.customers.CustomerDraft;
import io.sphere.sdk.customers.commands.updateactions.ChangeEmail;
import io.sphere.sdk.customers.commands.updateactions.SetCompanyName;
import io.sphere.sdk.customers.commands.updateactions.SetCustomerGroup;
import io.sphere.sdk.customers.commands.updateactions.SetCustomerNumber;
import io.sphere.sdk.customers.commands.updateactions.SetDateOfBirth;
import io.sphere.sdk.customers.commands.updateactions.SetExternalId;
import io.sphere.sdk.customers.commands.updateactions.SetFirstName;
import io.sphere.sdk.customers.commands.updateactions.SetLastName;
import io.sphere.sdk.customers.commands.updateactions.SetLocale;
import io.sphere.sdk.customers.commands.updateactions.SetMiddleName;
import io.sphere.sdk.customers.commands.updateactions.SetSalutation;
import io.sphere.sdk.customers.commands.updateactions.SetTitle;
import io.sphere.sdk.customers.commands.updateactions.SetVatId;
import io.sphere.sdk.models.Reference;
import io.sphere.sdk.models.Referenceable;
import io.sphere.sdk.models.ResourceIdentifier;
import io.sphere.sdk.models.ResourceImpl;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Optional;

import static com.commercetools.sync.commons.utils.CommonTypeUpdateActionUtils.buildUpdateAction;
import static com.commercetools.sync.commons.utils.CommonTypeUpdateActionUtils.buildUpdateActionForReferences;

public final class CustomerUpdateActionUtils {

    /**
     * Compares the {@code email} values of a {@link Customer} and a {@link CustomerDraft}
     * and returns an {@link Optional} of update action, which would contain the {@code "changeEmail"}
     * {@link UpdateAction}. If both {@link Customer} and {@link CustomerDraft} have the same
     * {@code email} values, then no update action is needed and empty optional will be returned.
     *
     * @param oldCustomer the customer that should be updated.
     * @param newCustomer the customer draft that contains the new email.
     * @return optional containing update action or empty optional if emails are identical.
     */
    @Nonnull
    public static Optional<UpdateAction<Customer>> buildChangeEmailUpdateAction(
        @Nonnull final Customer oldCustomer,
        @Nonnull final CustomerDraft newCustomer) {

        return buildUpdateAction(oldCustomer.getEmail(), newCustomer.getEmail(),
            () -> ChangeEmail.of(newCustomer.getEmail()));
    }

    /**
     * Compares the {@code firstName} values of a {@link Customer} and a {@link CustomerDraft}
     * and returns an {@link Optional} of update action, which would contain the {@code "setFirstName"}
     * {@link UpdateAction}. If both {@link Customer} and {@link CustomerDraft} have the same
     * {@code firstName} values, then no update action is needed and empty optional will be returned.
     *
     * @param oldCustomer the customer that should be updated.
     * @param newCustomer the customer draft that contains the new first name.
     * @return optional containing update action or empty optional if first names are identical.
     */
    @Nonnull
    public static Optional<UpdateAction<Customer>> buildSetFirstNameUpdateAction(
        @Nonnull final Customer oldCustomer,
        @Nonnull final CustomerDraft newCustomer) {

        return buildUpdateAction(oldCustomer.getFirstName(), newCustomer.getFirstName(),
            () -> SetFirstName.of(newCustomer.getFirstName()));
    }

    /**
     * Compares the {@code lastName} values of a {@link Customer} and a {@link CustomerDraft}
     * and returns an {@link Optional} of update action, which would contain the {@code "setLastName"}
     * {@link UpdateAction}. If both {@link Customer} and {@link CustomerDraft} have the same
     * {@code lastName} values, then no update action is needed and empty optional will be returned.
     *
     * @param oldCustomer the customer that should be updated.
     * @param newCustomer the customer draft that contains the new last name.
     * @return optional containing update action or empty optional if last names are identical.
     */
    @Nonnull
    public static Optional<UpdateAction<Customer>> buildSetLastNameUpdateAction(
        @Nonnull final Customer oldCustomer,
        @Nonnull final CustomerDraft newCustomer) {

        return buildUpdateAction(oldCustomer.getLastName(), newCustomer.getLastName(),
            () -> SetLastName.of(newCustomer.getLastName()));
    }

    /**
     * Compares the {@code middleName} values of a {@link Customer} and a {@link CustomerDraft}
     * and returns an {@link Optional} of update action, which would contain the {@code "setMiddleName"}
     * {@link UpdateAction}. If both {@link Customer} and {@link CustomerDraft} have the same
     * {@code middleName} values, then no update action is needed and empty optional will be returned.
     *
     * @param oldCustomer the customer that should be updated.
     * @param newCustomer the customer draft that contains the new middle name.
     * @return optional containing update action or empty optional if middle names are identical.
     */
    @Nonnull
    public static Optional<UpdateAction<Customer>> buildSetMiddleNameUpdateAction(
        @Nonnull final Customer oldCustomer,
        @Nonnull final CustomerDraft newCustomer) {

        return buildUpdateAction(oldCustomer.getMiddleName(), newCustomer.getMiddleName(),
            () -> SetMiddleName.of(newCustomer.getMiddleName()));
    }

    /**
     * Compares the {@code title} values of a {@link Customer} and a {@link CustomerDraft}
     * and returns an {@link Optional} of update action, which would contain the {@code "setTitle"}
     * {@link UpdateAction}. If both {@link Customer} and {@link CustomerDraft} have the same
     * {@code title} values, then no update action is needed and empty optional will be returned.
     *
     * @param oldCustomer the customer that should be updated.
     * @param newCustomer the customer draft that contains the new title.
     * @return optional containing update action or empty optional if titles are identical.
     */
    @Nonnull
    public static Optional<UpdateAction<Customer>> buildSetTitleUpdateAction(
        @Nonnull final Customer oldCustomer,
        @Nonnull final CustomerDraft newCustomer) {

        return buildUpdateAction(oldCustomer.getTitle(), newCustomer.getTitle(),
            () -> SetTitle.of(newCustomer.getTitle()));
    }

    /**
     * Compares the {@code salutation} values of a {@link Customer} and a {@link CustomerDraft}
     * and returns an {@link Optional} of update action, which would contain the {@code "SetSalutation"}
     * {@link UpdateAction}. If both {@link Customer} and {@link CustomerDraft} have the same
     * {@code salutation} values, then no update action is needed and empty optional will be returned.
     *
     * @param oldCustomer the Customer that should be updated.
     * @param newCustomer the Customer draft that contains the new salutation.
     * @return optional containing update action or empty optional if salutations are identical.
     */
    @Nonnull
    public static Optional<UpdateAction<Customer>> buildSetSalutationUpdateAction(
        @Nonnull final Customer oldCustomer,
        @Nonnull final CustomerDraft newCustomer) {

        return buildUpdateAction(oldCustomer.getSalutation(), newCustomer.getSalutation(),
            () -> SetSalutation.of(newCustomer.getSalutation()));
    }

    /**
     * Compares the {@code customerNumber} values of a {@link Customer} and a {@link CustomerDraft}
     * and returns an {@link Optional} of update action, which would contain the {@code "setCustomerNumber"}
     * {@link UpdateAction}. If both {@link Customer} and {@link CustomerDraft} have the same
     * {@code customerNumber} values, then no update action is needed and empty optional will be returned.
     *
     * @param oldCustomer the customer that should be updated.
     * @param newCustomer the customer draft that contains the new customer number.
     * @return optional containing update action or empty optional if customer numbers are identical.
     */
    @Nonnull
    public static Optional<UpdateAction<Customer>> buildSetCustomerNumberUpdateAction(
        @Nonnull final Customer oldCustomer,
        @Nonnull final CustomerDraft newCustomer) {

        return buildUpdateAction(oldCustomer.getCustomerNumber(), newCustomer.getCustomerNumber(),
            () -> SetCustomerNumber.of(newCustomer.getCustomerNumber()));
    }

    /**
     * Compares the {@code externalId} values of a {@link Customer} and a {@link CustomerDraft}
     * and returns an {@link Optional} of update action, which would contain the {@code "setExternalId"}
     * {@link UpdateAction}. If both {@link Customer} and {@link CustomerDraft} have the same
     * {@code externalId} values, then no update action is needed and empty optional will be returned.
     *
     * @param oldCustomer the customer that should be updated.
     * @param newCustomer the customer draft that contains the new external id.
     * @return optional containing update action or empty optional if external ids are identical.
     */
    @Nonnull
    public static Optional<UpdateAction<Customer>> buildSetExternalIdUpdateAction(
        @Nonnull final Customer oldCustomer,
        @Nonnull final CustomerDraft newCustomer) {

        return buildUpdateAction(oldCustomer.getExternalId(), newCustomer.getExternalId(),
            () -> SetExternalId.of(newCustomer.getExternalId()));
    }

    /**
     * Compares the {@code companyName} values of a {@link Customer} and a {@link CustomerDraft}
     * and returns an {@link Optional} of update action, which would contain the {@code "setCompanyName"}
     * {@link UpdateAction}. If both {@link Customer} and {@link CustomerDraft} have the same
     * {@code companyName} values, then no update action is needed and empty optional will be returned.
     *
     * @param oldCustomer the customer that should be updated.
     * @param newCustomer the customer draft that contains the new company name.
     * @return optional containing update action or empty optional if company names are identical.
     */
    @Nonnull
    public static Optional<UpdateAction<Customer>> buildSetCompanyNameUpdateAction(
        @Nonnull final Customer oldCustomer,
        @Nonnull final CustomerDraft newCustomer) {

        return buildUpdateAction(oldCustomer.getCompanyName(), newCustomer.getCompanyName(),
            () -> SetCompanyName.of(newCustomer.getCompanyName()));
    }

    /**
     * Compares the {@code dateOfBirth} values of a {@link Customer} and a {@link CustomerDraft}
     * and returns an {@link Optional} of update action, which would contain the {@code "setDateOfBirth"}
     * {@link UpdateAction}. If both {@link Customer} and {@link CustomerDraft} have the same
     * {@code dateOfBirth} values, then no update action is needed and empty optional will be returned.
     *
     * @param oldCustomer the customer that should be updated.
     * @param newCustomer the customer draft that contains the new date of birth.
     * @return optional containing update action or empty optional if dates of birth are identical.
     */
    @Nonnull
    public static Optional<UpdateAction<Customer>> buildSetDateOfBirthUpdateAction(
        @Nonnull final Customer oldCustomer,
        @Nonnull final CustomerDraft newCustomer) {

        return buildUpdateAction(oldCustomer.getDateOfBirth(), newCustomer.getDateOfBirth(),
            () -> SetDateOfBirth.of(newCustomer.getDateOfBirth()));
    }

    /**
     * Compares the {@code vatId} values of a {@link Customer} and a {@link CustomerDraft}
     * and returns an {@link Optional} of update action, which would contain the {@code "setVatId"}
     * {@link UpdateAction}. If both {@link Customer} and {@link CustomerDraft} have the same
     * {@code vatId} values, then no update action is needed and empty optional will be returned.
     *
     * @param oldCustomer the customer that should be updated.
     * @param newCustomer the customer draft that contains the new vat id.
     * @return optional containing update action or empty optional if vat ids are identical.
     */
    @Nonnull
    public static Optional<UpdateAction<Customer>> buildSetVatIdUpdateAction(
        @Nonnull final Customer oldCustomer,
        @Nonnull final CustomerDraft newCustomer) {

        return buildUpdateAction(oldCustomer.getVatId(), newCustomer.getVatId(),
            () -> SetVatId.of(newCustomer.getVatId()));
    }

    /**
     * Compares the {@code locale} values of a {@link Customer} and a {@link CustomerDraft}
     * and returns an {@link Optional} of update action, which would contain the {@code "setLocale"}
     * {@link UpdateAction}. If both {@link Customer} and {@link CustomerDraft} have the same
     * {@code locale} values, then no update action is needed and empty optional will be returned.
     *
     * @param oldCustomer the customer that should be updated.
     * @param newCustomer the customer draft that contains the new locale.
     * @return optional containing update action or empty optional if locales are identical.
     */
    @Nonnull
    public static Optional<UpdateAction<Customer>> buildSetLocaleUpdateAction(
        @Nonnull final Customer oldCustomer,
        @Nonnull final CustomerDraft newCustomer) {

        return buildUpdateAction(oldCustomer.getLocale(), newCustomer.getLocale(),
            () -> SetLocale.of(newCustomer.getLocale()));
    }

    /**
     * Compares the {@link CustomerGroup} references of an old {@link Customer} and
     * new {@link CustomerDraft}. If they are different - return {@link SetCustomerGroup} update action.
     *
     * <p>If the old value is set, but the new one is empty - the command will unset the customer group.
     *
     * @param oldCustomer the customer that should be updated.
     * @param newCustomer the customer draft with new {@link CustomerGroup} reference.
     * @return An optional with {@link SetCustomerGroup} update action.
     */
    @Nonnull
    public static Optional<UpdateAction<Customer>> buildSetCustomerGroupUpdateAction(
        @Nonnull final Customer oldCustomer,
        @Nonnull final CustomerDraft newCustomer) {

        return buildUpdateActionForReferences(oldCustomer.getCustomerGroup(), newCustomer.getCustomerGroup(),
            () -> SetCustomerGroup.of(mapResourceIdentifierToReferenceable(newCustomer.getCustomerGroup())));
    }

    @Nullable
    private static Referenceable<CustomerGroup> mapResourceIdentifierToReferenceable(
        @Nullable final ResourceIdentifier<CustomerGroup> resourceIdentifier) {

        if (resourceIdentifier == null) {
            return null; // unset
        }

        // todo (ahmetoz) add an issue to JVM SDK so SetCustomerGroup could be created with a ResourceIdentifier
        return new ResourceImpl<CustomerGroup>(null, null, null, null) {
            @Override
            public Reference<CustomerGroup> toReference() {
                return Reference.of(CustomerGroup.referenceTypeId(), resourceIdentifier.getId());
            }
        };
    }
}
