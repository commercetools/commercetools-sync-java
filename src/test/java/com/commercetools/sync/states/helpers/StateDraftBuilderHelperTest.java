package com.commercetools.sync.states.helpers;

import io.sphere.sdk.json.SphereJsonUtils;
import io.sphere.sdk.states.State;
import io.sphere.sdk.states.StateDraft;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

class StateDraftBuilderHelperTest {

    @Test
    void of_WithState_ShouldBuildStateDraft() {
        final State resource = SphereJsonUtils.readObjectFromResource("state.json", State.class);

        StateDraft draft = StateDraftBuilderHelper.of(resource);

        assertAll(
            () -> assertThat(draft.getType()).isEqualTo(resource.getType()),
            () -> assertThat(draft.getKey()).isEqualTo(resource.getKey()),
            () -> assertThat(draft.isInitial()).isEqualTo(resource.isInitial()),
            () -> assertThat(draft.getName()).isEqualTo(resource.getName()),
            () -> assertThat(draft.getDescription()).isEqualTo(resource.getDescription()),
            () -> assertThat(draft.getRoles()).isEqualTo(resource.getRoles()),
            () -> assertThat(draft.getTransitions()).isEqualTo(resource.getTransitions())
        );
    }

}
