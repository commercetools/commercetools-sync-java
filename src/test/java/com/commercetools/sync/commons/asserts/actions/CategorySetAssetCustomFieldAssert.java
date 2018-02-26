package com.commercetools.sync.commons.asserts.actions;

import com.fasterxml.jackson.databind.JsonNode;
import io.sphere.sdk.categories.commands.updateactions.SetAssetCustomField;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static org.assertj.core.api.Assertions.assertThat;


public class CategorySetAssetCustomFieldAssert
    extends AbstractSetCustomFieldAssert<CategorySetAssetCustomFieldAssert, SetAssetCustomField> {

    CategorySetAssetCustomFieldAssert(@Nullable final SetAssetCustomField actual) {
        super(actual, CategorySetAssetCustomFieldAssert.class);
    }

    /**
     * Verifies that the actual {@link SetAssetCustomField} value has identical fields as the ones supplied.
     *
     * @param actionName       the update action name.
     * @param assetId          the asset Id the action is performed on.
     * @param assetKey         the asset key the action is performed on.
     * @param customFieldName  the custom field name to update.
     * @param customFieldValue the new custom field name to update.
     * @return {@code this} assertion object.
     * @throws AssertionError if the actual value is {@code null}.
     * @throws AssertionError if the actual fields do not match the supplied values.
     */
    public CategorySetAssetCustomFieldAssert hasValues(
        @Nonnull final String actionName,
        @Nullable final String assetId,
        @Nullable final String assetKey,
        @Nullable final String customFieldName,
        @Nullable final JsonNode customFieldValue) {

        super.hasValues(actionName, customFieldName, customFieldValue);
        assertThat(actual.getAssetId()).isEqualTo(assetId);
        assertThat(actual.getAssetKey()).isEqualTo(assetKey);
        return myself;
    }
}
