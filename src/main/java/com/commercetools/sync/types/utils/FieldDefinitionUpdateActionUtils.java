package com.commercetools.sync.types.utils;

import com.commercetools.sync.commons.exceptions.DuplicateKeyException;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.models.EnumValue;
import io.sphere.sdk.models.LocalizedEnumValue;
import io.sphere.sdk.models.LocalizedString;
import io.sphere.sdk.types.EnumFieldType;
import io.sphere.sdk.types.FieldDefinition;
import io.sphere.sdk.types.FieldType;
import io.sphere.sdk.types.LocalizedEnumFieldType;
import io.sphere.sdk.types.SetFieldType;
import io.sphere.sdk.types.Type;
import io.sphere.sdk.types.commands.updateactions.ChangeFieldDefinitionLabel;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static com.commercetools.sync.commons.utils.CommonTypeUpdateActionUtils.buildUpdateAction;
import static com.commercetools.sync.commons.utils.OptionalUtils.filterEmptyOptionals;
import static com.commercetools.sync.types.utils.LocalizedEnumValueUpdateActionUtils.buildLocalizedEnumValuesUpdateActions;
import static com.commercetools.sync.types.utils.PlainEnumValueUpdateActionUtils.buildEnumValuesUpdateActions;

/**
 * This class is only meant for the internal use of the commercetools-sync-java library.
 */
final class FieldDefinitionUpdateActionUtils {

    /**
     * Compares all the fields of an old {@link FieldDefinition} with a new {@link FieldDefinition} and returns
     * a list of {@link UpdateAction}&lt;{@link Type}&gt; as a result. If both the {@link FieldDefinition}
     * and the {@link FieldDefinition} have identical fields, then no update action is needed and hence an
     * empty {@link List} is returned.
     *
     * @param oldFieldDefinition the old field definition which should be updated.
     * @param newFieldDefinition the new field definition where we get the new fields.
     * @return A list with the update actions or an empty list if the field definition fields are identical.
     *
     * @throws DuplicateKeyException in case there are localized enum values with duplicate keys.
     */
    @Nonnull
    static List<UpdateAction<Type>> buildActions(
        @Nonnull final FieldDefinition oldFieldDefinition,
        @Nonnull final FieldDefinition newFieldDefinition) {

        final List<UpdateAction<Type>> updateActions =
            filterEmptyOptionals(buildChangeLabelUpdateAction(oldFieldDefinition, newFieldDefinition));

        updateActions.addAll(buildEnumUpdateActions(oldFieldDefinition, newFieldDefinition));

        return updateActions;
    }

    /**
     * Compares all the {@link EnumValue} and {@link LocalizedEnumValue} values of {@link FieldDefinition}s and returns
     * a list of {@link UpdateAction}&lt;{@link Type}&gt; as a result. If both {@link FieldDefinition}s have identical
     * enum values, then no update action is needed and hence an empty {@link List} is returned.
     *
     * <p> On CTP, enum update actions can only update a enum type field definition or a Set of
     * enum type field definition. Therefore, this method will update build enum update actions of the types supplied
     * are of the same type and are either of Enum field type or LocalizedEnum field type or set of either.</p>
     *
     * @param oldFieldDefinition the old field definition which should be updated.
     * @param newFieldDefinition the new field definition where we get the new fields.
     * @return A list with the update actions or an empty list if the field definition enums are identical.
     *
     * @throws DuplicateKeyException in case there are localized enum values with duplicate keys.
     */
    @Nonnull
    static List<UpdateAction<Type>> buildEnumUpdateActions(
        @Nonnull final FieldDefinition oldFieldDefinition,
        @Nonnull final FieldDefinition newFieldDefinition) {

        final FieldType oldFieldDefinitionType = oldFieldDefinition.getType();
        final FieldType newFieldDefinitionType = newFieldDefinition.getType();


        // Check if the fieldType is of type Enum or Set (of set of set..) of Enums
        return getEnumFieldType(oldFieldDefinitionType)
            .map(oldEnumFieldType ->
                getEnumFieldType(newFieldDefinitionType)
                    .map(newEnumFieldType ->
                        buildEnumValuesUpdateActions(oldFieldDefinition.getName(),
                            oldEnumFieldType.getValues(),
                            newEnumFieldType.getValues())
                    )
                    .orElseGet(Collections::emptyList)
            )
            .orElseGet(() -> // Check if the fieldType is a LocalizedEnums or Set (of set..) of LocalizedEnums
                getLocalizedEnumFieldType(oldFieldDefinitionType)
                    .map(oldLocalizedEnumFieldType ->
                        getLocalizedEnumFieldType(newFieldDefinitionType)
                            .map(newLocalizedEnumFieldType ->

                                buildLocalizedEnumValuesUpdateActions(oldFieldDefinition.getName(),
                                    oldLocalizedEnumFieldType.getValues(),
                                    newLocalizedEnumFieldType.getValues())

                            )
                            .orElseGet(Collections::emptyList)
                    )
                    .orElseGet(Collections::emptyList)
            );
    }

    /**
     * Indicates if the field type is a plain enum value or a set of plain enum value.
     *
     * @param fieldType the field type.
     * @return an optional containing the enum field type if the field type is an enum field type
     *          or if it is a set of enum field type, then it returns an optional containing the localized enum field
     *          type under the set type.
     */
    private static Optional<EnumFieldType> getEnumFieldType(
        @Nonnull final FieldType fieldType) {

        if (fieldType instanceof EnumFieldType) {
            return Optional.of((EnumFieldType) fieldType);
        }

        if (fieldType instanceof SetFieldType) {

            final SetFieldType setFieldType = (SetFieldType) fieldType;
            final FieldType subType = setFieldType.getElementType();

            if (subType instanceof EnumFieldType) {
                return Optional.of((EnumFieldType) subType);
            }
        }

        return Optional.empty();
    }

    /**
     * Indicates if the field type is a localized enum value or a set of localized enum value.
     *
     * @param fieldType the field type.
     * @return an optional containing the localized enum field type if the field type is a localized enum field type
     *          or if is a set of localized enum field type, then it returns an optional containing the localized enum
     *          field type under the set type.
     */
    private static Optional<LocalizedEnumFieldType> getLocalizedEnumFieldType(
        @Nonnull final FieldType fieldType) {

        if (fieldType instanceof LocalizedEnumFieldType) {
            return Optional.of((LocalizedEnumFieldType) fieldType);
        }

        if (fieldType instanceof SetFieldType) {
            final SetFieldType setFieldType = (SetFieldType) fieldType;
            final FieldType subType = setFieldType.getElementType();

            if (subType instanceof LocalizedEnumFieldType) {
                return Optional.of((LocalizedEnumFieldType) subType);
            }
        }

        return Optional.empty();
    }

    /**
     * Compares the {@link LocalizedString} labels of old {@link FieldDefinition} with new
     * {@link FieldDefinition} and returns an {@link UpdateAction}&lt;{@link Type}&gt; as a result in
     * an {@link Optional}. If both the {@link FieldDefinition} and the {@link FieldDefinition} have the
     * same label, then no update action is needed and hence an empty {@link Optional} is returned.
     *
     * @param oldFieldDefinition the old field definition which should be updated.
     * @param newFieldDefinition the new field definition draft where we get the new label.
     * @return A filled optional with the update action or an empty optional if the labels are identical.
     */
    @Nonnull
    static Optional<UpdateAction<Type>> buildChangeLabelUpdateAction(
        @Nonnull final FieldDefinition oldFieldDefinition,
        @Nonnull final FieldDefinition newFieldDefinition) {

        return buildUpdateAction(oldFieldDefinition.getLabel(), newFieldDefinition.getLabel(),
            () -> ChangeFieldDefinitionLabel.of(oldFieldDefinition.getName(), newFieldDefinition.getLabel())
        );
    }

    private FieldDefinitionUpdateActionUtils() {
    }
}
