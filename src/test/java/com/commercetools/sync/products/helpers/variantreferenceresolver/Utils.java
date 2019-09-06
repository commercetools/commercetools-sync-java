package com.commercetools.sync.products.helpers.variantreferenceresolver;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import javax.annotation.Nonnull;
import java.util.Map;

import static com.commercetools.sync.products.helpers.VariantReferenceResolver.REFERENCE_ID_FIELD;
import static com.commercetools.sync.products.helpers.VariantReferenceResolver.REFERENCE_TYPE_ID_FIELD;
import static org.assertj.core.api.Assertions.assertThat;

public final class Utils {
    public static void assertReferenceAttributeValue(
        @Nonnull final Map<String, JsonNode> attributeDraftMap,
        @Nonnull final String attributeName,
        @Nonnull final String referenceId,
        @Nonnull final String referenceTypeId) {

        assertThat(attributeDraftMap.get(attributeName)).isNotNull();
        assertThat(attributeDraftMap.get(attributeName).get("value")).isNotNull();
        assertThat(attributeDraftMap.get(attributeName)
                                    .get("value")
                                    .get(REFERENCE_ID_FIELD).asText()).isEqualTo(referenceId);
        assertThat(attributeDraftMap.get(attributeName)
                                    .get("value")
                                    .get(REFERENCE_TYPE_ID_FIELD).asText()).isEqualTo(referenceTypeId);
    }

    public static void assertReferenceSetAttributeValue(
        @Nonnull final Map<String, JsonNode> attributeDraftMap,
        @Nonnull final String attributeName,
        final int numberOfReferences,
        @Nonnull final String referenceId,
        @Nonnull final String referenceTypeId) {

        assertThat(attributeDraftMap.get(attributeName)).isNotNull();
        final JsonNode value = attributeDraftMap.get(attributeName).get("value");
        assertThat(value).isInstanceOf(ArrayNode.class);

        final ArrayNode valueAsArrayNode = (ArrayNode) value;
        assertThat(valueAsArrayNode).hasSize(numberOfReferences);
        assertThat(valueAsArrayNode).allSatisfy(jsonNode -> {
            assertThat(jsonNode.get(REFERENCE_ID_FIELD).asText()).isEqualTo(referenceId);
            assertThat(jsonNode.get(REFERENCE_TYPE_ID_FIELD).asText()).isEqualTo(referenceTypeId);
        });
    }

    private Utils() {
    }
}
