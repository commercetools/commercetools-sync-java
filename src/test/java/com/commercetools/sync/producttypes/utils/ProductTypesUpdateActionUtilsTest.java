package com.commercetools.sync.producttypes.utils;

import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.models.LocalizedString;
import io.sphere.sdk.products.attributes.AttributeDefinition;
import io.sphere.sdk.products.attributes.AttributeDefinitionBuilder;
import io.sphere.sdk.products.attributes.AttributeDefinitionDraft;
import io.sphere.sdk.products.attributes.AttributeDefinitionDraftBuilder;
import io.sphere.sdk.products.attributes.StringAttributeType;
import io.sphere.sdk.producttypes.ProductType;

import static com.commercetools.sync.producttypes.utils.ProductTypesUpdateActionUtils.buildChangeDescriptionAction;
import static com.commercetools.sync.producttypes.utils.ProductTypesUpdateActionUtils.buildSetKeyAction;
import static com.commercetools.sync.producttypes.utils.ProductTypesUpdateActionUtils.buildChangeNameAction;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.sphere.sdk.producttypes.ProductTypeDraft;
import io.sphere.sdk.producttypes.commands.updateactions.ChangeDescription;
import io.sphere.sdk.producttypes.commands.updateactions.ChangeName;
import io.sphere.sdk.producttypes.commands.updateactions.SetKey;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.BiFunction;

public class ProductTypesUpdateActionUtilsTest {
    private static ProductType old;
    private static ProductTypeDraft newSame;
    private static ProductTypeDraft newDifferent;
    private static ProductTypeDraft newWithNullValues;

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

        newWithNullValues = ProductTypeDraft.ofAttributeDefinitionDrafts(
                null,
                "name3",
                "description 3",
                sameAttributeDefinitionDrafts
        );
    }

    @Test
    public void buildSetKeyAction_WithDifferentValues_ShouldReturnAction() {
        final Optional<UpdateAction<ProductType>> result = buildSetKeyAction(old, newDifferent);
        assertThat(result).isNotNull();
        assertThat(result.isPresent()).isTrue();
        assertThat(result.get()).isExactlyInstanceOf(SetKey.class);
        assertThat(((SetKey) result.get()).getKey()).isEqualTo(newDifferent.getKey());
    }

    @Test
    public void buildSetKeyAction_WithSameValues_ShouldReturnEmptyOptional() {
        assertNoUpdatesForSameValues(ProductTypesUpdateActionUtils::buildSetKeyAction);
    }

    @Test
    public void buildChangeNameAction_WithDifferentValues_ShouldReturnAction() {
        final Optional<UpdateAction<ProductType>> result = buildChangeNameAction(old, newDifferent);
        assertThat(result).isNotNull();
        assertThat(result.isPresent()).isTrue();
        assertThat(result.get()).isExactlyInstanceOf(ChangeName.class);
        assertThat(((ChangeName) result.get()).getName()).isEqualTo(newDifferent.getName());
    }

    @Test
    public void buildChangeNameAction_WithSameValues_ShouldReturnEmptyOptional() {
        assertNoUpdatesForSameValues(ProductTypesUpdateActionUtils::buildChangeNameAction);
    }

    @Test
    public void buildChangeDescriptionAction_WithDifferentValues_ShouldReturnAction() {
        final Optional<UpdateAction<ProductType>> result = buildChangeDescriptionAction(old, newDifferent);
        assertThat(result).isNotNull();
        assertThat(result.isPresent()).isTrue();
        assertThat(result.get()).isExactlyInstanceOf(ChangeDescription.class);
        assertThat(((ChangeDescription) result.get()).getDescription()).isEqualTo(newDifferent.getDescription());
    }

    @Test
    public void buildChangeDescriptionAction_WithSameValues_ShouldReturnEmptyOptional() {
        assertNoUpdatesForSameValues(ProductTypesUpdateActionUtils::buildChangeDescriptionAction);
    }

    private void assertNoUpdatesForSameValues(final BiFunction<ProductType, ProductTypeDraft,
            Optional<UpdateAction<ProductType>>> buildFunction) {
        final Optional<UpdateAction<ProductType>> result = buildFunction.apply(old, newSame);
        assertThat(result).isNotNull();
        assertThat(result.isPresent()).isFalse();
    }
}
