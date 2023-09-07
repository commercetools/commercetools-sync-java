package com.commercetools.sync.customobjects.helpers;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.custom_object.CustomObjectDraft;
import com.commercetools.api.models.custom_object.CustomObjectDraftBuilder;
import com.commercetools.sync.customobjects.CustomObjectSyncOptions;
import com.commercetools.sync.customobjects.CustomObjectSyncOptionsBuilder;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import javax.annotation.Nonnull;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CustomObjectBatchValidatorTest {
  private List<String> errorCallBackMessages;
  private CustomObjectSyncOptions syncOptions;
  private CustomObjectSyncStatistics syncStatistics;

  @BeforeEach
  void setup() {
    errorCallBackMessages = new ArrayList<>();
    final ProjectApiRoot ctpClient = mock(ProjectApiRoot.class);
    syncOptions =
        CustomObjectSyncOptionsBuilder.of(ctpClient)
            .errorCallback(
                (exception, oldResource, newResource, actions) -> {
                  errorCallBackMessages.add(exception.getMessage());
                })
            .build();
    syncStatistics = new CustomObjectSyncStatistics();
  }

  @Test
  void validateAndCollectReferencedKeys_WithValidDraft_ShouldHaveCorrectResult() {
    CustomObjectDraft customObjectDraft =
        CustomObjectDraftBuilder.of()
            .container("container")
            .key("key")
            .value(JsonNodeFactory.instance.numberNode(1))
            .build();

    final CustomObjectBatchValidator batchValidator =
        new CustomObjectBatchValidator(syncOptions, syncStatistics);
    final ImmutablePair<Set<CustomObjectDraft>, Set<CustomObjectCompositeIdentifier>> result =
        batchValidator.validateAndCollectReferencedKeys(singletonList(customObjectDraft));

    assertThat(result.getLeft()).contains(customObjectDraft);
    assertThat(errorCallBackMessages).isEmpty();
    assertThat(result.getRight()).contains(CustomObjectCompositeIdentifier.of(customObjectDraft));
  }

  @Test
  void validateAndCollectReferencedKeys_WithEmptyDraft_ShouldHaveEmptyResult() {
    final Set<CustomObjectDraft> validDrafts = getValidDrafts(emptyList());

    assertThat(validDrafts).isEmpty();
    assertThat(errorCallBackMessages).isEmpty();
  }

  @Test
  void
      validateAndCollectReferencedKeys_WithNullTypeDraft_ShouldHaveValidationErrorAndEmptyResult() {
    final Set<CustomObjectDraft> validDrafts = getValidDrafts(Collections.singletonList(null));

    assertThat(errorCallBackMessages).hasSize(1);
    assertThat(errorCallBackMessages.get(0))
        .isEqualTo(CustomObjectBatchValidator.CUSTOM_OBJECT_DRAFT_IS_NULL);
    assertThat(validDrafts).isEmpty();
  }

  @Nonnull
  private Set<CustomObjectDraft> getValidDrafts(
      @Nonnull final List<CustomObjectDraft> customObjectDrafts) {

    final CustomObjectBatchValidator batchValidator =
        new CustomObjectBatchValidator(syncOptions, syncStatistics);
    final ImmutablePair<Set<CustomObjectDraft>, Set<CustomObjectCompositeIdentifier>> pair =
        batchValidator.validateAndCollectReferencedKeys(customObjectDrafts);
    return pair.getLeft();
  }
}
