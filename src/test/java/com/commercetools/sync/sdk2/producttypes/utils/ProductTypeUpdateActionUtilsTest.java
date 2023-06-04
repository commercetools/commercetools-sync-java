package com.commercetools.sync.sdk2.producttypes.utils;

import static com.commercetools.sync.sdk2.producttypes.TestBuilderUtils.createDefaultAttributeDefinitionBuilder;
import static com.commercetools.sync.sdk2.producttypes.helpers.ResourceToDraftConverters.toAttributeDefinitionDraftBuilder;
import static com.commercetools.sync.sdk2.producttypes.utils.ProductTypeUpdateActionUtils.buildChangeDescriptionAction;
import static com.commercetools.sync.sdk2.producttypes.utils.ProductTypeUpdateActionUtils.buildChangeNameAction;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.commercetools.api.models.common.LocalizedString;
import com.commercetools.api.models.product_type.AttributeDefinition;
import com.commercetools.api.models.product_type.AttributeDefinitionDraft;
import com.commercetools.api.models.product_type.AttributeTypeBuilder;
import com.commercetools.api.models.product_type.ProductType;
import com.commercetools.api.models.product_type.ProductTypeChangeDescriptionAction;
import com.commercetools.api.models.product_type.ProductTypeChangeDescriptionActionBuilder;
import com.commercetools.api.models.product_type.ProductTypeChangeNameAction;
import com.commercetools.api.models.product_type.ProductTypeChangeNameActionBuilder;
import com.commercetools.api.models.product_type.ProductTypeDraft;
import com.commercetools.api.models.product_type.ProductTypeDraftBuilder;
import com.commercetools.api.models.product_type.ProductTypeUpdateAction;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ProductTypeUpdateActionUtilsTest {
  private static ProductType old;
  private static ProductTypeDraft newSame;
  private static ProductTypeDraft newDifferent;

  /** Initialises test data. */
  @BeforeAll
  static void setup() {
    final LocalizedString label = LocalizedString.of(Locale.ENGLISH, "label1");
    final AttributeDefinition attributeDefinition =
        createDefaultAttributeDefinitionBuilder()
            .name("attributeName1")
            .label(label)
            .type(AttributeTypeBuilder::textBuilder)
            .build();

    final List<AttributeDefinition> oldAttributeDefinitions = singletonList(attributeDefinition);

    final List<AttributeDefinitionDraft> sameAttributeDefinitionDrafts =
        singletonList(toAttributeDefinitionDraftBuilder(attributeDefinition).build());

    old = mock(ProductType.class);
    when(old.getKey()).thenReturn("key1");
    when(old.getName()).thenReturn("name1");
    when(old.getDescription()).thenReturn("description 1");
    when(old.getAttributes()).thenReturn(oldAttributeDefinitions);

    newSame =
        ProductTypeDraftBuilder.of()
            .key("key1")
            .name("name1")
            .description("description 1")
            .attributes(sameAttributeDefinitionDrafts)
            .build();

    newDifferent =
        ProductTypeDraftBuilder.of()
            .key("key2")
            .name("name2")
            .description("description 2")
            .attributes(sameAttributeDefinitionDrafts)
            .build();
  }

  @Test
  void buildChangeNameAction_WithDifferentValues_ShouldReturnAction() {
    final Optional<ProductTypeUpdateAction> result = buildChangeNameAction(old, newDifferent);

    assertThat(result).containsInstanceOf(ProductTypeChangeNameAction.class);
    assertThat(result)
        .contains(ProductTypeChangeNameActionBuilder.of().name(newDifferent.getName()).build());
  }

  @Test
  void buildChangeNameAction_WithSameValues_ShouldReturnEmptyOptional() {
    final Optional<ProductTypeUpdateAction> result = buildChangeNameAction(old, newSame);

    assertThat(result).isEmpty();
  }

  @Test
  void buildChangeDescriptionAction_WithDifferentValues_ShouldReturnAction() {
    final Optional<ProductTypeUpdateAction> result =
        buildChangeDescriptionAction(old, newDifferent);

    assertThat(result).containsInstanceOf(ProductTypeChangeDescriptionAction.class);
    assertThat(result)
        .contains(
            ProductTypeChangeDescriptionActionBuilder.of()
                .description(newDifferent.getDescription())
                .build());
  }

  @Test
  void buildChangeDescriptionAction_WithSameValues_ShouldReturnEmptyOptional() {
    final Optional<ProductTypeUpdateAction> result = buildChangeDescriptionAction(old, newSame);

    assertThat(result).isEmpty();
  }
}
