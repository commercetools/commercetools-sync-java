package com.commercetools.sync.shoppinglists.helpers;

import com.commercetools.sync.shoppinglists.ShoppingListSyncOptions;
import com.commercetools.sync.shoppinglists.ShoppingListSyncOptionsBuilder;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.shoppinglists.ShoppingListDraft;
import io.sphere.sdk.types.CustomFieldsDraft;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static com.commercetools.sync.shoppinglists.helpers.ShoppingListBatchValidator.SHOPPING_LIST_DRAFT_IS_NULL;
import static com.commercetools.sync.shoppinglists.helpers.ShoppingListBatchValidator.SHOPPING_LIST_DRAFT_KEY_NOT_SET;
import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ShoppingListBatchValidatorTest {

    private ShoppingListSyncOptions syncOptions;
    private ShoppingListSyncStatistics syncStatistics;
    private List<String> errorCallBackMessages;

    @BeforeEach
    void setup() {
        errorCallBackMessages = new ArrayList<>();
        final SphereClient ctpClient = mock(SphereClient.class);

        syncOptions = ShoppingListSyncOptionsBuilder
            .of(ctpClient)
            .errorCallback((exception, oldResource, newResource, updateActions) ->
                errorCallBackMessages.add(exception.getMessage()))
            .build();
        syncStatistics = mock(ShoppingListSyncStatistics.class);
    }

    @Test
    void validateAndCollectReferencedKeys_WithEmptyDraft_ShouldHaveEmptyResult() {
        final Set<ShoppingListDraft> validDrafts = getValidDrafts(Collections.emptyList());

        assertThat(errorCallBackMessages).hasSize(0);
        assertThat(validDrafts).isEmpty();
    }

    @Test
    void validateAndCollectReferencedKeys_WithNullShoppingListDraft_ShouldHaveValidationErrorAndEmptyResult() {
        final Set<ShoppingListDraft> validDrafts = getValidDrafts(Collections.singletonList(null));

        assertThat(errorCallBackMessages).hasSize(1);
        assertThat(errorCallBackMessages.get(0)).isEqualTo(SHOPPING_LIST_DRAFT_IS_NULL);
        assertThat(validDrafts).isEmpty();
    }

    @Test
    void validateAndCollectReferencedKeys_WithShoppingListDraftWithNullKey_ShouldHaveValidationErrorAndEmptyResult() {
        final ShoppingListDraft shoppingListDraft = mock(ShoppingListDraft.class);
        final Set<ShoppingListDraft> validDrafts = getValidDrafts(Collections.singletonList(shoppingListDraft));

        assertThat(errorCallBackMessages).hasSize(1);
        assertThat(errorCallBackMessages.get(0))
            .isEqualTo(format(SHOPPING_LIST_DRAFT_KEY_NOT_SET, shoppingListDraft.getName()));
        assertThat(validDrafts).isEmpty();
    }

    @Test
    void validateAndCollectReferencedKeys_WithShoppingListDraftWithEmptyKey_ShouldHaveValidationErrorAndEmptyResult() {
        final ShoppingListDraft shoppingListDraft = mock(ShoppingListDraft.class);
        when(shoppingListDraft.getKey()).thenReturn(EMPTY);
        final Set<ShoppingListDraft> validDrafts = getValidDrafts(Collections.singletonList(shoppingListDraft));

        assertThat(errorCallBackMessages).hasSize(1);
        assertThat(errorCallBackMessages.get(0))
            .isEqualTo(format(SHOPPING_LIST_DRAFT_KEY_NOT_SET, shoppingListDraft.getName()));
        assertThat(validDrafts).isEmpty();
    }

    @Test
    void validateAndCollectReferencedKeys_WithValidDrafts_ShouldReturnCorrectResults() {
        final ShoppingListDraft validShoppingListDraft = mock(ShoppingListDraft.class);
        when(validShoppingListDraft.getKey()).thenReturn("validDraftKey");
        when(validShoppingListDraft.getCustom())
            .thenReturn(CustomFieldsDraft.ofTypeKeyAndJson("typeKey", Collections.emptyMap()));

        final ShoppingListDraft validMainShoppingListDraft = mock(ShoppingListDraft.class);
        when(validMainShoppingListDraft.getKey()).thenReturn("validDraftKey1");

        final ShoppingListDraft invalidShoppingListDraft = mock(ShoppingListDraft.class);

        final ShoppingListBatchValidator shoppingListBatchValidator =
            new ShoppingListBatchValidator(syncOptions, syncStatistics);
        final ImmutablePair<Set<ShoppingListDraft>, Set<String>> pair
            = shoppingListBatchValidator.validateAndCollectReferencedKeys(
            Arrays.asList(validShoppingListDraft, invalidShoppingListDraft, validMainShoppingListDraft));

        assertThat(errorCallBackMessages).hasSize(1);
        assertThat(errorCallBackMessages.get(0))
            .isEqualTo(format(SHOPPING_LIST_DRAFT_KEY_NOT_SET, invalidShoppingListDraft.getName()));
        assertThat(pair.getLeft())
            .containsExactlyInAnyOrder(validShoppingListDraft, validMainShoppingListDraft);
        assertThat(pair.getRight())
            .containsExactlyInAnyOrder("typeKey");
    }

    @Nonnull
    private Set<ShoppingListDraft> getValidDrafts(@Nonnull final List<ShoppingListDraft> shoppingListDrafts) {
        final ShoppingListBatchValidator shoppingListBatchValidator =
            new ShoppingListBatchValidator(syncOptions, syncStatistics);
        final ImmutablePair<Set<ShoppingListDraft>, Set<String>> pair =
            shoppingListBatchValidator.validateAndCollectReferencedKeys(shoppingListDrafts);
        return pair.getLeft();
    }
}
