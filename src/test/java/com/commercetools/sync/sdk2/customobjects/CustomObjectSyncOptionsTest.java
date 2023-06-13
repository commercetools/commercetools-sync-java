package com.commercetools.sync.sdk2.customobjects;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.custom_object.CustomObject;
import com.commercetools.api.models.custom_object.CustomObjectDraft;
import com.commercetools.api.models.custom_object.CustomObjectDraftBuilder;
import com.commercetools.sync.sdk2.commons.utils.TriFunction;
import com.commercetools.sync.sdk2.customobjects.models.NoopResourceUpdateAction;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

@SuppressWarnings("unchecked")
class CustomObjectSyncOptionsTest {

  private static ProjectApiRoot CTP_CLIENT = mock(ProjectApiRoot.class);

  private interface MockTriFunction
      extends TriFunction<
          List<NoopResourceUpdateAction>,
          CustomObjectDraft,
          CustomObject,
          List<NoopResourceUpdateAction>> {}

  @Test
  void applyBeforeUpdateCallback_WithNullCallbackAndEmptyUpdateActions_ShouldReturnIdenticalList() {
    final CustomObjectSyncOptions customObjectSyncOptions =
        CustomObjectSyncOptionsBuilder.of(CTP_CLIENT).build();

    final List<NoopResourceUpdateAction> updateActions = emptyList();

    final List<NoopResourceUpdateAction> filteredList =
        customObjectSyncOptions.applyBeforeUpdateCallback(
            updateActions, mock(CustomObjectDraft.class), mock(CustomObject.class));

    assertThat(filteredList).isSameAs(updateActions);
  }

  @Test
  void
      applyBeforeUpdateCallback_WithNullReturnCallbackAndEmptyUpdateActions_ShouldReturnEmptyList() {
    final TriFunction<
            List<NoopResourceUpdateAction>,
            CustomObjectDraft,
            CustomObject,
            List<NoopResourceUpdateAction>>
        beforeUpdateCallback = (updateActions, newCustomObject, oldCustomObject) -> null;
    final CustomObjectSyncOptions customObjectSyncOptions =
        CustomObjectSyncOptionsBuilder.of(CTP_CLIENT)
            .beforeUpdateCallback(beforeUpdateCallback)
            .build();
    final List<NoopResourceUpdateAction> updateActions = emptyList();

    final List<NoopResourceUpdateAction> filteredList =
        customObjectSyncOptions.applyBeforeUpdateCallback(
            updateActions, mock(CustomObjectDraft.class), mock(CustomObject.class));

    assertAll(
        () -> assertThat(filteredList).isEqualTo(updateActions),
        () -> assertThat(filteredList).isEmpty());
  }

  @Test
  void applyBeforeUpdateCallback_WithEmptyUpdateActions_ShouldNotApplyBeforeUpdateCallback() {
    final MockTriFunction beforeUpdateCallback = mock(MockTriFunction.class);
    final CustomObjectSyncOptions customObjectSyncOptions =
        CustomObjectSyncOptionsBuilder.of(CTP_CLIENT)
            .beforeUpdateCallback(beforeUpdateCallback)
            .build();

    final List<NoopResourceUpdateAction> filteredList =
        customObjectSyncOptions.applyBeforeUpdateCallback(
            emptyList(), mock(CustomObjectDraft.class), mock(CustomObject.class));

    assertThat(filteredList).isEmpty();
    verify(beforeUpdateCallback, never()).apply(any(), any(), any());
  }

  @Test
  void applyBeforeCreateCallback_WithCallback_ShouldReturnFilteredDraft() {

    final Function<CustomObjectDraft, CustomObjectDraft> draftFunction =
        customObjectDraft ->
            CustomObjectDraftBuilder.of()
                .container(customObjectDraft.getContainer() + "_filteredContainer")
                .key(customObjectDraft.getKey() + "_filteredKey")
                .value(customObjectDraft.getValue())
                .build();
    final CustomObjectSyncOptions customObjectSyncOptions =
        CustomObjectSyncOptionsBuilder.of(CTP_CLIENT).beforeCreateCallback(draftFunction).build();
    final CustomObjectDraft resourceDraft = mock(CustomObjectDraft.class);
    when(resourceDraft.getKey()).thenReturn("myKey");
    when(resourceDraft.getContainer()).thenReturn("myContainer");
    when(resourceDraft.getValue()).thenReturn("value");

    final Optional<CustomObjectDraft> filteredDraft =
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
    final CustomObjectDraft resourceDraft = mock(CustomObjectDraft.class);

    final Optional<CustomObjectDraft> filteredDraft =
        customObjectSyncOptions.applyBeforeCreateCallback(resourceDraft);

    assertThat(filteredDraft).containsSame(resourceDraft);
  }

  @Test
  void applyBeforeCreateCallback_WithCallbackReturningNull_ShouldReturnEmptyOptional() {
    final Function<CustomObjectDraft, CustomObjectDraft> draftFunction = customObjectDraft -> null;
    final CustomObjectSyncOptions customObjectSyncOptions =
        CustomObjectSyncOptionsBuilder.of(CTP_CLIENT).beforeCreateCallback(draftFunction).build();
    final CustomObjectDraft resourceDraft = mock(CustomObjectDraft.class);

    final Optional<CustomObjectDraft> filteredDraft =
        customObjectSyncOptions.applyBeforeCreateCallback(resourceDraft);

    assertThat(filteredDraft).isEmpty();
  }
}
