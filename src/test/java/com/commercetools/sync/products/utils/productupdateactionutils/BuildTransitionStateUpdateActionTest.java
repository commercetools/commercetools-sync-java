package com.commercetools.sync.products.utils.productupdateactionutils;

import io.sphere.sdk.models.Reference;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.ProductDraft;
import io.sphere.sdk.products.commands.updateactions.TransitionState;
import io.sphere.sdk.states.State;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static com.commercetools.sync.products.utils.ProductUpdateActionUtils.buildTransitionStateUpdateAction;
import static io.sphere.sdk.json.SphereJsonUtils.readObject;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class BuildTransitionStateUpdateActionTest {

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
    public void buildTransitionStateUpdateAction_withEmptyOld() throws Exception {
        assertThat(buildTransitionStateUpdateAction(oldProduct, newProduct)).isEmpty();

        when(newProduct.getState()).thenReturn(newState);
        assertThat(buildTransitionStateUpdateAction(oldProduct, newProduct))
            .contains(TransitionState.of(newState, true));
    }

    @Test
    public void buildTransitionStateUpdateAction_withEmptyNewShouldReturnEmpty() throws Exception {
        assertThat(buildTransitionStateUpdateAction(oldProduct, newProduct)).isEmpty();
    }

    @Test
    public void buildTransitionStateUpdateAction_withEqual() throws Exception {
        when(oldProduct.getState()).thenReturn(oldState);
        when(newProduct.getState()).thenReturn(newState);
        assertThat(buildTransitionStateUpdateAction(oldProduct, newProduct)).isEmpty();
    }

    @Test
    public void buildTransitionStateUpdateAction_withEmptyOldShouldReturnNew() throws Exception {
        when(newProduct.getState()).thenReturn(newChangedState);
        assertThat(buildTransitionStateUpdateAction(oldProduct, newProduct))
            .contains(TransitionState.of(newChangedState, true));
    }


    @Test
    public void buildTransitionStateUpdateAction_withDifferent() throws Exception {
        when(oldProduct.getState()).thenReturn(oldState);
        when(newProduct.getState()).thenReturn(newChangedState);
        assertThat(buildTransitionStateUpdateAction(oldProduct, newProduct))
            .contains(TransitionState.of(newChangedState, true));
    }

}