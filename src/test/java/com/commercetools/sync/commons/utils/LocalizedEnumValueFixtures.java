package com.commercetools.sync.commons.utils;

import static io.sphere.sdk.models.LocalizedString.ofEnglish;
import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;

import io.sphere.sdk.models.LocalizedEnumValue;
import java.util.List;

public final class LocalizedEnumValueFixtures {

  public static final LocalizedEnumValue ENUM_VALUE_A =
      LocalizedEnumValue.of("a", ofEnglish("label_a"));
  public static final LocalizedEnumValue ENUM_VALUE_A_DIFFERENT_LABEL =
      LocalizedEnumValue.of("a", ofEnglish("label_a_diff"));
  public static final LocalizedEnumValue ENUM_VALUE_B =
      LocalizedEnumValue.of("b", ofEnglish("label_b"));
  public static final LocalizedEnumValue ENUM_VALUE_C =
      LocalizedEnumValue.of("c", ofEnglish("label_c"));
  public static final LocalizedEnumValue ENUM_VALUE_D =
      LocalizedEnumValue.of("d", ofEnglish("label_d"));

  public static final List<LocalizedEnumValue> ENUM_VALUES_ABC =
      unmodifiableList(asList(ENUM_VALUE_A, ENUM_VALUE_B, ENUM_VALUE_C));
  public static final List<LocalizedEnumValue> ENUM_VALUES_BAC =
      unmodifiableList(asList(ENUM_VALUE_B, ENUM_VALUE_A, ENUM_VALUE_C));
  public static final List<LocalizedEnumValue> ENUM_VALUES_AB_WITH_DIFFERENT_LABEL =
      unmodifiableList(asList(ENUM_VALUE_A_DIFFERENT_LABEL, ENUM_VALUE_B));
  public static final List<LocalizedEnumValue> ENUM_VALUES_AB =
      unmodifiableList(asList(ENUM_VALUE_A, ENUM_VALUE_B));
  public static final List<LocalizedEnumValue> ENUM_VALUES_ABB =
      unmodifiableList(asList(ENUM_VALUE_A, ENUM_VALUE_B, ENUM_VALUE_B));
  public static final List<LocalizedEnumValue> ENUM_VALUES_ABD =
      unmodifiableList(asList(ENUM_VALUE_A, ENUM_VALUE_B, ENUM_VALUE_D));
  public static final List<LocalizedEnumValue> ENUM_VALUES_ABCD =
      unmodifiableList(asList(ENUM_VALUE_A, ENUM_VALUE_B, ENUM_VALUE_C, ENUM_VALUE_D));
  public static final List<LocalizedEnumValue> ENUM_VALUES_CAB =
      unmodifiableList(asList(ENUM_VALUE_C, ENUM_VALUE_A, ENUM_VALUE_B));
  public static final List<LocalizedEnumValue> ENUM_VALUES_CB =
      unmodifiableList(asList(ENUM_VALUE_C, ENUM_VALUE_B));
  public static final List<LocalizedEnumValue> ENUM_VALUES_ACBD =
      unmodifiableList(asList(ENUM_VALUE_A, ENUM_VALUE_C, ENUM_VALUE_B, ENUM_VALUE_D));
  public static final List<LocalizedEnumValue> ENUM_VALUES_ADBC =
      unmodifiableList(asList(ENUM_VALUE_A, ENUM_VALUE_D, ENUM_VALUE_B, ENUM_VALUE_C));
  public static final List<LocalizedEnumValue> ENUM_VALUES_CBD =
      unmodifiableList(asList(ENUM_VALUE_C, ENUM_VALUE_B, ENUM_VALUE_D));

  private LocalizedEnumValueFixtures() {}
}
