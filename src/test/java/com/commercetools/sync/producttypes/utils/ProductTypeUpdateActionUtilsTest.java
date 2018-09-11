package com.commercetools.sync.producttypes.utils;

import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.models.LocalizedString;
import io.sphere.sdk.products.attributes.AttributeDefinition;
import io.sphere.sdk.products.attributes.AttributeDefinitionBuilder;
import io.sphere.sdk.products.attributes.AttributeDefinitionDraft;
import io.sphere.sdk.products.attributes.AttributeDefinitionDraftBuilder;
import io.sphere.sdk.products.attributes.StringAttributeType;
import io.sphere.sdk.producttypes.ProductType;

import static com.commercetools.sync.producttypes.utils.ProductTypeUpdateActionUtils.buildChangeDescriptionAction;
import static com.commercetools.sync.producttypes.utils.ProductTypeUpdateActionUtils.buildChangeNameAction;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.sphere.sdk.producttypes.ProductTypeDraft;
import io.sphere.sdk.producttypes.commands.updateactions.ChangeDescription;
import io.sphere.sdk.producttypes.commands.updateactions.ChangeName;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class ProductTypeUpdateActionUtilsTest {
    private static ProductType old;
    private static ProductTypeDraft newSame;
    private static ProductTypeDraft newDifferent;

    /**
     * Initialises test data.
     */
    @BeforeClass
    public static void setup() {
        final LocalizedString label = LocalizedString.of(Locale.ENGLISH, "label1");
        final AttributeDefinition attributeDefinition = AttributeDefinitionBuilder
                .of("attributeName1", label, StringAttributeType.of())
                .build();

        final List<AttributeDefinition> oldAttributeDefinitions = singletonList(attributeDefinition);

        final List<AttributeDefinitionDraft> sameAttributeDefinitionDrafts = singletonList(
                AttributeDefinitionDraftBuilder.of(attributeDefinition).build());

        old = mock(ProductType.class);
        when(old.getKey()).thenReturn("key1");
        when(old.getName()).thenReturn("name1");
        when(old.getDescription()).thenReturn("description 1");
        when(old.getAttributes()).thenReturn(oldAttributeDefinitions);

        newSame = ProductTypeDraft.ofAttributeDefinitionDrafts(
                "key1",
                "name1",
                "description 1",
                sameAttributeDefinitionDrafts
        );

        newDifferent = ProductTypeDraft.ofAttributeDefinitionDrafts(
                "key2",
                "name2",
                "description 2",
                sameAttributeDefinitionDrafts
        );
    }

    @Test
    public void buildChangeNameAction_WithDifferentValues_ShouldReturnAction() {
        final Optional<UpdateAction<ProductType>> result = buildChangeNameAction(old, newDifferent);

        assertThat(result).containsInstanceOf(ChangeName.class);
        assertThat(result).contains(ChangeName.of(newDifferent.getName()));
    }

    @Test
    public void buildChangeNameAction_WithSameValues_ShouldReturnEmptyOptional() {
        final Optional<UpdateAction<ProductType>> result = buildChangeNameAction(old, newSame);

        assertThat(result).isEmpty();
    }

    @Test
    public void buildChangeDescriptionAction_WithDifferentValues_ShouldReturnAction() {
        final Optional<UpdateAction<ProductType>> result = buildChangeDescriptionAction(old, newDifferent);

        assertThat(result).containsInstanceOf(ChangeDescription.class);
        assertThat(result).contains(ChangeDescription.of(newDifferent.getDescription()));
    }

    @Test
    public void buildChangeDescriptionAction_WithSameValues_ShouldReturnEmptyOptional() {
        final Optional<UpdateAction<ProductType>> result = buildChangeDescriptionAction(old, newSame);

        assertThat(result).isEmpty();
    }
}
