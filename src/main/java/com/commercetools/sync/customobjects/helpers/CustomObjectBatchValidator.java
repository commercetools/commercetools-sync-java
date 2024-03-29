package com.commercetools.sync.customobjects.helpers;

import static java.util.stream.Collectors.toSet;

import com.commercetools.api.models.custom_object.CustomObjectDraft;
import com.commercetools.sync.commons.helpers.BaseBatchValidator;
import com.commercetools.sync.customobjects.CustomObjectSyncOptions;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.apache.commons.lang3.tuple.ImmutablePair;

public class CustomObjectBatchValidator
    extends BaseBatchValidator<
        CustomObjectDraft, CustomObjectSyncOptions, CustomObjectSyncStatistics> {

  static final String CUSTOM_OBJECT_DRAFT_IS_NULL = "CustomObjectDraft is null.";

  public CustomObjectBatchValidator(
      @Nonnull final CustomObjectSyncOptions syncOptions,
      @Nonnull final CustomObjectSyncStatistics syncStatistics) {
    super(syncOptions, syncStatistics);
  }

  /**
   * Given the {@link List}&lt;{@link CustomObjectDraft}&gt; of drafts this method attempts to
   * validate drafts and return an {@link ImmutablePair}&lt;{@link Set}&lt;{@link
   * CustomObjectDraft}&gt;, {@link Set}&lt;{@link CustomObjectCompositeIdentifier} &gt;&gt; which
   * contains the {@link Set} of valid drafts and valid custom object identifiers (container with
   * key).
   *
   * <p>A valid custom object draft is one which satisfies the following conditions:
   *
   * <ol>
   *   <li>It is not null
   * </ol>
   *
   * @param customObjectDrafts the custom object drafts to validate and collect valid custom object
   *     identifiers.
   * @return {@link ImmutablePair}&lt;{@link Set}&lt;{@link CustomObjectDraft}&gt;, {@link
   *     Set}&lt;{@link CustomObjectCompositeIdentifier}&gt;&gt; which contains the {@link Set} of
   *     valid drafts and valid custom object identifiers (container with key).
   */
  @Override
  public ImmutablePair<Set<CustomObjectDraft>, Set<CustomObjectCompositeIdentifier>>
      validateAndCollectReferencedKeys(@Nonnull final List<CustomObjectDraft> customObjectDrafts) {

    final Set<CustomObjectDraft> validDrafts =
        customObjectDrafts.stream()
            .filter(this::isValidCustomObjectDraft)
            .collect(Collectors.toSet());

    final Set<CustomObjectCompositeIdentifier> validIdentifiers =
        validDrafts.stream().map(CustomObjectCompositeIdentifier::of).collect(toSet());

    return ImmutablePair.of(validDrafts, validIdentifiers);
  }

  private boolean isValidCustomObjectDraft(@Nullable final CustomObjectDraft customObjectDraft) {

    if (customObjectDraft == null) {
      handleError(CUSTOM_OBJECT_DRAFT_IS_NULL);
      return false;
    } else {
      return true;
    }
  }
}
