package com.commercetools.sync.customobjects.helpers;

import com.commercetools.sync.commons.helpers.BaseBatchValidator;
import com.commercetools.sync.customobjects.CustomObjectSyncOptions;
import com.fasterxml.jackson.databind.JsonNode;
import io.sphere.sdk.customobjects.CustomObjectDraft;
import org.apache.commons.lang3.tuple.ImmutablePair;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toSet;

public class CustomObjectBatchValidator
    extends BaseBatchValidator<CustomObjectDraft<JsonNode>, CustomObjectSyncOptions, CustomObjectSyncStatistics> {

    static final String CUSTOM_OBJECT_DRAFT_IS_NULL = "CustomObjectDraft is null.";

    public CustomObjectBatchValidator(@Nonnull final CustomObjectSyncOptions syncOptions,
                                      @Nonnull final CustomObjectSyncStatistics syncStatistics) {
        super(syncOptions, syncStatistics);
    }

    /**
     * Given the {@link List}&lt;{@link CustomObjectDraft}&gt; of drafts this method attempts to validate
     * drafts and return an {@link ImmutablePair}&lt;{@link Set}&lt;{@link CustomObjectDraft}&gt;,
     * {@link Set}&lt;{@link CustomObjectCompositeIdentifier} &gt;&gt; which contains the {@link Set} of valid drafts
     * and valid custom object identifiers (container with key).
     *
     * <p>A valid custom object draft is one which satisfies the following conditions:
     * <ol>
     * <li>It is not null</li>
     * </ol>
     *
     * @param customObjectDrafts the custom object drafts to validate and collect valid custom object identifiers.
     * @return {@link ImmutablePair}&lt;{@link Set}&lt;{@link CustomObjectDraft}&gt;,
     *      {@link Set}&lt;{@link CustomObjectCompositeIdentifier}&gt;&gt; which contains the {@link Set} of
     *      valid drafts and valid custom object identifiers (container with key).
     */
    @Override
    public ImmutablePair<Set<CustomObjectDraft<JsonNode>>, Set<CustomObjectCompositeIdentifier>>
        validateAndCollectReferencedKeys(@Nonnull final List<CustomObjectDraft<JsonNode>> customObjectDrafts) {

        final Set<CustomObjectDraft<JsonNode>> validDrafts = customObjectDrafts
            .stream()
            .filter(this::isValidCustomObjectDraft)
            .collect(Collectors.toSet());

        final Set<CustomObjectCompositeIdentifier> validIdentifiers = validDrafts
            .stream()
            .map(CustomObjectCompositeIdentifier::of)
            .collect(toSet());

        return ImmutablePair.of(validDrafts, validIdentifiers);
    }

    private boolean isValidCustomObjectDraft(
        @Nullable final CustomObjectDraft<JsonNode> customObjectDraft) {

        if (customObjectDraft == null) {
            handleError(CUSTOM_OBJECT_DRAFT_IS_NULL);
        } else {
            return true;
        }

        return false;
    }
}
