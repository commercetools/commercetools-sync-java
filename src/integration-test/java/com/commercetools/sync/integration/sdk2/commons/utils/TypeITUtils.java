package com.commercetools.sync.integration.sdk2.commons.utils;

import static com.commercetools.sync.integration.sdk2.commons.utils.ITUtils.*;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.common.LocalizedString;
import com.commercetools.api.models.type.*;
import java.util.Optional;
import javax.annotation.Nonnull;

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

  /**
   * Tries to fetch type of {@code key} using {@code projectApiRoot}.
   *
   * @param ctpClient client used to execute requests.
   * @param typeKey key of requested type.
   * @return {@link Optional} which may contain type of {@code key}.
   */
  public static Optional<Type> getTypeByKey(
      @Nonnull final ProjectApiRoot ctpClient, @Nonnull final String typeKey) {

    return typeExists(typeKey, ctpClient).toCompletableFuture().join();
  }

  private TypeITUtils() {}
}
