package com.commercetools.sync.shoppinglists.utils;

import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.models.LocalizedString;
import io.sphere.sdk.shoppinglists.ShoppingList;
import io.sphere.sdk.shoppinglists.ShoppingListDraft;
import io.sphere.sdk.shoppinglists.commands.updateactions.ChangeName;
import io.sphere.sdk.shoppinglists.commands.updateactions.SetDescription;
import io.sphere.sdk.shoppinglists.commands.updateactions.SetSlug;
import org.junit.jupiter.api.Test;

import java.util.Locale;
import java.util.Optional;

import static com.commercetools.sync.shoppinglists.utils.ShoppingListUpdateActionUtils.buildChangeNameUpdateAction;
import static com.commercetools.sync.shoppinglists.utils.ShoppingListUpdateActionUtils.buildSetDescriptionUpdateAction;
import static com.commercetools.sync.shoppinglists.utils.ShoppingListUpdateActionUtils.buildSetSlugUpdateAction;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ShoppingListUpdateActionUtilsTest {
    private static final Locale LOCALE = Locale.GERMAN;

    @Test
    void buildSetSlugUpdateAction_WithDifferentValues_ShouldBuildUpdateAction() {
        final ShoppingList oldShoppingList = mock(ShoppingList.class);
        when(oldShoppingList.getSlug()).thenReturn(LocalizedString.of(LOCALE, "oldSlug"));

        final ShoppingListDraft newShoppingList = mock(ShoppingListDraft.class);
        when(newShoppingList.getSlug()).thenReturn(LocalizedString.of(LOCALE, "newSlug"));

        final UpdateAction<ShoppingList> setSlugUpdateAction =
            buildSetSlugUpdateAction(oldShoppingList, newShoppingList).orElse(null);

        assertThat(setSlugUpdateAction).isNotNull();
        assertThat(setSlugUpdateAction.getAction()).isEqualTo("setSlug");
        assertThat(((SetSlug) setSlugUpdateAction).getSlug())
            .isEqualTo(LocalizedString.of(LOCALE, "newSlug"));
    }

    @Test
    void buildSetSlugUpdateAction_WithNullValues_ShouldBuildUpdateAction() {
        final ShoppingList oldShoppingList = mock(ShoppingList.class);
        when(oldShoppingList.getSlug()).thenReturn(LocalizedString.of(LOCALE, "oldSlug"));

        final ShoppingListDraft newShoppingList = mock(ShoppingListDraft.class);
        when(newShoppingList.getSlug()).thenReturn(null);

        final UpdateAction<ShoppingList> setSlugUpdateAction =
            buildSetSlugUpdateAction(oldShoppingList, newShoppingList).orElse(null);

        assertThat(setSlugUpdateAction).isNotNull();
        assertThat(setSlugUpdateAction.getAction()).isEqualTo("setSlug");
        assertThat(((SetSlug) setSlugUpdateAction).getSlug()).isNull();
    }

    @Test
    void buildSetSlugUpdateAction_WithSameValues_ShouldNotBuildUpdateAction() {
        final ShoppingList oldShoppingList = mock(ShoppingList.class);
        when(oldShoppingList.getSlug()).thenReturn(LocalizedString.of(LOCALE, "oldSlug"));

        final ShoppingListDraft newShoppingList = mock(ShoppingListDraft.class);
        when(newShoppingList.getSlug()).thenReturn(LocalizedString.of(LOCALE, "oldSlug"));

        final Optional<UpdateAction<ShoppingList>> setSlugUpdateAction =
            buildSetSlugUpdateAction(oldShoppingList, newShoppingList);

        assertThat(setSlugUpdateAction).isNotNull();
        assertThat(setSlugUpdateAction).isNotPresent();
    }

    @Test
    void buildChangeNameUpdateAction_WithDifferentValues_ShouldBuildUpdateAction() {
        final ShoppingList oldShoppingList = mock(ShoppingList.class);
        when(oldShoppingList.getName()).thenReturn(LocalizedString.of(LOCALE, "oldName"));

        final ShoppingListDraft newShoppingList = mock(ShoppingListDraft.class);
        when(newShoppingList.getName()).thenReturn(LocalizedString.of(LOCALE, "newName"));

        final UpdateAction<ShoppingList> changeNameUpdateAction =
            buildChangeNameUpdateAction(oldShoppingList, newShoppingList).orElse(null);

        assertThat(changeNameUpdateAction).isNotNull();
        assertThat(changeNameUpdateAction.getAction()).isEqualTo("changeName");
        assertThat(((ChangeName) changeNameUpdateAction).getName()).isEqualTo(LocalizedString.of(LOCALE, "newName"));
    }

    @Test
    void buildChangeNameUpdateAction_WithNullValues_ShouldBuildUpdateAction() {
        final ShoppingList oldShoppingList = mock(ShoppingList.class);
        when(oldShoppingList.getName()).thenReturn(LocalizedString.of(LOCALE, "oldName"));

        final ShoppingListDraft newShoppingList = mock(ShoppingListDraft.class);
        when(newShoppingList.getName()).thenReturn(null);

        final UpdateAction<ShoppingList> changeNameUpdateAction =
            buildChangeNameUpdateAction(oldShoppingList, newShoppingList).orElse(null);

        assertThat(changeNameUpdateAction).isNotNull();
        assertThat(changeNameUpdateAction.getAction()).isEqualTo("changeName");
        assertThat(((ChangeName) changeNameUpdateAction).getName()).isNull();
    }

    @Test
    void buildChangeNameUpdateAction_WithSameValues_ShouldNotBuildUpdateAction() {
        final ShoppingList oldShoppingList = mock(ShoppingList.class);
        when(oldShoppingList.getName()).thenReturn(LocalizedString.of(LOCALE, "oldName"));

        final ShoppingListDraft newShoppingListWithSameName = mock(ShoppingListDraft.class);
        when(newShoppingListWithSameName.getName())
            .thenReturn(LocalizedString.of(LOCALE, "oldName"));

        final Optional<UpdateAction<ShoppingList>> changeNameUpdateAction =
            buildChangeNameUpdateAction(oldShoppingList, newShoppingListWithSameName);

        assertThat(changeNameUpdateAction).isNotNull();
        assertThat(changeNameUpdateAction).isNotPresent();
    }

    @Test
    void buildSetDescriptionUpdateAction_WithDifferentValues_ShouldBuildUpdateAction() {
        final ShoppingList oldShoppingList = mock(ShoppingList.class);
        when(oldShoppingList.getDescription()).thenReturn(LocalizedString.of(LOCALE, "oldDescription"));

        final ShoppingListDraft newShoppingList = mock(ShoppingListDraft.class);
        when(newShoppingList.getDescription()).thenReturn(LocalizedString.of(LOCALE, "newDescription"));

        final UpdateAction<ShoppingList> setDescriptionUpdateAction =
            buildSetDescriptionUpdateAction(oldShoppingList, newShoppingList).orElse(null);

        assertThat(setDescriptionUpdateAction).isNotNull();
        assertThat(setDescriptionUpdateAction.getAction()).isEqualTo("setDescription");
        assertThat(((SetDescription) setDescriptionUpdateAction).getDescription())
            .isEqualTo(LocalizedString.of(LOCALE, "newDescription"));
    }

    @Test
    void buildSetDescriptionUpdateAction_WithNullValues_ShouldBuildUpdateAction() {
        final ShoppingList oldShoppingList = mock(ShoppingList.class);
        when(oldShoppingList.getDescription()).thenReturn(LocalizedString.of(LOCALE, "oldDescription"));

        final ShoppingListDraft newShoppingList = mock(ShoppingListDraft.class);
        when(newShoppingList.getDescription()).thenReturn(null);

        final UpdateAction<ShoppingList> setDescriptionUpdateAction =
            buildSetDescriptionUpdateAction(oldShoppingList, newShoppingList).orElse(null);

        assertThat(setDescriptionUpdateAction).isNotNull();
        assertThat(setDescriptionUpdateAction.getAction()).isEqualTo("setDescription");
        assertThat(((SetDescription) setDescriptionUpdateAction).getDescription()).isEqualTo(null);
    }

    @Test
    void buildSetDescriptionUpdateAction_WithSameValues_ShouldNotBuildUpdateAction() {
        final ShoppingList oldShoppingList = mock(ShoppingList.class);
        when(oldShoppingList.getDescription()).thenReturn(LocalizedString.of(LOCALE, "oldDescription"));

        final ShoppingListDraft newShoppingList = mock(ShoppingListDraft.class);
        when(newShoppingList.getDescription())
            .thenReturn(LocalizedString.of(LOCALE, "oldDescription"));

        final Optional<UpdateAction<ShoppingList>> setDescriptionUpdateAction =
            buildSetDescriptionUpdateAction(oldShoppingList, newShoppingList);

        assertThat(setDescriptionUpdateAction).isNotNull();
        assertThat(setDescriptionUpdateAction).isNotPresent();
    }

}
