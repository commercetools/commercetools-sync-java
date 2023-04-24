package com.commercetools.sync.sdk2.products.helpers.variantreferenceresolver;

import static org.assertj.core.api.Assertions.assertThat;

import com.commercetools.api.models.common.Reference;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;

public final class AssertionUtilsForVariantReferenceResolver {

  public static void assertReferenceAttributeValue(
      @Nonnull final Map<String, Object> attributeDraftMap,
      @Nonnull final String attributeName,
      @Nonnull final String referenceId,
      @Nonnull final String referenceTypeId) {

    assertThat(attributeDraftMap.get(attributeName)).isNotNull();
    if (attributeDraftMap.get(attributeName) instanceof Reference) {
      final Reference ref = (Reference) attributeDraftMap.get(attributeName);
      assertThat(ref.getId()).isEqualTo(referenceId);
      assertThat(ref.getTypeId().getJsonName()).isEqualTo(referenceTypeId);
    }
  }

  public static void assertReferenceSetAttributeValue(
      @Nonnull final Map<String, Object> attributeDraftMap,
      @Nonnull final String attributeName,
      final int numberOfReferences,
      @Nonnull final String referenceId,
      @Nonnull final String referenceTypeId) {

    assertThat(attributeDraftMap.get(attributeName)).isNotNull();
    assertThat(attributeDraftMap.get(attributeName)).isInstanceOf(List.class);
    final List value = (List) attributeDraftMap.get(attributeName);

    assertThat(value).hasSize(numberOfReferences);
    assertThat(value)
        .allSatisfy(
            obj -> {
              assertThat(obj).isInstanceOf(Reference.class);
              final Reference ref = (Reference) obj;
              assertThat(ref.getId()).isEqualTo(referenceId);
              assertThat(ref.getTypeId().getJsonName()).isEqualTo(referenceTypeId);
            });
  }

  public static void assertReferenceSetAttributeValue(
      @Nonnull final Map<String, Object> attributeDraftMap,
      @Nonnull final String attributeName,
      @Nonnull final Reference... expectedReferences) {

    assertThat(attributeDraftMap.get(attributeName)).isNotNull();
    assertThat(attributeDraftMap.get(attributeName)).isInstanceOf(List.class);
    final List<Reference> value = (List) attributeDraftMap.get(attributeName);

    assertThat(value).containsAll(Arrays.asList(expectedReferences));
  }

  private AssertionUtilsForVariantReferenceResolver() {}
}
