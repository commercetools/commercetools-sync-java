package com.commercetools.sync.sdk2.commons.utils;

import static com.commercetools.api.models.common.LocalizedString.ofEnglish;
import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;

import com.commercetools.api.models.product_type.AttributeLocalizedEnumValue;
import com.commercetools.api.models.product_type.AttributeLocalizedEnumValueBuilder;
import java.util.List;

public final class LocalizedEnumValueFixtures {

  public static final AttributeLocalizedEnumValue ENUM_VALUE_A =
      AttributeLocalizedEnumValueBuilder.of().key("a").label(ofEnglish("label_a")).build();
  public static final AttributeLocalizedEnumValue ENUM_VALUE_A_DIFFERENT_LABEL =
      AttributeLocalizedEnumValueBuilder.of().key("a").label(ofEnglish("label_a_diff")).build();
  public static final AttributeLocalizedEnumValue ENUM_VALUE_B =
      AttributeLocalizedEnumValueBuilder.of().key("b").label(ofEnglish("label_b")).build();
  public static final AttributeLocalizedEnumValue ENUM_VALUE_C =
      AttributeLocalizedEnumValueBuilder.of().key("c").label(ofEnglish("label_c")).build();
  public static final AttributeLocalizedEnumValue ENUM_VALUE_D =
      AttributeLocalizedEnumValueBuilder.of().key("d").label(ofEnglish("label_d")).build();

  public static final List<AttributeLocalizedEnumValue> ENUM_VALUES_ABC =
      unmodifiableList(asList(ENUM_VALUE_A, ENUM_VALUE_B, ENUM_VALUE_C));
  public static final List<AttributeLocalizedEnumValue> ENUM_VALUES_BAC =
      unmodifiableList(asList(ENUM_VALUE_B, ENUM_VALUE_A, ENUM_VALUE_C));
  public static final List<AttributeLocalizedEnumValue> ENUM_VALUES_AB_WITH_DIFFERENT_LABEL =
      unmodifiableList(asList(ENUM_VALUE_A_DIFFERENT_LABEL, ENUM_VALUE_B));
  public static final List<AttributeLocalizedEnumValue> ENUM_VALUES_AB =
      unmodifiableList(asList(ENUM_VALUE_A, ENUM_VALUE_B));
  public static final List<AttributeLocalizedEnumValue> ENUM_VALUES_ABB =
      unmodifiableList(asList(ENUM_VALUE_A, ENUM_VALUE_B, ENUM_VALUE_B));
  public static final List<AttributeLocalizedEnumValue> ENUM_VALUES_ABD =
      unmodifiableList(asList(ENUM_VALUE_A, ENUM_VALUE_B, ENUM_VALUE_D));
  public static final List<AttributeLocalizedEnumValue> ENUM_VALUES_ABCD =
      unmodifiableList(asList(ENUM_VALUE_A, ENUM_VALUE_B, ENUM_VALUE_C, ENUM_VALUE_D));
  public static final List<AttributeLocalizedEnumValue> ENUM_VALUES_CAB =
      unmodifiableList(asList(ENUM_VALUE_C, ENUM_VALUE_A, ENUM_VALUE_B));
  public static final List<AttributeLocalizedEnumValue> ENUM_VALUES_CB =
      unmodifiableList(asList(ENUM_VALUE_C, ENUM_VALUE_B));
  public static final List<AttributeLocalizedEnumValue> ENUM_VALUES_ACBD =
      unmodifiableList(asList(ENUM_VALUE_A, ENUM_VALUE_C, ENUM_VALUE_B, ENUM_VALUE_D));
  public static final List<AttributeLocalizedEnumValue> ENUM_VALUES_ADBC =
      unmodifiableList(asList(ENUM_VALUE_A, ENUM_VALUE_D, ENUM_VALUE_B, ENUM_VALUE_C));
  public static final List<AttributeLocalizedEnumValue> ENUM_VALUES_CBD =
      unmodifiableList(asList(ENUM_VALUE_C, ENUM_VALUE_B, ENUM_VALUE_D));

  private LocalizedEnumValueFixtures() {}
}
