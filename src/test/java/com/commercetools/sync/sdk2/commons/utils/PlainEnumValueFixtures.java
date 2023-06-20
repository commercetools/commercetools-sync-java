package com.commercetools.sync.sdk2.commons.utils;

import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;

import com.commercetools.api.models.product_type.AttributePlainEnumValue;
import com.commercetools.api.models.product_type.AttributePlainEnumValueBuilder;
import java.util.List;

public final class PlainEnumValueFixtures {

  public static final AttributePlainEnumValue ENUM_VALUE_A =
      AttributePlainEnumValueBuilder.of().key("a").label("label_a").build();
  public static final AttributePlainEnumValue ENUM_VALUE_A_DIFFERENT_LABEL =
      AttributePlainEnumValueBuilder.of().key("a").label("label_a_diff").build();
  public static final AttributePlainEnumValue ENUM_VALUE_B =
      AttributePlainEnumValueBuilder.of().key("b").label("label_b").build();
  public static final AttributePlainEnumValue ENUM_VALUE_C =
      AttributePlainEnumValueBuilder.of().key("c").label("label_c").build();
  public static final AttributePlainEnumValue ENUM_VALUE_D =
      AttributePlainEnumValueBuilder.of().key("d").label("label_d").build();

  public static final List<AttributePlainEnumValue> ENUM_VALUES_ABC =
      unmodifiableList(asList(ENUM_VALUE_A, ENUM_VALUE_B, ENUM_VALUE_C));
  public static final List<AttributePlainEnumValue> ENUM_VALUES_AB =
      unmodifiableList(asList(ENUM_VALUE_A, ENUM_VALUE_B));
  public static final List<AttributePlainEnumValue> ENUM_VALUES_BAC =
      unmodifiableList(asList(ENUM_VALUE_B, ENUM_VALUE_A, ENUM_VALUE_C));
  public static final List<AttributePlainEnumValue> ENUM_VALUES_AB_WITH_DIFFERENT_LABEL =
      unmodifiableList(asList(ENUM_VALUE_A_DIFFERENT_LABEL, ENUM_VALUE_B));
  public static final List<AttributePlainEnumValue> ENUM_VALUES_ABB =
      unmodifiableList(asList(ENUM_VALUE_A, ENUM_VALUE_B, ENUM_VALUE_B));
  public static final List<AttributePlainEnumValue> ENUM_VALUES_ABD =
      unmodifiableList(asList(ENUM_VALUE_A, ENUM_VALUE_B, ENUM_VALUE_D));
  public static final List<AttributePlainEnumValue> ENUM_VALUES_ABCD =
      unmodifiableList(asList(ENUM_VALUE_A, ENUM_VALUE_B, ENUM_VALUE_C, ENUM_VALUE_D));
  public static final List<AttributePlainEnumValue> ENUM_VALUES_CAB =
      unmodifiableList(asList(ENUM_VALUE_C, ENUM_VALUE_A, ENUM_VALUE_B));
  public static final List<AttributePlainEnumValue> ENUM_VALUES_CB =
      unmodifiableList(asList(ENUM_VALUE_C, ENUM_VALUE_B));
  public static final List<AttributePlainEnumValue> ENUM_VALUES_ACBD =
      unmodifiableList(asList(ENUM_VALUE_A, ENUM_VALUE_C, ENUM_VALUE_B, ENUM_VALUE_D));
  public static final List<AttributePlainEnumValue> ENUM_VALUES_ADBC =
      unmodifiableList(asList(ENUM_VALUE_A, ENUM_VALUE_D, ENUM_VALUE_B, ENUM_VALUE_C));
  public static final List<AttributePlainEnumValue> ENUM_VALUES_CBD =
      unmodifiableList(asList(ENUM_VALUE_C, ENUM_VALUE_B, ENUM_VALUE_D));

  private PlainEnumValueFixtures() {}
}
