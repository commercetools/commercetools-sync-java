package com.commercetools.sync.types.utils;

import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.models.LocalizedString;
import io.sphere.sdk.types.FieldDefinition;
import io.sphere.sdk.types.ResourceTypeIdsSetBuilder;
import io.sphere.sdk.types.Type;
import io.sphere.sdk.types.TypeDraft;
import io.sphere.sdk.types.TypeDraftBuilder;
import io.sphere.sdk.types.commands.updateactions.ChangeName;
import io.sphere.sdk.types.commands.updateactions.SetDescription;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static com.commercetools.sync.types.FieldDefinitionTestHelper.imageUrlFieldDefinition;
import static com.commercetools.sync.types.FieldDefinitionTestHelper.relatedCategoriesFieldDefinition;
import static com.commercetools.sync.types.FieldDefinitionTestHelper.stateFieldDefinition;
import static com.commercetools.sync.types.utils.TypeUpdateActionUtils.buildChangeNameUpdateAction;
import static com.commercetools.sync.types.utils.TypeUpdateActionUtils.buildSetDescriptionUpdateAction;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TypeUpdateActionUtilsTest {
    private static Type old;
    private static TypeDraft newSame;
    private static TypeDraft newDifferent;

    /**
     * Initialises test data.
     */
    @BeforeClass
    public static void setup() {
        final String key = "category-custom-type-key";
        final LocalizedString name = LocalizedString.ofEnglish("type for standard categories");
        final LocalizedString desc = LocalizedString.ofEnglish("description for category custom type");

        //this enables the type only to be used for categories
        final Set<String> resourceTypeIds = ResourceTypeIdsSetBuilder.of()
                .addCategories()
                //there are more methods for other objects which support custom
                .build();

        final List<FieldDefinition> fieldDefinitions =
                Arrays.asList(stateFieldDefinition(),
                        imageUrlFieldDefinition(),
                        relatedCategoriesFieldDefinition());


        old = mock(Type.class);
        when(old.getKey()).thenReturn(key);
        when(old.getName()).thenReturn(name);
        when(old.getDescription()).thenReturn(desc);
        when(old.getFieldDefinitions()).thenReturn(fieldDefinitions);

        newSame = TypeDraftBuilder.of(key, name, resourceTypeIds)
                .description(desc)
                .fieldDefinitions(fieldDefinitions)
                .build();


        newDifferent = TypeDraftBuilder.of("category-custom-type-key-2",
                LocalizedString.ofEnglish("type for standard categories 2"),
                resourceTypeIds)
                .description(LocalizedString.ofEnglish("description for category custom type 2"))
                .fieldDefinitions(fieldDefinitions)
                .build();
    }

    @Test
    public void buildChangeNameAction_WithDifferentValues_ShouldReturnAction() {
        final Optional<UpdateAction<Type>> result = buildChangeNameUpdateAction(old, newDifferent);

        assertThat(result).containsInstanceOf(ChangeName.class);
        assertThat(result).contains(ChangeName.of(newDifferent.getName()));
    }

    @Test
    public void buildChangeNameAction_WithSameValues_ShouldReturnEmptyOptional() {
        final Optional<UpdateAction<Type>> result = buildChangeNameUpdateAction(old, newSame);

        assertThat(result).isEmpty();
    }

    @Test
    public void buildSetDescriptionAction_WithDifferentValues_ShouldReturnAction() {
        final Optional<UpdateAction<Type>> result = buildSetDescriptionUpdateAction(old, newDifferent);

        assertThat(result).containsInstanceOf(SetDescription.class);
        assertThat(result).contains(SetDescription.of(newDifferent.getDescription()));
    }

    @Test
    public void buildSetDescriptionAction_WithSameValues_ShouldReturnEmptyOptional() {
        final Optional<UpdateAction<Type>> result = buildSetDescriptionUpdateAction(old, newSame);

        assertThat(result).isEmpty();
    }
}
