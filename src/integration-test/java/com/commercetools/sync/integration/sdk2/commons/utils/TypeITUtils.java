package com.commercetools.sync.integration.sdk2.commons.utils;

import static com.commercetools.sync.integration.sdk2.commons.utils.ITUtils.*;
import static com.commercetools.sync.integration.sdk2.commons.utils.TestClientUtils.CTP_SOURCE_CLIENT;
import static com.commercetools.sync.integration.sdk2.commons.utils.TestClientUtils.CTP_TARGET_CLIENT;
import static java.util.Collections.singletonList;

import com.commercetools.api.models.common.LocalizedString;
import com.commercetools.api.models.type.*;
import java.util.Arrays;

public final class TypeITUtils {
  public static final String TYPE_KEY_1 = "key_1";
  public static final String TYPE_KEY_2 = "key_2";

  public static final LocalizedString TYPE_NAME_1 = LocalizedString.ofEnglish("name_1");
  public static final LocalizedString TYPE_NAME_2 = LocalizedString.ofEnglish("name_2");

  public static final String FIELD_DEFINITION_NAME_1 = "field_name_1";
  private static final String FIELD_DEFINITION_NAME_2 = "field_name_2";
  private static final String FIELD_DEFINITION_NAME_3 = "field_name_3";

  public static final LocalizedString FIELD_DEFINITION_LABEL_1 =
      LocalizedString.ofEnglish("label_1");
  private static final LocalizedString FIELD_DEFINITION_LABEL_2 =
      LocalizedString.ofEnglish("label_2");
  private static final LocalizedString FIELD_DEFINITION_LABEL_3 =
      LocalizedString.ofEnglish("label_3");

  public static final LocalizedString TYPE_DESCRIPTION_1 =
      LocalizedString.ofEnglish("description_1");
  public static final LocalizedString TYPE_DESCRIPTION_2 =
      LocalizedString.ofEnglish("description_2");

  public static final FieldDefinition FIELD_DEFINITION_1 =
      FieldDefinitionBuilder.of()
          .type(CustomFieldStringTypeBuilder.of().build())
          .name(FIELD_DEFINITION_NAME_1)
          .label(FIELD_DEFINITION_LABEL_1)
          .required(true)
          .inputHint(TypeTextInputHint.SINGLE_LINE)
          .build();
  public static final FieldDefinition FIELD_DEFINITION_2 =
      FieldDefinitionBuilder.of()
          .type(CustomFieldStringTypeBuilder.of().build())
          .name(FIELD_DEFINITION_NAME_2)
          .label(FIELD_DEFINITION_LABEL_2)
          .required(true)
          .inputHint(TypeTextInputHint.SINGLE_LINE)
          .build();
  public static final FieldDefinition FIELD_DEFINITION_3 =
      FieldDefinitionBuilder.of()
          .type(CustomFieldStringTypeBuilder.of().build())
          .name(FIELD_DEFINITION_NAME_3)
          .label(FIELD_DEFINITION_LABEL_3)
          .required(true)
          .inputHint(TypeTextInputHint.SINGLE_LINE)
          .build();

  private static final TypeDraft typeDraft1 =
      TypeDraftBuilder.of()
          .key(TYPE_KEY_1)
          .name(TYPE_NAME_1)
          .description(TYPE_DESCRIPTION_1)
          .fieldDefinitions(Arrays.asList(FIELD_DEFINITION_1, FIELD_DEFINITION_2))
          .resourceTypeIds(ResourceTypeId.CATEGORY)
          .build();
  private static final TypeDraft typeDraft2 =
      TypeDraftBuilder.of()
          .key(TYPE_KEY_2)
          .name(TYPE_NAME_2)
          .description(TYPE_DESCRIPTION_2)
          .fieldDefinitions(singletonList(FIELD_DEFINITION_2))
          .resourceTypeIds(ResourceTypeId.CATEGORY)
          .build();

  /**
   * Populate source CTP project. Creates type with key TYPE_KEY_1, TYPE_NAME_1, TYPE_DESCRIPTION_1
   * and fields FIELD_DEFINITION_1, FIELD_DEFINITION_2. Creates type with key TYPE_KEY_2,
   * TYPE_NAME_2, TYPE_DESCRIPTION_2 and fields FIELD_DEFINITION_1.
   */
  public static void populateSourceProject() {
    ensureTypeByTypeDraft(typeDraft1, CTP_SOURCE_CLIENT);
    ensureTypeByTypeDraft(typeDraft2, CTP_SOURCE_CLIENT);
  }

  /**
   * Populate target CTP project. Creates type with key TYPE_KEY_1, TYPE_NAME_1, TYPE_DESCRIPTION_1
   * and fields FIELD_DEFINITION_1, FIELD_DEFINITION_2.
   */
  public static void populateTargetProject() {
    ensureTypeByTypeDraft(typeDraft1, CTP_TARGET_CLIENT);
  }

  private TypeITUtils() {}
}
