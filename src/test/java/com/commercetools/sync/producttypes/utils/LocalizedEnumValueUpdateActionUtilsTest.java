package com.commercetools.sync.producttypes.utils;

import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.models.LocalizedEnumValue;
import io.sphere.sdk.models.LocalizedString;
import io.sphere.sdk.producttypes.ProductType;
import io.sphere.sdk.producttypes.commands.updateactions.ChangeLocalizedEnumValueLabel;
import org.junit.Test;

import java.util.Optional;

import static com.commercetools.sync.producttypes.utils.LocalizedEnumUpdateActionsUtils.buildChangeLabelAction;
import static org.assertj.core.api.Assertions.assertThat;

public class LocalizedEnumValueUpdateActionUtilsTest {
    private static LocalizedEnumValue old = LocalizedEnumValue.of("key1", LocalizedString.ofEnglish("label1"));
    private static LocalizedEnumValue newSame = LocalizedEnumValue.of("key1", LocalizedString.ofEnglish("label1"));
    private static LocalizedEnumValue newDifferent = LocalizedEnumValue.of("key1", LocalizedString.ofEnglish("label2"));

    @Test
    public void buildChangeLabelAction_WithDifferentValues_ShouldReturnAction() {
        final Optional<UpdateAction<ProductType>> result = buildChangeLabelAction(
            "attribute_definition_name_1",
            old,
            newDifferent
        );

        assertThat(result).contains(ChangeLocalizedEnumValueLabel.of("attribute_definition_name_1", newDifferent));
    }

    @Test
    public void buildChangeLabelAction_WithSameValues_ShouldReturnEmptyOptional() {
        final Optional<UpdateAction<ProductType>> result = buildChangeLabelAction(
            "attribute_definition_name_1",
            old,
            newSame
        );

        assertThat(result).isEmpty();
    }
}
