package com.commercetools.sync.types.helpers;

import com.commercetools.sync.commons.helpers.BaseBatchValidator;
import com.commercetools.sync.types.TypeSyncOptions;
import io.sphere.sdk.types.TypeDraft;
import org.apache.commons.lang3.tuple.ImmutablePair;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.lang3.StringUtils.isBlank;

public class TypeBatchValidator
    extends BaseBatchValidator<TypeDraft, TypeSyncOptions, TypeSyncStatistics> {

    static final String TYPE_DRAFT_KEY_NOT_SET = "TypeDraft with name: %s doesn't have a key. "
        + "Please make sure all type drafts have keys.";
    static final String TYPE_DRAFT_IS_NULL = "TypeDraft is null.";

    public TypeBatchValidator(@Nonnull final TypeSyncOptions syncOptions,
                              @Nonnull final TypeSyncStatistics syncStatistics) {
        super(syncOptions, syncStatistics);
    }

    /**
     * Given the {@link List}&lt;{@link TypeDraft}&gt; of drafts this method attempts to validate
     * drafts and return an {@link ImmutablePair}&lt;{@link Set}&lt;{@link TypeDraft}&gt;,{@link Set}&lt;{@link String}
     * &gt;&gt; which contains the {@link Set} of valid drafts and valid type keys.
     *
     * <p>A valid type draft is one which satisfies the following conditions:
     * <ol>
     * <li>It is not null</li>
     * <li>It has a key which is not blank (null/empty)</li>
     * </ol>
     *
     * @param typeDrafts the type drafts to validate and collect valid type keys.
     * @return {@link ImmutablePair}&lt;{@link Set}&lt;{@link TypeDraft}&gt;,
     *      {@link Set}&lt;{@link String}&gt;&gt; which contains the {@link Set} of valid drafts and
     *      valid type keys.
     */
    @Override
    public ImmutablePair<Set<TypeDraft>, Set<String>> validateAndCollectReferencedKeys(
        @Nonnull final List<TypeDraft> typeDrafts) {

        final Set<TypeDraft> validDrafts = typeDrafts
            .stream()
            .filter(this::isValidTypeDraft)
            .collect(Collectors.toSet());

        final Set<String> validKeys = validDrafts
            .stream()
            .map(TypeDraft::getKey)
            .collect(toSet());

        return ImmutablePair.of(validDrafts, validKeys);
    }

    private boolean isValidTypeDraft(
        @Nullable final TypeDraft typeDraft) {

        if (typeDraft == null) {
            handleError(TYPE_DRAFT_IS_NULL);
        } else if (isBlank(typeDraft.getKey())) {
            handleError(format(TYPE_DRAFT_KEY_NOT_SET, typeDraft.getName()));
        } else {
            return true;
        }

        return false;
    }
}
