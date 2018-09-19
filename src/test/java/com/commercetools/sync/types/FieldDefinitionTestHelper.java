package com.commercetools.sync.types;

import io.sphere.sdk.categories.Category;
import io.sphere.sdk.models.EnumValue;
import io.sphere.sdk.models.LocalizedString;
import io.sphere.sdk.models.TextInputHint;
import io.sphere.sdk.types.EnumFieldType;
import io.sphere.sdk.types.FieldDefinition;
import io.sphere.sdk.types.LocalizedStringFieldType;
import io.sphere.sdk.types.ReferenceFieldType;
import io.sphere.sdk.types.SetFieldType;
import io.sphere.sdk.types.StringFieldType;

import java.util.Arrays;
import java.util.List;

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

    public static FieldDefinition stateFieldDefinition() {
        final List<EnumValue> values = Arrays.asList(
                EnumValue.of("published", "the category is publicly visible"),
                EnumValue.of("draft", "the category should not be displayed in the frontend")
        );
        final boolean required = false;
        final LocalizedString label = LocalizedString.ofEnglish("state of the category concerning to show it publicly");
        final String fieldName = "state";
        return FieldDefinition
                .of(EnumFieldType.of(values), fieldName, label, required);
    }

    public static FieldDefinition imageUrlFieldDefinition() {
        final LocalizedString imageUrlLabel =
                LocalizedString.ofEnglish("absolute url to an image to display for the category");
        return FieldDefinition
                .of(StringFieldType.of(), "imageUrl", imageUrlLabel, false, TextInputHint.SINGLE_LINE);
    }

    public static FieldDefinition relatedCategoriesFieldDefinition() {
        final LocalizedString relatedCategoriesLabel =
                LocalizedString.ofEnglish("categories to suggest products similar to the current category");
        //referenceTypeId is required to refere to categories
        final String referenceTypeId = Category.referenceTypeId();
        final SetFieldType setType = SetFieldType.of(ReferenceFieldType.of(referenceTypeId));
        return FieldDefinition
                .of(setType, "relatedCategories", relatedCategoriesLabel, false);
    }
}
