package com.commercetools.sync.sdk2.shoppinglists.utils;

import static com.commercetools.sync.sdk2.shoppinglists.utils.ShoppingListUpdateActionUtils.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.commercetools.api.models.common.LocalizedString;
import com.commercetools.api.models.customer.CustomerReference;
import com.commercetools.api.models.customer.CustomerReferenceBuilder;
import com.commercetools.api.models.customer.CustomerResourceIdentifier;
import com.commercetools.api.models.customer.CustomerResourceIdentifierBuilder;
import com.commercetools.api.models.shopping_list.ShoppingList;
import com.commercetools.api.models.shopping_list.ShoppingListChangeNameAction;
import com.commercetools.api.models.shopping_list.ShoppingListDraft;
import com.commercetools.api.models.shopping_list.ShoppingListSetAnonymousIdAction;
import com.commercetools.api.models.shopping_list.ShoppingListSetCustomerAction;
import com.commercetools.api.models.shopping_list.ShoppingListSetDeleteDaysAfterLastModificationAction;
import com.commercetools.api.models.shopping_list.ShoppingListSetDescriptionAction;
import com.commercetools.api.models.shopping_list.ShoppingListSetSlugAction;
import com.commercetools.api.models.shopping_list.ShoppingListUpdateAction;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ShoppingListUpdateActionUtilsTest {
  private static final Locale LOCALE = Locale.GERMAN;

  @Test
  void buildSetSlugUpdateAction_WithDifferentValues_ShouldBuildUpdateAction() {
    final ShoppingList oldShoppingList = mock(ShoppingList.class);
    when(oldShoppingList.getSlug()).thenReturn(LocalizedString.of(LOCALE, "oldSlug"));

    final ShoppingListDraft newShoppingList = mock(ShoppingListDraft.class);
    when(newShoppingList.getSlug()).thenReturn(LocalizedString.of(LOCALE, "newSlug"));

    final ShoppingListUpdateAction setSlugUpdateAction =
        buildSetSlugUpdateAction(oldShoppingList, newShoppingList).orElse(null);

    assertThat(setSlugUpdateAction).isNotNull();
    assertThat(setSlugUpdateAction.getAction()).isEqualTo("setSlug");
    assertThat(((ShoppingListSetSlugAction) setSlugUpdateAction).getSlug())
        .isEqualTo(LocalizedString.of(LOCALE, "newSlug"));
  }

  @Test
  void buildSetSlugUpdateAction_WithNullValues_ShouldBuildUpdateAction() {
    final ShoppingList oldShoppingList = mock(ShoppingList.class);
    when(oldShoppingList.getSlug()).thenReturn(LocalizedString.of(LOCALE, "oldSlug"));

    final ShoppingListDraft newShoppingList = mock(ShoppingListDraft.class);
    when(newShoppingList.getSlug()).thenReturn(null);

    final ShoppingListUpdateAction setSlugUpdateAction =
        buildSetSlugUpdateAction(oldShoppingList, newShoppingList).orElse(null);

    assertThat(setSlugUpdateAction).isNotNull();
    assertThat(setSlugUpdateAction.getAction()).isEqualTo("setSlug");
    assertThat(((ShoppingListSetSlugAction) setSlugUpdateAction).getSlug()).isNull();
  }

  @Test
  void buildSetSlugUpdateAction_WithSameValues_ShouldNotBuildUpdateAction() {
    final ShoppingList oldShoppingList = mock(ShoppingList.class);
    when(oldShoppingList.getSlug()).thenReturn(LocalizedString.of(LOCALE, "oldSlug"));

    final ShoppingListDraft newShoppingList = mock(ShoppingListDraft.class);
    when(newShoppingList.getSlug()).thenReturn(LocalizedString.of(LOCALE, "oldSlug"));

    final Optional<ShoppingListUpdateAction> setSlugUpdateAction =
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

    final ShoppingListUpdateAction changeNameUpdateAction =
        buildChangeNameUpdateAction(oldShoppingList, newShoppingList).orElse(null);

    assertThat(changeNameUpdateAction).isNotNull();
    assertThat(changeNameUpdateAction.getAction()).isEqualTo("changeName");
    assertThat(((ShoppingListChangeNameAction) changeNameUpdateAction).getName())
        .isEqualTo(LocalizedString.of(LOCALE, "newName"));
  }

  @Test
  void buildChangeNameUpdateAction_WithEmptyValues_ShouldBuildUpdateAction() {
    final ShoppingList oldShoppingList = mock(ShoppingList.class);
    when(oldShoppingList.getName()).thenReturn(LocalizedString.of(LOCALE, "oldName"));

    final ShoppingListDraft newShoppingList = mock(ShoppingListDraft.class);
    when(newShoppingList.getName()).thenReturn(LocalizedString.ofEnglish(""));

    final ShoppingListUpdateAction changeNameUpdateAction =
        buildChangeNameUpdateAction(oldShoppingList, newShoppingList).orElse(null);

    assertThat(changeNameUpdateAction).isNotNull();
    assertThat(changeNameUpdateAction.getAction()).isEqualTo("changeName");
    assertThat(((ShoppingListChangeNameAction) changeNameUpdateAction).getName())
        .isEqualTo(LocalizedString.ofEnglish(""));
  }

  @Test
  void buildChangeNameUpdateAction_WithSameValues_ShouldNotBuildUpdateAction() {
    final ShoppingList oldShoppingList = mock(ShoppingList.class);
    when(oldShoppingList.getName()).thenReturn(LocalizedString.of(LOCALE, "oldName"));

    final ShoppingListDraft newShoppingListWithSameName = mock(ShoppingListDraft.class);
    when(newShoppingListWithSameName.getName()).thenReturn(LocalizedString.of(LOCALE, "oldName"));

    final Optional<ShoppingListUpdateAction> changeNameUpdateAction =
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

    final ShoppingListUpdateAction setDescriptionUpdateAction =
        buildSetDescriptionUpdateAction(oldShoppingList, newShoppingList).orElse(null);

    assertThat(setDescriptionUpdateAction).isNotNull();
    assertThat(setDescriptionUpdateAction.getAction()).isEqualTo("setDescription");
    assertThat(((ShoppingListSetDescriptionAction) setDescriptionUpdateAction).getDescription())
        .isEqualTo(LocalizedString.of(LOCALE, "newDescription"));
  }

  @Test
  void buildSetDescriptionUpdateAction_WithNullValues_ShouldBuildUpdateAction() {
    final ShoppingList oldShoppingList = mock(ShoppingList.class);
    when(oldShoppingList.getDescription()).thenReturn(LocalizedString.of(LOCALE, "oldDescription"));

    final ShoppingListDraft newShoppingList = mock(ShoppingListDraft.class);
    when(newShoppingList.getDescription()).thenReturn(null);

    final ShoppingListUpdateAction setDescriptionUpdateAction =
        buildSetDescriptionUpdateAction(oldShoppingList, newShoppingList).orElse(null);

    assertThat(setDescriptionUpdateAction).isNotNull();
    assertThat(setDescriptionUpdateAction.getAction()).isEqualTo("setDescription");
    assertThat(((ShoppingListSetDescriptionAction) setDescriptionUpdateAction).getDescription())
        .isEqualTo(null);
  }

  @Test
  void buildSetDescriptionUpdateAction_WithSameValues_ShouldNotBuildUpdateAction() {
    final ShoppingList oldShoppingList = mock(ShoppingList.class);
    when(oldShoppingList.getDescription()).thenReturn(LocalizedString.of(LOCALE, "oldDescription"));

    final ShoppingListDraft newShoppingList = mock(ShoppingListDraft.class);
    when(newShoppingList.getDescription()).thenReturn(LocalizedString.of(LOCALE, "oldDescription"));

    final Optional<ShoppingListUpdateAction> setDescriptionUpdateAction =
        buildSetDescriptionUpdateAction(oldShoppingList, newShoppingList);

    assertThat(setDescriptionUpdateAction).isNotNull();
    assertThat(setDescriptionUpdateAction).isNotPresent();
  }

  @Test
  void buildSetCustomerUpdateAction_WithDifferentReference_ShouldBuildUpdateAction() {
    final String customerId = UUID.randomUUID().toString();
    final CustomerReference customerReference =
        CustomerReferenceBuilder.of().id(customerId).build();
    final ShoppingList oldShoppingList = mock(ShoppingList.class);
    when(oldShoppingList.getCustomer()).thenReturn(customerReference);

    final String resolvedCustomerId = UUID.randomUUID().toString();
    final CustomerResourceIdentifier newCustomerReference =
        CustomerResourceIdentifierBuilder.of().id(resolvedCustomerId).build();
    final ShoppingListDraft newShoppingList = mock(ShoppingListDraft.class);
    when(newShoppingList.getCustomer()).thenReturn(newCustomerReference);

    final Optional<ShoppingListUpdateAction> setCustomerUpdateAction =
        buildSetCustomerUpdateAction(oldShoppingList, newShoppingList);

    assertThat(setCustomerUpdateAction).isNotEmpty();
    assertThat(setCustomerUpdateAction).containsInstanceOf(ShoppingListSetCustomerAction.class);
    assertThat(((ShoppingListSetCustomerAction) setCustomerUpdateAction.get()).getCustomer())
        .isEqualTo(CustomerResourceIdentifierBuilder.of().id(resolvedCustomerId).build());
  }

  @Test
  void buildSetCustomerUpdateAction_WithSameReference_ShouldNotBuildUpdateAction() {
    final String customerId = UUID.randomUUID().toString();
    final CustomerReference customerReference =
        CustomerReferenceBuilder.of().id(customerId).build();
    final ShoppingList oldShoppingList = mock(ShoppingList.class);
    when(oldShoppingList.getCustomer()).thenReturn(customerReference);

    final ShoppingListDraft newShoppingList = mock(ShoppingListDraft.class);
    when(newShoppingList.getCustomer())
        .thenReturn(CustomerResourceIdentifierBuilder.of().id(customerId).build());

    final Optional<ShoppingListUpdateAction> setCustomerUpdateAction =
        buildSetCustomerUpdateAction(oldShoppingList, newShoppingList);

    assertThat(setCustomerUpdateAction).isEmpty();
  }

  @Test
  void buildSetCustomerUpdateAction_WithOnlyNewReference_ShouldBuildUpdateAction() {
    final ShoppingList oldShoppingList = mock(ShoppingList.class);

    final String newCustomerId = UUID.randomUUID().toString();
    final ShoppingListDraft newShoppingList = mock(ShoppingListDraft.class);
    when(newShoppingList.getCustomer())
        .thenReturn(CustomerResourceIdentifierBuilder.of().id(newCustomerId).build());

    final Optional<ShoppingListUpdateAction> setCustomerUpdateAction =
        buildSetCustomerUpdateAction(oldShoppingList, newShoppingList);

    assertThat(setCustomerUpdateAction).isNotEmpty();
    assertThat(setCustomerUpdateAction).containsInstanceOf(ShoppingListSetCustomerAction.class);
    assertThat(((ShoppingListSetCustomerAction) setCustomerUpdateAction.get()).getCustomer())
        .isEqualTo(CustomerResourceIdentifierBuilder.of().id(newCustomerId).build());
  }

  @Test
  void buildSetCustomerUpdateAction_WithoutNewReference_ShouldReturnUnsetAction() {
    final String customerId = UUID.randomUUID().toString();
    final CustomerReference customerReference =
        CustomerReferenceBuilder.of().id(customerId).build();
    final ShoppingList oldShoppingList = mock(ShoppingList.class);
    when(oldShoppingList.getCustomer()).thenReturn(customerReference);

    final ShoppingListDraft newShoppingList = mock(ShoppingListDraft.class);
    when(newShoppingList.getCustomer()).thenReturn(null);

    final Optional<ShoppingListUpdateAction> setCustomerUpdateAction =
        buildSetCustomerUpdateAction(oldShoppingList, newShoppingList);

    assertThat(setCustomerUpdateAction).isNotEmpty();
    assertThat(setCustomerUpdateAction).containsInstanceOf(ShoppingListSetCustomerAction.class);
    // Note: If the old value is set, but the new one is empty - the command will unset the
    // customer.
    assertThat(((ShoppingListSetCustomerAction) setCustomerUpdateAction.get()).getCustomer())
        .isNull();
  }

  @Test
  void buildSetAnonymousId_WithDifferentValues_ShouldBuildUpdateAction() {
    final ShoppingList oldShoppingList = mock(ShoppingList.class);
    when(oldShoppingList.getAnonymousId()).thenReturn("123");

    final ShoppingListDraft newShoppingList = mock(ShoppingListDraft.class);
    when(newShoppingList.getAnonymousId()).thenReturn("567");

    final Optional<ShoppingListUpdateAction> setAnonymousIdUpdateAction =
        buildSetAnonymousIdUpdateAction(oldShoppingList, newShoppingList);

    assertThat(setAnonymousIdUpdateAction).isNotNull();
    assertThat(setAnonymousIdUpdateAction)
        .containsInstanceOf(ShoppingListSetAnonymousIdAction.class);
  }

  @Test
  void buildSetAnonymousId_WithNullValues_ShouldBuildUpdateActions() {
    final ShoppingList oldShoppingList = mock(ShoppingList.class);
    when(oldShoppingList.getAnonymousId()).thenReturn("123");

    final ShoppingListDraft newShoppingList = mock(ShoppingListDraft.class);
    when(newShoppingList.getAnonymousId()).thenReturn(null);

    final Optional<ShoppingListUpdateAction> setAnonymousIdUpdateAction =
        buildSetAnonymousIdUpdateAction(oldShoppingList, newShoppingList);

    assertThat(setAnonymousIdUpdateAction).isNotNull();
    assertThat(setAnonymousIdUpdateAction)
        .containsInstanceOf(ShoppingListSetAnonymousIdAction.class);
  }

  @Test
  void buildSetAnonymousId_WithSameValues_ShouldNotBuildUpdateActions() {
    final ShoppingList oldShoppingList = mock(ShoppingList.class);
    when(oldShoppingList.getAnonymousId()).thenReturn("123");

    final ShoppingListDraft newShoppingList = mock(ShoppingListDraft.class);
    when(newShoppingList.getAnonymousId()).thenReturn("123");

    final Optional<ShoppingListUpdateAction> setAnonymousIdUpdateAction =
        buildSetAnonymousIdUpdateAction(oldShoppingList, newShoppingList);

    assertThat(setAnonymousIdUpdateAction).isEmpty();
  }

  @Test
  void
      buildSetDeleteDaysAfterLastModificationUpdateAction_WithDifferentValues_ShouldBuildUpdateAction() {
    final ShoppingList oldShoppingList = mock(ShoppingList.class);
    when(oldShoppingList.getDeleteDaysAfterLastModification()).thenReturn(50L);

    final ShoppingListDraft newShoppingList = mock(ShoppingListDraft.class);
    when(newShoppingList.getDeleteDaysAfterLastModification()).thenReturn(70L);

    final Optional<ShoppingListUpdateAction> setDeleteDaysUpdateAction =
        buildSetDeleteDaysAfterLastModificationUpdateAction(oldShoppingList, newShoppingList);

    assertThat(setDeleteDaysUpdateAction).isNotNull();
    assertThat(setDeleteDaysUpdateAction)
        .containsInstanceOf(ShoppingListSetDeleteDaysAfterLastModificationAction.class);
  }

  @Test
  void
      buildSetDeleteDaysAfterLastModificationUpdateAction_WithNullValues_ShouldBuildUpdateAction() {
    final ShoppingList oldShoppingList = mock(ShoppingList.class);
    when(oldShoppingList.getDeleteDaysAfterLastModification()).thenReturn(50L);

    final ShoppingListDraft newShoppingList = mock(ShoppingListDraft.class);
    when(newShoppingList.getDeleteDaysAfterLastModification()).thenReturn(null);

    final Optional<ShoppingListUpdateAction> setDeleteDaysUpdateAction =
        buildSetDeleteDaysAfterLastModificationUpdateAction(oldShoppingList, newShoppingList);

    assertThat(setDeleteDaysUpdateAction).isNotNull();
    assertThat(setDeleteDaysUpdateAction)
        .containsInstanceOf(ShoppingListSetDeleteDaysAfterLastModificationAction.class);
  }
}
