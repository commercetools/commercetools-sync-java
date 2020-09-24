package com.commercetools.sync.cartdiscounts.helpers;

import com.commercetools.sync.cartdiscounts.CartDiscountSyncOptions;
import com.commercetools.sync.cartdiscounts.CartDiscountSyncOptionsBuilder;
import io.sphere.sdk.cartdiscounts.CartDiscountDraft;
import io.sphere.sdk.client.SphereClient;
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

import static com.commercetools.sync.cartdiscounts.helpers.CartDiscountBatchValidator.CART_DISCOUNT_DRAFT_IS_NULL;
import static com.commercetools.sync.cartdiscounts.helpers.CartDiscountBatchValidator.CART_DISCOUNT_DRAFT_KEY_NOT_SET;
import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CartDiscountBatchValidatorTest {

    private CartDiscountSyncOptions syncOptions;
    private CartDiscountSyncStatistics syncStatistics;
    private List<String> errorCallBackMessages;

    @BeforeEach
    void setup() {
        errorCallBackMessages = new ArrayList<>();
        final SphereClient ctpClient = mock(SphereClient.class);

        syncOptions = CartDiscountSyncOptionsBuilder
            .of(ctpClient)
            .errorCallback((exception, oldResource, newResource, updateActions) ->
                errorCallBackMessages.add(exception.getMessage()))
            .build();
        syncStatistics = mock(CartDiscountSyncStatistics.class);
    }

    @Test
    void validateAndCollectReferencedKeys_WithEmptyDraft_ShouldHaveEmptyResult() {
        final Set<CartDiscountDraft> validDrafts = getValidDrafts(Collections.emptyList());

        assertThat(errorCallBackMessages).hasSize(0);
        assertThat(validDrafts).isEmpty();
    }

    @Test
    void validateAndCollectReferencedKeys_WithNullCartDiscountDraft_ShouldHaveValidationErrorAndEmptyResult() {
        final Set<CartDiscountDraft> validDrafts = getValidDrafts(Collections.singletonList(null));

        assertThat(errorCallBackMessages).hasSize(1);
        assertThat(errorCallBackMessages.get(0)).isEqualTo(CART_DISCOUNT_DRAFT_IS_NULL);
        assertThat(validDrafts).isEmpty();
    }

    @Test
    void validateAndCollectReferencedKeys_WithCartDiscountDraftWithNullKey_ShouldHaveValidationErrorAndEmptyResult() {
        final CartDiscountDraft cartDiscountDraft = mock(CartDiscountDraft.class);
        final Set<CartDiscountDraft> validDrafts = getValidDrafts(Collections.singletonList(cartDiscountDraft));

        assertThat(errorCallBackMessages).hasSize(1);
        assertThat(errorCallBackMessages.get(0))
            .isEqualTo(format(CART_DISCOUNT_DRAFT_KEY_NOT_SET, cartDiscountDraft.getName()));
        assertThat(validDrafts).isEmpty();
    }

    @Test
    void validateAndCollectReferencedKeys_WithCartDiscountDraftWithEmptyKey_ShouldHaveValidationErrorAndEmptyResult() {
        final CartDiscountDraft cartDiscountDraft = mock(CartDiscountDraft.class);
        when(cartDiscountDraft.getKey()).thenReturn(EMPTY);
        final Set<CartDiscountDraft> validDrafts = getValidDrafts(Collections.singletonList(cartDiscountDraft));

        assertThat(errorCallBackMessages).hasSize(1);
        assertThat(errorCallBackMessages.get(0))
            .isEqualTo(format(CART_DISCOUNT_DRAFT_KEY_NOT_SET, cartDiscountDraft.getName()));
        assertThat(validDrafts).isEmpty();
    }

    @Test
    void validateAndCollectReferencedKeys_WithValidDrafts_ShouldReturnCorrectResults() {
        final CartDiscountDraft validCartDiscountDraft = mock(CartDiscountDraft.class);
        when(validCartDiscountDraft.getKey()).thenReturn("validDraftKey");
        when(validCartDiscountDraft.getCustom())
            .thenReturn(CustomFieldsDraft.ofTypeKeyAndJson("typeKey", Collections.emptyMap()));

        final CartDiscountDraft validMainCartDiscountDraft = mock(CartDiscountDraft.class);
        when(validMainCartDiscountDraft.getKey()).thenReturn("validDraftKey1");

        final CartDiscountDraft invalidCartDiscountDraft = mock(CartDiscountDraft.class);

        final CartDiscountBatchValidator cartDiscountBatchValidator =
            new CartDiscountBatchValidator(syncOptions, syncStatistics);
        final ImmutablePair<Set<CartDiscountDraft>, Set<String>> pair
            = cartDiscountBatchValidator.validateAndCollectReferencedKeys(
            Arrays.asList(validCartDiscountDraft, invalidCartDiscountDraft, validMainCartDiscountDraft));

        assertThat(errorCallBackMessages).hasSize(1);
        assertThat(errorCallBackMessages.get(0))
            .isEqualTo(format(CART_DISCOUNT_DRAFT_KEY_NOT_SET, invalidCartDiscountDraft.getName()));
        assertThat(pair.getLeft())
            .containsExactlyInAnyOrder(validCartDiscountDraft, validMainCartDiscountDraft);
        assertThat(pair.getRight())
            .containsExactlyInAnyOrder("typeKey");
    }

    @Nonnull
    private Set<CartDiscountDraft> getValidDrafts(@Nonnull final List<CartDiscountDraft> cartDiscountDrafts) {
        final CartDiscountBatchValidator cartDiscountBatchValidator =
            new CartDiscountBatchValidator(syncOptions, syncStatistics);
        final ImmutablePair<Set<CartDiscountDraft>, Set<String>> pair =
            cartDiscountBatchValidator.validateAndCollectReferencedKeys(cartDiscountDrafts);
        return pair.getLeft();
    }
}
