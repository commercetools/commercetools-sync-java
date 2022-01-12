package com.commercetools.sync.customobjects;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.commercetools.sync.commons.utils.TriFunction;
import com.fasterxml.jackson.databind.JsonNode;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.customobjects.CustomObject;
import io.sphere.sdk.customobjects.CustomObjectDraft;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

@SuppressWarnings("unchecked")
class CustomObjectSyncOptionsTest {

  private static SphereClient CTP_CLIENT = mock(SphereClient.class);

  private interface MockTriFunction
      extends TriFunction<
          List<UpdateAction<CustomObject<JsonNode>>>,
          CustomObjectDraft<JsonNode>,
          CustomObject<JsonNode>,
          List<UpdateAction<CustomObject<JsonNode>>>> {}

  @Test
  void applyBeforeUpdateCallback_WithNullCallbackAndEmptyUpdateActions_ShouldReturnIdenticalList() {
    final CustomObjectSyncOptions customObjectSyncOptions =
        CustomObjectSyncOptionsBuilder.of(CTP_CLIENT).build();

    final List<UpdateAction<CustomObject<JsonNode>>> updateActions = emptyList();

    final List<UpdateAction<CustomObject<JsonNode>>> filteredList =
        customObjectSyncOptions.applyBeforeUpdateCallback(
            updateActions, mock(CustomObjectDraft.class), mock(CustomObject.class));

    assertThat(filteredList).isSameAs(updateActions);
  }

  @Test
  void
      applyBeforeUpdateCallback_WithNullReturnCallbackAndEmptyUpdateActions_ShouldReturnEmptyList() {
    final TriFunction<
            List<UpdateAction<CustomObject<JsonNode>>>,
            CustomObjectDraft<JsonNode>,
            CustomObject<JsonNode>,
            List<UpdateAction<CustomObject<JsonNode>>>>
        beforeUpdateCallback = (updateActions, newCustomObject, oldCustomObject) -> null;
    final CustomObjectSyncOptions customObjectSyncOptions =
        CustomObjectSyncOptionsBuilder.of(CTP_CLIENT)
            .beforeUpdateCallback(beforeUpdateCallback)
            .build();
    final List<UpdateAction<CustomObject<JsonNode>>> updateActions = emptyList();

    final List<UpdateAction<CustomObject<JsonNode>>> filteredList =
        customObjectSyncOptions.applyBeforeUpdateCallback(
            updateActions, mock(CustomObjectDraft.class), mock(CustomObject.class));

    assertAll(
        () -> assertThat(filteredList).isEqualTo(updateActions),
        () -> assertThat(filteredList).isEmpty());
  }

  @Test
  void applyBeforeUpdateCallback_WithEmptyUpdateActions_ShouldNotApplyBeforeUpdateCallback() {
    final CustomObjectSyncOptionsTest.MockTriFunction beforeUpdateCallback =
        mock(CustomObjectSyncOptionsTest.MockTriFunction.class);
    final CustomObjectSyncOptions customObjectSyncOptions =
        CustomObjectSyncOptionsBuilder.of(CTP_CLIENT)
            .beforeUpdateCallback(beforeUpdateCallback)
            .build();

    final List<UpdateAction<CustomObject<JsonNode>>> filteredList =
        customObjectSyncOptions.applyBeforeUpdateCallback(
            emptyList(), mock(CustomObjectDraft.class), mock(CustomObject.class));

    assertThat(filteredList).isEmpty();
    verify(beforeUpdateCallback, never()).apply(any(), any(), any());
  }

  @Test
  void applyBeforeCreateCallback_WithCallback_ShouldReturnFilteredDraft() {

    final Function<CustomObjectDraft<JsonNode>, CustomObjectDraft<JsonNode>> draftFunction =
        customObjectDraft ->
            CustomObjectDraft.ofUnversionedUpsert(
                customObjectDraft.getContainer() + "_filteredContainer",
                customObjectDraft.getKey() + "_filteredKey",
                customObjectDraft.getValue());
    final CustomObjectSyncOptions customObjectSyncOptions =
        CustomObjectSyncOptionsBuilder.of(CTP_CLIENT).beforeCreateCallback(draftFunction).build();
    final CustomObjectDraft<JsonNode> resourceDraft = mock(CustomObjectDraft.class);
    when(resourceDraft.getKey()).thenReturn("myKey");
    when(resourceDraft.getContainer()).thenReturn("myContainer");

    final Optional<CustomObjectDraft<JsonNode>> filteredDraft =
        customObjectSyncOptions.applyBeforeCreateCallback(resourceDraft);

    assertThat(filteredDraft)
        .hasValueSatisfying(
            customObjectDraft ->
                assertAll(
                    () -> assertThat(customObjectDraft.getKey()).isEqualTo("myKey_filteredKey"),
                    () ->
                        assertThat(customObjectDraft.getContainer())
                            .isEqualTo("myContainer_filteredContainer")));
  }

  @Test
  void applyBeforeCreateCallback_WithNullCallback_ShouldReturnIdenticalDraftInOptional() {
    final CustomObjectSyncOptions customObjectSyncOptions =
        CustomObjectSyncOptionsBuilder.of(CTP_CLIENT).build();
    final CustomObjectDraft<JsonNode> resourceDraft = mock(CustomObjectDraft.class);

    final Optional<CustomObjectDraft<JsonNode>> filteredDraft =
        customObjectSyncOptions.applyBeforeCreateCallback(resourceDraft);

    assertThat(filteredDraft).containsSame(resourceDraft);
  }

  @Test
  void applyBeforeCreateCallback_WithCallbackReturningNull_ShouldReturnEmptyOptional() {
    final Function<CustomObjectDraft<JsonNode>, CustomObjectDraft<JsonNode>> draftFunction =
        customObjectDraft -> null;
    final CustomObjectSyncOptions customObjectSyncOptions =
        CustomObjectSyncOptionsBuilder.of(CTP_CLIENT).beforeCreateCallback(draftFunction).build();
    final CustomObjectDraft<JsonNode> resourceDraft = mock(CustomObjectDraft.class);

    final Optional<CustomObjectDraft<JsonNode>> filteredDraft =
        customObjectSyncOptions.applyBeforeCreateCallback(resourceDraft);

    assertThat(filteredDraft).isEmpty();
  }
}
