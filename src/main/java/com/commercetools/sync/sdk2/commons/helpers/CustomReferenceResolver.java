package com.commercetools.sync.sdk2.commons.helpers;

import static io.sphere.sdk.utils.CompletableFutureUtils.exceptionallyCompletedFuture;
import static java.lang.String.format;
import static java.util.concurrent.CompletableFuture.completedFuture;

import com.commercetools.api.models.category.CategoryDraft;
import com.commercetools.api.models.common.ResourceIdentifier;
import com.commercetools.api.models.type.CustomFieldsDraft;
import com.commercetools.api.models.type.CustomFieldsDraftBuilder;
import com.commercetools.api.models.type.FieldContainer;
import com.commercetools.api.models.type.TypeResourceIdentifierBuilder;
import com.commercetools.sync.sdk2.commons.BaseSyncOptions;
import com.commercetools.sync.sdk2.commons.exceptions.ReferenceResolutionException;
import com.commercetools.sync.sdk2.services.TypeService;
import io.vrap.rmf.base.client.Builder;
import io.vrap.rmf.base.client.Draft;
import java.util.concurrent.CompletionStage;
import java.util.function.BiFunction;
import java.util.function.Function;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * This class is responsible for providing an abstract implementation of reference resolution on
 * Custom CTP resources for example (Categories, inventories and resource with a custom field). For
 * concrete reference resolution implementation of the CTP resources, this class should be extended.
 *
 * @param <ResourceDraftT> the resource draft type for which reference resolution has to be done on.
 * @param <BuilderT> the resource draft builder where resolved values should be set. The builder
 *     type should correspond the {@code D} type.
 * @param <SyncOptionsT> a subclass implementation of {@link BaseSyncOptions} that is used to
 *     allow/deny some specific options, specified by the user, on reference resolution.
 */
public abstract class CustomReferenceResolver<
        ResourceDraftT extends Draft<ResourceDraftT>,
        BuilderT extends Builder<ResourceDraftT>,
        SyncOptionsT extends BaseSyncOptions>
    extends BaseReferenceResolver<ResourceDraftT, SyncOptionsT> {

  public static final String TYPE_DOES_NOT_EXIST = "Type with key '%s' doesn't exist.";
  private final TypeService typeService;

  protected CustomReferenceResolver(
      @Nonnull final SyncOptionsT options, @Nonnull final TypeService typeService) {
    super(options);
    this.typeService = typeService;
  }

  /**
   * Given a draft of {@code D} (e.g. {@link CategoryDraft}) this method attempts to resolve it's
   * custom type reference to return {@link CompletionStage} which contains a new instance of the
   * draft with the resolved custom type reference.
   *
   * <p>The method then tries to fetch the key of the custom type, optimistically from a cache. If
   * the key is is not found, the resultant draft would remain exactly the same as the passed draft
   * (without a custom type reference resolution).
   *
   * @param draftBuilder the draft builder to resolve it's references.
   * @return a {@link CompletionStage} that contains as a result a new draft instance with resolved
   *     custom type references or, in case an error occurs during reference resolution, a {@link
   *     ReferenceResolutionException}.
   */
  protected abstract CompletionStage<BuilderT> resolveCustomTypeReference(
      @Nonnull BuilderT draftBuilder);

  /**
   * Given a draft of {@code D} (e.g. {@link CategoryDraft}) this method attempts to resolve it's
   * custom type reference to return {@link CompletionStage} which contains a new instance of the
   * draft with the resolved custom type reference.
   *
   * <p>The method then tries to fetch the key of the custom type, optimistically from a cache. If
   * the key is is not found, the resultant draft would remain exactly the same as the passed draft
   * (without a custom type reference resolution).
   *
   * <p>Note: If the id field is set, then it is an evidence of resource existence on commercetools,
   * so we can issue an update/create API request right away without reference resolution.
   *
   * @param draftBuilder the draft builder to resolve it's references.
   * @param customGetter a function to return the CustomFieldsDraft instance of the draft builder.
   * @param customSetter a function to set the CustomFieldsDraft instance of the builder and return
   *     this builder.
   * @param errorMessage the error message to inject in the {@link ReferenceResolutionException} if
   *     it occurs.
   * @return a {@link CompletionStage} that contains as a result a new draft instance with resolved
   *     custom type references or, in case an error occurs during reference resolution, a {@link
   *     ReferenceResolutionException}.
   */
  @Nonnull
  protected CompletionStage<BuilderT> resolveCustomTypeReference(
      @Nonnull final BuilderT draftBuilder,
      @Nonnull final Function<BuilderT, CustomFieldsDraft> customGetter,
      @Nonnull final BiFunction<BuilderT, CustomFieldsDraft, BuilderT> customSetter,
      @Nonnull final String errorMessage) {

    final CustomFieldsDraft custom = customGetter.apply(draftBuilder);

    if (custom != null) {
      final ResourceIdentifier customType = custom.getType();

      if (customType.getId() == null) {
        String customTypeKey;

        try {
          customTypeKey = getCustomTypeKey(customType, errorMessage);
        } catch (ReferenceResolutionException referenceResolutionException) {
          return exceptionallyCompletedFuture(referenceResolutionException);
        }

        return fetchAndResolveTypeReference(
            draftBuilder, customSetter, custom.getFields(), customTypeKey, errorMessage);
      }
    }
    return completedFuture(draftBuilder);
  }

  /**
   * Given a custom fields object this method fetches the custom type reference id.
   *
   * @param customType the custom fields' Type.
   * @param referenceResolutionErrorMessage the message containing the information about the draft
   *     to attach to the {@link ReferenceResolutionException} in case it occurs.
   * @return a {@link CompletionStage} that contains as a result an optional which either contains
   *     the custom type id if it exists or empty if it doesn't.
   */
  private String getCustomTypeKey(
      @Nonnull final ResourceIdentifier customType,
      @Nonnull final String referenceResolutionErrorMessage)
      throws ReferenceResolutionException {

    try {
      return getKeyFromResourceIdentifier(customType);
    } catch (ReferenceResolutionException exception) {
      final String errorMessage =
          format("%s Reason: %s", referenceResolutionErrorMessage, exception.getMessage());
      throw new ReferenceResolutionException(errorMessage, exception);
    }
  }

  @Nonnull
  private CompletionStage<BuilderT> fetchAndResolveTypeReference(
      @Nonnull final BuilderT draftBuilder,
      @Nonnull final BiFunction<BuilderT, CustomFieldsDraft, BuilderT> customSetter,
      @Nullable final FieldContainer customFields,
      @Nonnull final String typeKey,
      @Nonnull final String referenceResolutionErrorMessage) {

    return typeService
        .fetchCachedTypeId(typeKey)
        .thenCompose(
            resolvedTypeIdOptional ->
                resolvedTypeIdOptional
                    .map(
                        resolvedTypeId ->
                            completedFuture(
                                customSetter.apply(
                                    draftBuilder,
                                    // todo: I did not understand this part without debugging, check
                                    // with running code.
                                    // where to add resolvedTypeId
                                    CustomFieldsDraftBuilder.of()
                                        .fields(customFields)
                                        .type(
                                            TypeResourceIdentifierBuilder.of()
                                                .id(resolvedTypeId)
                                                .build())
                                        .build())))
                    .orElseGet(
                        () -> {
                          final String errorMessage =
                              format(
                                  "%s Reason: %s",
                                  referenceResolutionErrorMessage,
                                  format(TYPE_DOES_NOT_EXIST, typeKey));
                          return exceptionallyCompletedFuture(
                              new ReferenceResolutionException(errorMessage));
                        }));
  }
}
