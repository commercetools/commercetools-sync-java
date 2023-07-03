package com.commercetools.sync.sdk2.types.helpers;

import static com.commercetools.api.models.common.LocalizedString.ofEnglish;
import static com.commercetools.sync.sdk2.types.helpers.TypeBatchValidator.TYPE_DRAFT_IS_NULL;
import static com.commercetools.sync.sdk2.types.helpers.TypeBatchValidator.TYPE_DRAFT_KEY_NOT_SET;
import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.type.TypeDraft;
import com.commercetools.api.models.type.TypeDraftBuilder;
import com.commercetools.sync.sdk2.types.TypeSyncOptions;
import com.commercetools.sync.sdk2.types.TypeSyncOptionsBuilder;
import java.util.*;
import javax.annotation.Nonnull;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TypeBatchValidatorTest {
  private List<String> errorCallBackMessages;
  private TypeSyncOptions syncOptions;
  private TypeSyncStatistics syncStatistics;

  @BeforeEach
  void setup() {
    errorCallBackMessages = new ArrayList<>();
    final ProjectApiRoot ctpClient = mock(ProjectApiRoot.class);
    syncOptions =
        TypeSyncOptionsBuilder.of(ctpClient)
            .errorCallback(
                (exception, oldResource, newResource, actions) -> {
                  errorCallBackMessages.add(exception.getMessage());
                })
            .build();
    syncStatistics = new TypeSyncStatistics();
  }

  @Test
  void validateAndCollectReferencedKeys_WithEmptyDraft_ShouldHaveEmptyResult() {
    final Set<TypeDraft> validDrafts = getValidDrafts(emptyList());

    assertThat(validDrafts).isEmpty();
    assertThat(errorCallBackMessages).isEmpty();
  }

  @Test
  void
      validateAndCollectReferencedKeys_WithNullTypeDraft_ShouldHaveValidationErrorAndEmptyResult() {
    final Set<TypeDraft> validDrafts = getValidDrafts(Collections.singletonList(null));

    assertThat(errorCallBackMessages).hasSize(1);
    assertThat(errorCallBackMessages.get(0)).isEqualTo(TYPE_DRAFT_IS_NULL);
    assertThat(validDrafts).isEmpty();
  }

  @Test
  void
      validateAndCollectReferencedKeys_WithTypeDraftWithNullKey_ShouldHaveValidationErrorAndEmptyResult() {
    final TypeDraft typeDraft = mock(TypeDraft.class);
    final Set<TypeDraft> validDrafts = getValidDrafts(Collections.singletonList(typeDraft));

    assertThat(errorCallBackMessages).hasSize(1);
    assertThat(errorCallBackMessages.get(0))
        .isEqualTo(format(TYPE_DRAFT_KEY_NOT_SET, typeDraft.getName()));
    assertThat(validDrafts).isEmpty();
  }

  @Test
  void
      validateAndCollectReferencedKeys_WithTypeDraftWithEmptyKey_ShouldHaveValidationErrorAndEmptyResult() {
    final TypeDraft typeDraft = mock(TypeDraft.class);
    when(typeDraft.getKey()).thenReturn(EMPTY);
    final Set<TypeDraft> validDrafts = getValidDrafts(Collections.singletonList(typeDraft));

    assertThat(errorCallBackMessages).hasSize(1);
    assertThat(errorCallBackMessages.get(0))
        .isEqualTo(format(TYPE_DRAFT_KEY_NOT_SET, typeDraft.getName()));
    assertThat(validDrafts).isEmpty();
  }

  @Test
  void validateAndCollectReferencedKeys_WithMixOfValidAndInvalidDrafts_ShouldValidateCorrectly() {
    final TypeDraft typeDraft =
        TypeDraftBuilder.of()
            .key("foo")
            .name(ofEnglish("name"))
            .resourceTypeIds(emptyList())
            .build();

    final TypeDraft typeDraftWithEmptyKey =
        TypeDraftBuilder.of().key("").name(ofEnglish("name")).resourceTypeIds(emptyList()).build();

    final TypeDraft typeDraftWithNullKey =
        TypeDraftBuilder.of()
            .key("null")
            .name(ofEnglish("name"))
            .resourceTypeIds(emptyList())
            .build();
    typeDraftWithNullKey.setKey(null);

    final TypeBatchValidator batchValidator = new TypeBatchValidator(syncOptions, syncStatistics);
    final ImmutablePair<Set<TypeDraft>, Set<String>> pair =
        batchValidator.validateAndCollectReferencedKeys(
            Arrays.asList(null, typeDraft, typeDraftWithEmptyKey, typeDraftWithNullKey));

    assertThat(pair.getLeft()).containsExactlyInAnyOrder(typeDraft);
    assertThat(pair.getRight()).containsExactlyInAnyOrder("foo");

    assertThat(errorCallBackMessages).hasSize(3);
    assertThat(errorCallBackMessages.get(0)).isEqualTo(TYPE_DRAFT_IS_NULL);
    assertThat(errorCallBackMessages.get(1))
        .isEqualTo(format(TYPE_DRAFT_KEY_NOT_SET, typeDraftWithEmptyKey.getName()));
    assertThat(errorCallBackMessages.get(2))
        .isEqualTo(format(TYPE_DRAFT_KEY_NOT_SET, typeDraftWithNullKey.getName()));
  }

  @Nonnull
  private Set<TypeDraft> getValidDrafts(@Nonnull final List<TypeDraft> typeDrafts) {
    final TypeBatchValidator batchValidator = new TypeBatchValidator(syncOptions, syncStatistics);
    final ImmutablePair<Set<TypeDraft>, Set<String>> pair =
        batchValidator.validateAndCollectReferencedKeys(typeDrafts);
    return pair.getLeft();
  }
}
