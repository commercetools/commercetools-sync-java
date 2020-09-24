package com.commercetools.sync.types.helpers;

import com.commercetools.sync.types.TypeSyncOptions;
import com.commercetools.sync.types.TypeSyncOptionsBuilder;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.types.TypeDraft;
import io.sphere.sdk.types.TypeDraftBuilder;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static com.commercetools.sync.types.helpers.TypeBatchValidator.TYPE_DRAFT_IS_NULL;
import static com.commercetools.sync.types.helpers.TypeBatchValidator.TYPE_DRAFT_KEY_NOT_SET;
import static io.sphere.sdk.models.LocalizedString.ofEnglish;
import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TypeBatchValidatorTest {
    private List<String> errorCallBackMessages;
    private TypeSyncOptions syncOptions;
    private TypeSyncStatistics syncStatistics;

    @BeforeEach
    void setup() {
        errorCallBackMessages = new ArrayList<>();
        final SphereClient ctpClient = mock(SphereClient.class);
        syncOptions = TypeSyncOptionsBuilder
            .of(ctpClient)
            .errorCallback((exception, oldResource, newResource, actions) -> {
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
    void validateAndCollectReferencedKeys_WithNullTypeDraft_ShouldHaveValidationErrorAndEmptyResult() {
        final Set<TypeDraft> validDrafts = getValidDrafts(Collections.singletonList(null));

        assertThat(errorCallBackMessages).hasSize(1);
        assertThat(errorCallBackMessages.get(0)).isEqualTo(TYPE_DRAFT_IS_NULL);
        assertThat(validDrafts).isEmpty();
    }

    @Test
    void validateAndCollectReferencedKeys_WithTypeDraftWithNullKey_ShouldHaveValidationErrorAndEmptyResult() {
        final TypeDraft typeDraft = mock(TypeDraft.class);
        final Set<TypeDraft> validDrafts = getValidDrafts(Collections.singletonList(typeDraft));

        assertThat(errorCallBackMessages).hasSize(1);
        assertThat(errorCallBackMessages.get(0))
            .isEqualTo(format(TYPE_DRAFT_KEY_NOT_SET, typeDraft.getName()));
        assertThat(validDrafts).isEmpty();
    }

    @Test
    void validateAndCollectReferencedKeys_WithTypeDraftWithEmptyKey_ShouldHaveValidationErrorAndEmptyResult() {
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
        final TypeDraft typeDraft = TypeDraftBuilder
            .of("foo", ofEnglish("name"), emptySet())
            .build();

        final TypeDraft typeDraftWithEmptyKey = TypeDraftBuilder
            .of("", ofEnglish("name"), emptySet())
            .build();

        final TypeDraft typeDraftWithNullKey = TypeDraftBuilder
            .of(null, ofEnglish("name"), emptySet())
            .build();

        final TypeBatchValidator batchValidator = new TypeBatchValidator(syncOptions, syncStatistics);
        final ImmutablePair<Set<TypeDraft>, Set<String>> pair = batchValidator.validateAndCollectReferencedKeys(
            Arrays.asList(null, typeDraft, typeDraftWithEmptyKey, typeDraftWithNullKey));

        assertThat(pair.getLeft()).containsExactlyInAnyOrder(typeDraft);
        assertThat(pair.getRight()).containsExactlyInAnyOrder("foo");

        assertThat(errorCallBackMessages).hasSize(3);
        assertThat(errorCallBackMessages.get(0))
            .isEqualTo(TYPE_DRAFT_IS_NULL);
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
