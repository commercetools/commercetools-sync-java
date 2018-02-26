package com.commercetools.sync.commons.asserts.actions;

import com.fasterxml.jackson.databind.JsonNode;
import io.sphere.sdk.products.commands.updateactions.SetProductPriceCustomField;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static org.assertj.core.api.Assertions.assertThat;


public class SetPriceCustomFieldAssert
    extends AbstractSetCustomFieldAssert<SetPriceCustomFieldAssert, SetProductPriceCustomField> {

    SetPriceCustomFieldAssert(@Nullable final SetProductPriceCustomField actual) {
        super(actual, SetPriceCustomFieldAssert.class);
    }

    /**
     * Verifies that the actual {@link SetProductPriceCustomField} value has identical fields as the ones supplied.
     *
     * @param actionName       the update action name.
     * @param priceId          the price id that has the custom fields.
     * @param staged           the staged flag of the action.
     * @param customFieldName  the custom field name to update.
     * @param customFieldValue the new custom field name to update.
     * @return {@code this} assertion object.
     * @throws AssertionError if the actual value is {@code null}.
     * @throws AssertionError if the actual fields do not match the supplied values.
     */
    public SetPriceCustomFieldAssert hasValues(
        @Nonnull final String actionName,
        @Nullable final Boolean staged,
        @Nullable final String priceId,
        @Nullable final String customFieldName,
        @Nullable final JsonNode customFieldValue) {

        super.hasValues(actionName, customFieldName, customFieldValue);
        assertThat(actual.getPriceId()).isEqualTo(priceId);
        assertThat(actual.getStaged()).isEqualTo(staged);
        return myself;
    }
}
