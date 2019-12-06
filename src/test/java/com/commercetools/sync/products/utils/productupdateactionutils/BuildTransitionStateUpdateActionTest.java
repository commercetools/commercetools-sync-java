package com.commercetools.sync.products.utils.productupdateactionutils;

import io.sphere.sdk.models.Reference;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.ProductDraft;
import io.sphere.sdk.products.commands.updateactions.TransitionState;
import io.sphere.sdk.states.State;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.commercetools.sync.products.utils.ProductUpdateActionUtils.buildTransitionStateUpdateAction;
import static io.sphere.sdk.json.SphereJsonUtils.readObject;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BuildTransitionStateUpdateActionTest {

    @Mock
    private Product oldProduct;

    @Mock
    private ProductDraft newProduct;

    @SuppressWarnings("unchecked")
    private static final Reference<State> oldState = readObject(
        "{\"typeId\": \"state\",\"id\": \"11111111-1111-1111-1111-111111111111\"}", Reference.class);

    @SuppressWarnings("unchecked")
    private static final Reference<State> newState = readObject(
        "{\"typeId\": \"state\",\"id\": \"11111111-1111-1111-1111-111111111111\"}", Reference.class);

    @SuppressWarnings("unchecked")
    private static final Reference<State> newChangedState = readObject(
        "{\"typeId\": \"state\",\"id\": \"22222222-2222-2222-2222-222222222222\"}", Reference.class);

    @Test
    void buildTransitionStateUpdateAction_withEmptyOld() {
        assertThat(buildTransitionStateUpdateAction(oldProduct, newProduct)).isEmpty();

        when(newProduct.getState()).thenReturn(newState);
        assertThat(buildTransitionStateUpdateAction(oldProduct, newProduct))
            .contains(TransitionState.of(newState, true));
    }

    @Test
    void buildTransitionStateUpdateAction_withEmptyNewShouldReturnEmpty() {
        assertThat(buildTransitionStateUpdateAction(oldProduct, newProduct)).isEmpty();
    }

    @Test
    void buildTransitionStateUpdateAction_withEqual() {
        when(oldProduct.getState()).thenReturn(oldState);
        when(newProduct.getState()).thenReturn(newState);
        assertThat(buildTransitionStateUpdateAction(oldProduct, newProduct)).isEmpty();
    }

    @Test
    void buildTransitionStateUpdateAction_withEmptyOldShouldReturnNew() {
        when(newProduct.getState()).thenReturn(newChangedState);
        assertThat(buildTransitionStateUpdateAction(oldProduct, newProduct))
            .contains(TransitionState.of(newChangedState, true));
    }


    @Test
    void buildTransitionStateUpdateAction_withDifferent() {
        when(oldProduct.getState()).thenReturn(oldState);
        when(newProduct.getState()).thenReturn(newChangedState);
        assertThat(buildTransitionStateUpdateAction(oldProduct, newProduct))
            .contains(TransitionState.of(newChangedState, true));
    }

}