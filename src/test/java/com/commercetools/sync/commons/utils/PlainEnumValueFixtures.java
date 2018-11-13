package com.commercetools.sync.commons.utils;

import io.sphere.sdk.models.EnumValue;

import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;

public final class PlainEnumValueFixtures {

    public static final EnumValue ENUM_VALUE_A = EnumValue.of("a", "label_a");
    public static final EnumValue ENUM_VALUE_A_DIFFERENT_LABEL = EnumValue.of("a", "label_a_diff");
    public static final EnumValue ENUM_VALUE_B = EnumValue.of("b", "label_b");
    public static final EnumValue ENUM_VALUE_C = EnumValue.of("c", "label_c");
    public static final EnumValue ENUM_VALUE_D = EnumValue.of("d", "label_d");

    public static final List<EnumValue> ENUM_VALUES_ABC =
        unmodifiableList(asList(ENUM_VALUE_A, ENUM_VALUE_B, ENUM_VALUE_C));
    public static final List<EnumValue> ENUM_VALUES_AB =
        unmodifiableList(asList(ENUM_VALUE_A, ENUM_VALUE_B));
    public static final List<EnumValue> ENUM_VALUES_AB_WITH_DIFFERENT_LABEL =
        unmodifiableList(asList(ENUM_VALUE_A_DIFFERENT_LABEL, ENUM_VALUE_B));
    public static final List<EnumValue> ENUM_VALUES_ABB =
        unmodifiableList(asList(ENUM_VALUE_A, ENUM_VALUE_B, ENUM_VALUE_B));
    public static final List<EnumValue> ENUM_VALUES_ABD =
        unmodifiableList(asList(ENUM_VALUE_A, ENUM_VALUE_B, ENUM_VALUE_D));
    public static final List<EnumValue> ENUM_VALUES_ABCD =
        unmodifiableList(asList(ENUM_VALUE_A, ENUM_VALUE_B, ENUM_VALUE_C, ENUM_VALUE_D));
    public static final List<EnumValue> ENUM_VALUES_CAB =
        unmodifiableList(asList(ENUM_VALUE_C, ENUM_VALUE_A, ENUM_VALUE_B));
    public static final List<EnumValue> ENUM_VALUES_CB =
        unmodifiableList(asList(ENUM_VALUE_C, ENUM_VALUE_B));
    public static final List<EnumValue> ENUM_VALUES_ACBD =
        unmodifiableList(asList(ENUM_VALUE_A, ENUM_VALUE_C, ENUM_VALUE_B, ENUM_VALUE_D));
    public static final List<EnumValue> ENUM_VALUES_ADBC =
        unmodifiableList(asList(ENUM_VALUE_A, ENUM_VALUE_D, ENUM_VALUE_B, ENUM_VALUE_C));
    public static final List<EnumValue> ENUM_VALUES_CBD =
        unmodifiableList(asList(ENUM_VALUE_C, ENUM_VALUE_B, ENUM_VALUE_D));

    private PlainEnumValueFixtures() {
    }
}
