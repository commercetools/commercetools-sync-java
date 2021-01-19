package com.commercetools.sync.customobjects.helpers;

import static com.commercetools.sync.customobjects.helpers.CustomObjectBatchValidator.CUSTOM_OBJECT_DRAFT_IS_NULL;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.commercetools.sync.customobjects.CustomObjectSyncOptions;
import com.commercetools.sync.customobjects.CustomObjectSyncOptionsBuilder;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.customobjects.CustomObjectDraft;
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
    final SphereClient ctpClient = mock(SphereClient.class);
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
    CustomObjectDraft<JsonNode> customObjectDraft =
        CustomObjectDraft.ofUnversionedUpsert(
            "container", "key", JsonNodeFactory.instance.numberNode(1));

    final CustomObjectBatchValidator batchValidator =
        new CustomObjectBatchValidator(syncOptions, syncStatistics);
    final ImmutablePair<Set<CustomObjectDraft<JsonNode>>, Set<CustomObjectCompositeIdentifier>>
        result = batchValidator.validateAndCollectReferencedKeys(singletonList(customObjectDraft));

    assertThat(result.getLeft()).contains(customObjectDraft);
    assertThat(errorCallBackMessages).isEmpty();
    assertThat(result.getRight()).contains(CustomObjectCompositeIdentifier.of(customObjectDraft));
  }

  @Test
  void validateAndCollectReferencedKeys_WithEmptyDraft_ShouldHaveEmptyResult() {
    final Set<CustomObjectDraft<JsonNode>> validDrafts = getValidDrafts(emptyList());

    assertThat(validDrafts).isEmpty();
    assertThat(errorCallBackMessages).isEmpty();
  }

  @Test
  void
      validateAndCollectReferencedKeys_WithNullTypeDraft_ShouldHaveValidationErrorAndEmptyResult() {
    final Set<CustomObjectDraft<JsonNode>> validDrafts =
        getValidDrafts(Collections.singletonList(null));

    assertThat(errorCallBackMessages).hasSize(1);
    assertThat(errorCallBackMessages.get(0)).isEqualTo(CUSTOM_OBJECT_DRAFT_IS_NULL);
    assertThat(validDrafts).isEmpty();
  }

  @Nonnull
  private Set<CustomObjectDraft<JsonNode>> getValidDrafts(
      @Nonnull final List<CustomObjectDraft<JsonNode>> customObjectDrafts) {

    final CustomObjectBatchValidator batchValidator =
        new CustomObjectBatchValidator(syncOptions, syncStatistics);
    final ImmutablePair<Set<CustomObjectDraft<JsonNode>>, Set<CustomObjectCompositeIdentifier>>
        pair = batchValidator.validateAndCollectReferencedKeys(customObjectDrafts);
    return pair.getLeft();
  }
}
