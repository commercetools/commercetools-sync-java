package com.commercetools.sync.types.utils;

import static com.commercetools.sync.types.utils.TypeUpdateActionUtils.buildChangeNameUpdateAction;
import static com.commercetools.sync.types.utils.TypeUpdateActionUtils.buildSetDescriptionUpdateAction;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.models.LocalizedString;
import io.sphere.sdk.types.ResourceTypeIdsSetBuilder;
import io.sphere.sdk.types.Type;
import io.sphere.sdk.types.TypeDraft;
import io.sphere.sdk.types.TypeDraftBuilder;
import io.sphere.sdk.types.commands.updateactions.ChangeName;
import io.sphere.sdk.types.commands.updateactions.SetDescription;
import java.util.Optional;
import java.util.Set;
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

    final Set<String> resourceTypeIds = ResourceTypeIdsSetBuilder.of().addCategories().build();

    old = mock(Type.class);
    when(old.getKey()).thenReturn(key);
    when(old.getName()).thenReturn(name);
    when(old.getDescription()).thenReturn(desc);

    newSame = TypeDraftBuilder.of(key, name, resourceTypeIds).description(desc).build();

    newDifferent =
        TypeDraftBuilder.of(
                "category-custom-type-key-2",
                LocalizedString.ofEnglish("type for standard categories 2"),
                resourceTypeIds)
            .description(LocalizedString.ofEnglish("description for category custom type 2"))
            .build();
  }

  @Test
  void buildChangeNameAction_WithDifferentValues_ShouldReturnAction() {
    final Optional<UpdateAction<Type>> result = buildChangeNameUpdateAction(old, newDifferent);

    assertThat(result).contains(ChangeName.of(newDifferent.getName()));
  }

  @Test
  void buildChangeNameAction_WithSameValues_ShouldReturnEmptyOptional() {
    final Optional<UpdateAction<Type>> result = buildChangeNameUpdateAction(old, newSame);

    assertThat(result).isEmpty();
  }

  @Test
  void buildSetDescriptionAction_WithDifferentValues_ShouldReturnAction() {
    final Optional<UpdateAction<Type>> result = buildSetDescriptionUpdateAction(old, newDifferent);

    assertThat(result).contains(SetDescription.of(newDifferent.getDescription()));
  }

  @Test
  void buildSetDescriptionAction_WithSameValues_ShouldReturnEmptyOptional() {
    final Optional<UpdateAction<Type>> result = buildSetDescriptionUpdateAction(old, newSame);

    assertThat(result).isEmpty();
  }
}
