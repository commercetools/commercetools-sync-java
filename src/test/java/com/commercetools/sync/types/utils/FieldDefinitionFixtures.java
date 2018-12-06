package com.commercetools.sync.types;

import io.sphere.sdk.models.LocalizedString;
import io.sphere.sdk.models.TextInputHint;
import io.sphere.sdk.types.FieldDefinition;
import io.sphere.sdk.types.LocalizedStringFieldType;
import io.sphere.sdk.types.StringFieldType;

public final class FieldDefinitionTestHelper {

    public static FieldDefinition stringFieldDefinition(final String fieldName,
                                                        final String labelEng,
                                                        boolean required,
                                                        final TextInputHint hint) {
        return FieldDefinition.of(StringFieldType.of(),
                        fieldName,
                        LocalizedString.ofEnglish(labelEng),
                        required,
                        hint);
    }

    public static FieldDefinition localizedStringFieldDefinition(final String fieldName,
                                                        final String labelEng,
                                                        boolean required,
                                                        final TextInputHint hint) {
        return FieldDefinition.of(LocalizedStringFieldType.of(),
                fieldName,
                LocalizedString.ofEnglish(labelEng),
                required,
                hint);
    }

    private FieldDefinitionTestHelper() {
    }
}
