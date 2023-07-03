package com.commercetools.sync.sdk2.types.utils;

import static com.commercetools.sync.sdk2.types.utils.TypeUpdateActionUtils.buildChangeNameUpdateAction;
import static com.commercetools.sync.sdk2.types.utils.TypeUpdateActionUtils.buildSetDescriptionUpdateAction;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.commercetools.api.models.common.LocalizedString;
import com.commercetools.api.models.type.*;
import java.util.Optional;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class TypeUpdateActionUtilsTest {
  private static Type old;
  private static TypeDraft newSame;
  private static TypeDraft newDifferent;

  /** Initialises test data. */
  @BeforeAll
  static void setup() {
    final String key = "category-custom-type-key";
    final LocalizedString name = LocalizedString.ofEnglish("type for standard categories");
    final LocalizedString desc = LocalizedString.ofEnglish("description for category custom type");

    old = mock(Type.class);
    when(old.getKey()).thenReturn(key);
    when(old.getName()).thenReturn(name);
    when(old.getDescription()).thenReturn(desc);

    newSame =
        TypeDraftBuilder.of()
            .key(key)
            .name(name)
            .resourceTypeIds(ResourceTypeId.CATEGORY)
            .description(desc)
            .build();

    newDifferent =
        TypeDraftBuilder.of()
            .key("category-custom-type-key-2")
            .name(LocalizedString.ofEnglish("type for standard categories 2"))
            .resourceTypeIds(ResourceTypeId.CATEGORY)
            .description(LocalizedString.ofEnglish("description for category custom type 2"))
            .build();
  }

  @Test
  void buildChangeNameAction_WithDifferentValues_ShouldReturnAction() {
    final Optional<TypeUpdateAction> result = buildChangeNameUpdateAction(old, newDifferent);

    assertThat(result)
        .contains(TypeChangeNameActionBuilder.of().name(newDifferent.getName()).build());
  }

  @Test
  void buildChangeNameAction_WithSameValues_ShouldReturnEmptyOptional() {
    final Optional<TypeUpdateAction> result = buildChangeNameUpdateAction(old, newSame);

    assertThat(result).isEmpty();
  }

  @Test
  void buildSetDescriptionAction_WithDifferentValues_ShouldReturnAction() {
    final Optional<TypeUpdateAction> result = buildSetDescriptionUpdateAction(old, newDifferent);

    assertThat(result)
        .contains(
            TypeSetDescriptionActionBuilder.of()
                .description(newDifferent.getDescription())
                .build());
  }

  @Test
  void buildSetDescriptionAction_WithSameValues_ShouldReturnEmptyOptional() {
    final Optional<TypeUpdateAction> result = buildSetDescriptionUpdateAction(old, newSame);

    assertThat(result).isEmpty();
  }
}
