package com.commercetools.sync.products.utils.productupdateactionutils;

import static io.vrap.rmf.base.client.utils.json.JsonUtils.fromJsonString;
import static org.mockito.Mockito.when;

import com.commercetools.api.models.product.ProductDraft;
import com.commercetools.api.models.product.ProductProjection;
import com.commercetools.api.models.product.ProductTransitionStateAction;
import com.commercetools.api.models.state.StateReference;
import com.commercetools.api.models.state.StateResourceIdentifier;
import com.commercetools.sync.products.utils.ProductUpdateActionUtils;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BuildTransitionStateUpdateActionTest {

  @Mock private ProductProjection oldProduct;

  @Mock private ProductDraft newProduct;

  @SuppressWarnings("unchecked")
  private static final StateReference oldState =
      fromJsonString(
          "{\"typeId\": \"state\",\"id\": \"11111111-1111-1111-1111-111111111111\"}",
          StateReference.class);

  @SuppressWarnings("unchecked")
  private static final StateReference newState =
      fromJsonString(
          "{\"typeId\": \"state\",\"id\": \"11111111-1111-1111-1111-111111111111\"}",
          StateReference.class);

  @SuppressWarnings("unchecked")
  private static final StateReference newChangedState =
      fromJsonString(
          "{\"typeId\": \"state\",\"id\": \"22222222-2222-2222-2222-222222222222\"}",
          StateReference.class);

  @Test
  void buildTransitionStateUpdateAction_withEmptyOld() {
    Assertions.assertThat(
            ProductUpdateActionUtils.buildTransitionStateUpdateAction(oldProduct, newProduct))
        .isEmpty();

    StateResourceIdentifier stateResourceIdentifier =
        StateResourceIdentifier.builder().id(newState.getId()).build();

    when(newProduct.getState()).thenReturn(stateResourceIdentifier);
    Assertions.assertThat(
            ProductUpdateActionUtils.buildTransitionStateUpdateAction(oldProduct, newProduct))
        .contains(ProductTransitionStateAction.builder().state(stateResourceIdentifier).build());
  }

  @Test
  void buildTransitionStateUpdateAction_withEmptyNewShouldReturnEmpty() {
    Assertions.assertThat(
            ProductUpdateActionUtils.buildTransitionStateUpdateAction(oldProduct, newProduct))
        .isEmpty();
  }

  @Test
  void buildTransitionStateUpdateAction_withEqual() {
    when(oldProduct.getState()).thenReturn(oldState);

    StateResourceIdentifier stateResourceIdentifier =
        StateResourceIdentifier.builder().id(newState.getId()).build();
    when(newProduct.getState()).thenReturn(stateResourceIdentifier);

    Assertions.assertThat(
            ProductUpdateActionUtils.buildTransitionStateUpdateAction(oldProduct, newProduct))
        .isEmpty();
  }

  @Test
  void buildTransitionStateUpdateAction_withEmptyOldShouldReturnNew() {

    StateResourceIdentifier changedStateResourceIdentifier =
        StateResourceIdentifier.builder().id(newChangedState.getId()).build();
    when(newProduct.getState()).thenReturn(changedStateResourceIdentifier);
    Assertions.assertThat(
            ProductUpdateActionUtils.buildTransitionStateUpdateAction(oldProduct, newProduct))
        .contains(
            ProductTransitionStateAction.builder().state(changedStateResourceIdentifier).build());
  }

  @Test
  void buildTransitionStateUpdateAction_withDifferent() {
    when(oldProduct.getState()).thenReturn(oldState);

    StateResourceIdentifier changedStateResourceIdentifier =
        StateResourceIdentifier.builder().id(newChangedState.getId()).build();
    when(newProduct.getState()).thenReturn(changedStateResourceIdentifier);
    Assertions.assertThat(
            ProductUpdateActionUtils.buildTransitionStateUpdateAction(oldProduct, newProduct))
        .contains(
            ProductTransitionStateAction.builder().state(changedStateResourceIdentifier).build());
  }
}
