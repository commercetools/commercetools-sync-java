package com.commercetools.sync.types.utils;

import static com.commercetools.api.models.common.LocalizedString.ofEnglish;

import com.commercetools.api.models.type.FieldDefinition;
import com.commercetools.api.models.type.FieldDefinitionBuilder;
import com.commercetools.api.models.type.FieldTypeBuilder;
import com.commercetools.api.models.type.TypeTextInputHint;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class FieldDefinitionFixtures {
  private static final String RES_ROOT =
      "com/commercetools/sync/types/utils/updatefielddefinitions/fields/";

  static final String TYPE_WITH_FIELDS_AB = RES_ROOT + "type-with-field-definitions-ab.json";
  static final String TYPE_WITH_FIELDS_ABB = RES_ROOT + "type-with-field-definitions-abb.json";
  static final String TYPE_WITH_FIELDS_ABC = RES_ROOT + "type-with-field-definitions-abc.json";
  static final String TYPE_WITH_FIELDS_ABCD = RES_ROOT + "type-with-field-definitions-abcd.json";
  static final String TYPE_WITH_FIELDS_ABD = RES_ROOT + "type-with-field-definitions-abd.json";
  static final String TYPE_WITH_FIELDS_CAB = RES_ROOT + "type-with-field-definitions-cab.json";
  static final String TYPE_WITH_FIELDS_CB = RES_ROOT + "type-with-field-definitions-cb.json";
  static final String TYPE_WITH_FIELDS_ACBD = RES_ROOT + "type-with-field-definitions-acbd.json";
  static final String TYPE_WITH_FIELDS_ADBC = RES_ROOT + "type-with-field-definitions-adbc.json";
  static final String TYPE_WITH_FIELDS_CBD = RES_ROOT + "type-with-field-definitions-cbd.json";
  static final String TYPE_WITH_FIELDS_ABC_WITH_DIFFERENT_TYPE =
      RES_ROOT + "type-with-field-definitions-abc-with-different-type.json";

  static final String FIELD_A = "a";
  static final String FIELD_B = "b";
  static final String FIELD_C = "c";
  static final String FIELD_D = "d";
  static final String LABEL_EN = "label_en";

  static final FieldDefinition FIELD_DEFINITION_A =
      stringFieldDefinition(FIELD_A, LABEL_EN, false, TypeTextInputHint.SINGLE_LINE);
  static final FieldDefinition FIELD_DEFINITION_A_LOCALIZED_TYPE =
      localizedStringFieldDefinition(FIELD_A, LABEL_EN, false, TypeTextInputHint.SINGLE_LINE);
  static final FieldDefinition FIELD_DEFINITION_B =
      stringFieldDefinition(FIELD_B, LABEL_EN, false, TypeTextInputHint.SINGLE_LINE);
  static final FieldDefinition FIELD_DEFINITION_C =
      stringFieldDefinition(FIELD_C, LABEL_EN, false, TypeTextInputHint.SINGLE_LINE);
  static final FieldDefinition FIELD_DEFINITION_D =
      stringFieldDefinition(FIELD_D, LABEL_EN, false, TypeTextInputHint.SINGLE_LINE);

  static FieldDefinition stringFieldDefinition(
      @Nonnull final String fieldName,
      @Nonnull final String labelEng,
      boolean required,
      @Nullable final TypeTextInputHint hint) {
    return FieldDefinitionBuilder.of()
        .type(FieldTypeBuilder::stringBuilder)
        .name(fieldName)
        .label(ofEnglish(labelEng))
        .required(required)
        .inputHint(hint)
        .build();
  }

  private static FieldDefinition localizedStringFieldDefinition(
      @Nonnull final String fieldName,
      @Nonnull final String labelEng,
      boolean required,
      @Nullable final TypeTextInputHint hint) {
    return FieldDefinitionBuilder.of()
        .type(FieldTypeBuilder::localizedStringBuilder)
        .name(fieldName)
        .label(ofEnglish(labelEng))
        .required(required)
        .inputHint(hint)
        .build();
  }

  private FieldDefinitionFixtures() {}
}
