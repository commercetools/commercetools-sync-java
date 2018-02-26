package com.commercetools.sync.commons.asserts.actions;

import com.fasterxml.jackson.databind.JsonNode;
import io.sphere.sdk.models.ResourceIdentifier;
import io.sphere.sdk.types.Type;
import io.sphere.sdk.types.customupdateactions.SetCustomTypeBase;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;

import static java.util.Optional.ofNullable;
import static org.assertj.core.api.Assertions.assertThat;

public class AbstractSetCustomTypeAssert<T extends AbstractSetCustomTypeAssert<T, S>,
    S extends SetCustomTypeBase> extends AbstractUpdateActionAssert<T, S> {

    AbstractSetCustomTypeAssert(@Nullable final S actual, @Nonnull final Class<T> selfType) {
        super(actual, selfType);
    }

    /**
     * Verifies that the actual {@link SetCustomTypeBase} value has identical fields as the ones supplied.
     * TODO: GITHUB ISSUE#257
     *
     * @param actionName   the update action name.
     * @param customFields the new custom type fields.
     * @param type         the new custom type.
     * @return {@code this} assertion object.
     * @throws AssertionError if the actual value is {@code null}.
     * @throws AssertionError if the actual fields do not match the supplied values.
     */
    @SuppressWarnings("unchecked")
    public AbstractSetCustomTypeAssert hasValues(
        @Nonnull final String actionName,
        @Nullable final Map<String, JsonNode> customFields,
        @Nullable final ResourceIdentifier<Type> type) {

        super.hasValues(actionName);

        assertThat(actual.getFields()).isEqualTo(customFields);
        return ofNullable(type)
            .map(customType -> hasTypeValues(customType.getId(), customType.getKey(), customType.getTypeId()))
            .orElseGet(() -> {
                assertThat(actual.getType()).isNull();
                return myself;
            });
    }

    private AbstractSetCustomTypeAssert hasTypeValues(
        @Nullable final String id,
        @Nullable final String key,
        @Nullable final String typeId) {

        assertThat(actual.getType()).isNotNull();
        assertThat(actual.getType().getId()).isEqualTo(id);
        assertThat(actual.getType().getKey()).isEqualTo(key);
        assertThat(actual.getType().getTypeId()).isEqualTo(typeId);
        return myself;
    }
}
