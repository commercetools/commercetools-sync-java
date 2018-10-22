package com.commercetools.sync.commons.utils.enums;

import io.sphere.sdk.models.EnumValue;

import java.util.List;

import static java.util.Arrays.asList;

public final class PlainEnumValueTestObjects {

    public static final EnumValue ENUM_VALUE_A = EnumValue.of("a", "label_a");
    public static final EnumValue ENUM_VALUE_A_DIFFERENT_LABEL = EnumValue.of("a", "label_a_diff");
    public static final EnumValue ENUM_VALUE_B = EnumValue.of("b", "label_b");
    public static final EnumValue ENUM_VALUE_C = EnumValue.of("c", "label_c");
    public static final EnumValue ENUM_VALUE_D = EnumValue.of("d", "label_d");

    public static final List<EnumValue> ENUM_VALUES_ABC = asList(ENUM_VALUE_A, ENUM_VALUE_B, ENUM_VALUE_C);
    public static final List<EnumValue> ENUM_VALUES_AB = asList(ENUM_VALUE_A, ENUM_VALUE_B);
    public static final List<EnumValue> ENUM_VALUES_AB_WITH_DIFFERENT_LABEL =
        asList(ENUM_VALUE_A_DIFFERENT_LABEL, ENUM_VALUE_B);
    public static final List<EnumValue> ENUM_VALUES_ABB = asList(ENUM_VALUE_A, ENUM_VALUE_B, ENUM_VALUE_B);
    public static final List<EnumValue> ENUM_VALUES_ABD = asList(ENUM_VALUE_A, ENUM_VALUE_B, ENUM_VALUE_D);
    public static final List<EnumValue> ENUM_VALUES_ABCD = asList(
            ENUM_VALUE_A,
            ENUM_VALUE_B,
            ENUM_VALUE_C,
            ENUM_VALUE_D
    );
    public static final List<EnumValue> ENUM_VALUES_CAB = asList(ENUM_VALUE_C, ENUM_VALUE_A, ENUM_VALUE_B);
    public static final List<EnumValue> ENUM_VALUES_CB = asList(ENUM_VALUE_C, ENUM_VALUE_B);
    public static final List<EnumValue> ENUM_VALUES_ACBD = asList(
            ENUM_VALUE_A,
            ENUM_VALUE_C,
            ENUM_VALUE_B,
            ENUM_VALUE_D
    );
    public static final List<EnumValue> ENUM_VALUES_ADBC = asList(
            ENUM_VALUE_A,
            ENUM_VALUE_D,
            ENUM_VALUE_B,
            ENUM_VALUE_C
    );
    public static final List<EnumValue> ENUM_VALUES_CBD = asList(ENUM_VALUE_C, ENUM_VALUE_B, ENUM_VALUE_D);

}
