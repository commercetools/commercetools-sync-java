package com.commercetools.sync.customers.utils;

import com.commercetools.sync.commons.helpers.GenericCustomActionBuilder;
import com.fasterxml.jackson.databind.JsonNode;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.customers.Customer;
import io.sphere.sdk.customers.commands.updateactions.SetCustomField;
import io.sphere.sdk.customers.commands.updateactions.SetCustomType;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;

public final class CustomerCustomActionBuilder implements GenericCustomActionBuilder<Customer> {

    private static final CustomerCustomActionBuilder builder = new CustomerCustomActionBuilder();

    private CustomerCustomActionBuilder() {
        super();
    }

    @Nonnull
    public static CustomerCustomActionBuilder of() {
        return builder;
    }

    @Nonnull
    @Override
    public UpdateAction<Customer> buildRemoveCustomTypeAction(
        @Nullable final Integer variantId,
        @Nullable final String objectId) {

        return SetCustomType.ofRemoveType();
    }

    @Nonnull
    @Override
    public UpdateAction<Customer> buildSetCustomTypeAction(
        @Nullable final Integer variantId,
        @Nullable final String objectId,
        @Nonnull final String customTypeId,
        @Nullable final Map<String, JsonNode> customFieldsJsonMap) {

        return SetCustomType.ofTypeIdAndJson(customTypeId, customFieldsJsonMap);
    }

    @Nonnull
    @Override
    public UpdateAction<Customer> buildSetCustomFieldAction(
        @Nullable final Integer variantId,
        @Nullable final String objectId,
        @Nullable final String customFieldName,
        @Nullable final JsonNode customFieldValue) {

        return SetCustomField.ofJson(customFieldName, customFieldValue);
    }
}
