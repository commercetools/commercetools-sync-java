package com.commercetools.sync.commons.utils;

import io.sphere.sdk.models.LocalizedEnumValue;

import java.util.List;

import static io.sphere.sdk.models.LocalizedString.ofEnglish;
import static java.util.Arrays.asList;

public final class LocalizedEnumValueTestObjects {

    public static final LocalizedEnumValue ENUM_VALUE_A = LocalizedEnumValue.of("a", ofEnglish("label_a"));
    public static final LocalizedEnumValue ENUM_VALUE_A_DIFFERENT_LABEL =
        LocalizedEnumValue.of("a", ofEnglish("label_a_diff"));
    public static final LocalizedEnumValue ENUM_VALUE_B = LocalizedEnumValue.of("b", ofEnglish("label_b"));
    public static final LocalizedEnumValue ENUM_VALUE_C = LocalizedEnumValue.of("c", ofEnglish("label_c"));
    public static final LocalizedEnumValue ENUM_VALUE_D = LocalizedEnumValue.of("d", ofEnglish("label_d"));

    public static final List<LocalizedEnumValue> ENUM_VALUES_ABC = asList(ENUM_VALUE_A, ENUM_VALUE_B, ENUM_VALUE_C);
    public static final List<LocalizedEnumValue> ENUM_VALUES_AB = asList(ENUM_VALUE_A, ENUM_VALUE_B);
    public static final List<LocalizedEnumValue> ENUM_VALUES_ABB = asList(ENUM_VALUE_A, ENUM_VALUE_B, ENUM_VALUE_B);
    public static final List<LocalizedEnumValue> ENUM_VALUES_ABD = asList(ENUM_VALUE_A, ENUM_VALUE_B, ENUM_VALUE_D);
    public static final List<LocalizedEnumValue> ENUM_VALUES_ABCD = asList(
            ENUM_VALUE_A,
            ENUM_VALUE_B,
            ENUM_VALUE_C,
            ENUM_VALUE_D
    );
    public static final List<LocalizedEnumValue> ENUM_VALUES_CAB = asList(ENUM_VALUE_C, ENUM_VALUE_A, ENUM_VALUE_B);
    public static final List<LocalizedEnumValue> ENUM_VALUES_CB = asList(ENUM_VALUE_C, ENUM_VALUE_B);
    public static final List<LocalizedEnumValue> ENUM_VALUES_ACBD = asList(
            ENUM_VALUE_A,
            ENUM_VALUE_C,
            ENUM_VALUE_B,
            ENUM_VALUE_D
    );
    public static final List<LocalizedEnumValue> ENUM_VALUES_ADBC = asList(
            ENUM_VALUE_A,
            ENUM_VALUE_D,
            ENUM_VALUE_B,
            ENUM_VALUE_C
    );
    public static final List<LocalizedEnumValue> ENUM_VALUES_CBD = asList(ENUM_VALUE_C, ENUM_VALUE_B, ENUM_VALUE_D);

}
