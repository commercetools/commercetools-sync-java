package com.commercetools.sync.commons.helpers;


import com.commercetools.sync.categories.CategorySyncOptions;
import com.commercetools.sync.commons.BaseSyncOptions;
import com.commercetools.sync.commons.exceptions.ReferenceResolutionException;
import com.commercetools.sync.services.CategoryService;
import com.commercetools.sync.services.TypeService;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.categories.CategoryDraft;
import io.sphere.sdk.categories.CategoryDraftBuilder;
import io.sphere.sdk.models.Reference;
import io.sphere.sdk.models.ResourceIdentifier;
import io.sphere.sdk.types.CustomFieldsDraft;

import javax.annotation.Nonnull;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

public final class ReferenceResolver {
    private static final String FAILED_TO_RESOLVE_PARENT = "Failed to resolve parent reference.";
    private static final String FAILED_TO_RESOLVE_CUSTOM_TYPE = "Failed to resolve custom type reference.";
    private static final String UUID_NOT_ALLOWED = "Found a UUID in the id field. Expecting a key without a UUID value."
        + " If you want to allow UUID values for your keys please use the setAllowUuid(true) option for your sync "
        + "options.";
    private static final String UNSET_ID_FIELD = "Reference 'id' field value is blank (null/empty).";

    /**
     * Given a {@link CategoryDraft} this method attempts to resolve both the parent category reference and the custom
     * type reference. A {@link TypeService} and {@link CategoryService} are also passed to the method in order to be
     * able to fetch the actual type id and the parent category id from the CTP project and replace them in the
     * reference id fields on the draft. The {@link CategorySyncOptions} instance is passed to infer whether it's
     * allowed to accept keys in UUID format or not, if not, a {@link ReferenceResolutionException} will be thrown.
     *
     * <b>Note</b> This method will block to fetch the actual ids from the CTP project on the first call. Later calls,
     * will utilise the services' caches to fetch the id.
     *
     * @param categoryDraft   the category draft to resolve it's references.
     * @param typeService     the service with which the type id is fetched.
     * @param categoryService the service with which the parent category id is fetched.
     * @param options         the options which specifies whether it's allowed to resolve keys in UUID format.
     * @return a new Category draft instance with resolved references.
     * @throws ReferenceResolutionException in case an error occurs during reference resolution. Either due to fail to
     *                                      fetch the ids from the CTP project or either due to setting an invalid key
     *                                      for references.
     */
    public static CategoryDraft resolveReferences(@Nonnull final CategoryDraft categoryDraft,
                                                  @Nonnull final TypeService typeService,
                                                  @Nonnull final CategoryService categoryService,
                                                  @Nonnull final CategorySyncOptions options)
        throws ReferenceResolutionException {
        final CategoryDraft draftWithResolvedCustomTypeReference = resolveCategoryCustomTypeReference(categoryDraft,
            typeService, options);
        return resolveParentReference(draftWithResolvedCustomTypeReference, categoryService, options);
    }

    /**
     * Given a {@link CategoryDraft} this method attempts to resolve the custom type reference. A {@link TypeService} is
     * also passed to the method in order to be able to fetch the actual type id from the CTP project and replace it in
     * the reference id field on the draft. The {@link CategorySyncOptions} instance is passed to infer whether it's
     * allowed to accept keys in UUID format or not, If not, a {@link ReferenceResolutionException} will be thrown.
     *
     * <b>Note</b> This method will block to fetch the actual id from the CTP project on the first call. Later calls,
     * will utilise the service's cache to fetch the id.
     *
     * @param categoryDraft the category draft to resolve it's references.
     * @param typeService   the service with which the type id is fetched.
     * @param options       the options which specifies whether it's allowed to resolve keys in UUID format.
     * @return a new Category draft instance with resolved custom type references.
     * @throws ReferenceResolutionException in case an error occurs during reference resolution. Either due to fail to
     *                                      fetch the ids from the CTP project or either due to setting an invalid key
     *                                      for references.
     */
    private static CategoryDraft resolveCategoryCustomTypeReference(@Nonnull final CategoryDraft categoryDraft,
                                                                    @Nonnull final TypeService typeService,
                                                                    @Nonnull final CategorySyncOptions options)
        throws ReferenceResolutionException {
        CategoryDraftBuilder categoryDraftBuilder = CategoryDraftBuilder.of(categoryDraft);
        final CustomFieldsDraft custom = categoryDraft.getCustom();
        if (custom != null) {
            final String resolvedTypeId = getCustomTypeId(custom, typeService, options).orElse(null);
            if (isNotBlank(resolvedTypeId)) {
                categoryDraftBuilder = categoryDraftBuilder
                    .custom(CustomFieldsDraft.ofTypeIdAndJson(resolvedTypeId, custom.getFields()));
            }
        }
        return categoryDraftBuilder.build();
    }

    /**
     * Given a {@link CustomFieldsDraft} this method fetches the custom type reference id. A {@link TypeService} is
     * passed to the method in order to be able to fetch the actual type id from the CTP project.
     * The {@link CategorySyncOptions} instance is passed to infer whether it's allowed to accept keys in UUID format or
     * not, if not, a {@link ReferenceResolutionException} will be thrown.
     *
     * <b>Note</b> This method will block to fetch the actual id from the CTP project on the first call. Later calls,
     * will utilise the service's cache to fetch the id.
     *
     * @param customFieldsDraft the custom fields object to fetch it's type key.
     * @param typeService       the service with which the type id is fetched.
     * @param options           the options which specifies whether it's allowed to resolve keys in UUID format.
     * @return an optional which might contain the actual id of the custom type.
     * @throws ReferenceResolutionException in case an error occurs during reference resolution. Either due to fail to
     *                                      fetch the ids from the CTP project or either due to setting an invalid key
     *                                      for references.
     */
    private static Optional<String> getCustomTypeId(@Nonnull final CustomFieldsDraft customFieldsDraft,
                                                    @Nonnull final TypeService typeService,
                                                    @Nonnull final BaseSyncOptions options)
        throws ReferenceResolutionException {
        try {
            final String customTypeKey = getKeyOnResourceIdentifierIdField(customFieldsDraft.getType(), options);
            return typeService.fetchCachedTypeId(customTypeKey).toCompletableFuture().get();
        } catch (ReferenceResolutionException | ExecutionException | InterruptedException exception) {
            throw new ReferenceResolutionException(buildErrorMessage(FAILED_TO_RESOLVE_CUSTOM_TYPE, exception),
                exception);
        }
    }

    /**
     * Given a {@link String} {@code customMessage} and {@link Throwable} exception, this method appends the exception's
     * message, if it exists, as a reason after the {@code customMessage} {@link String} and returns the result.
     *
     * @param customMessage the main message, which would always be at the beginning of the resultant string.
     * @param cause         the exception from which it's message, if it exists, will be appended to the customMessage.
     * @return the complete error message in the form "{customMessage}. Reason: {cause.getMessage()}"
     */
    private static String buildErrorMessage(@Nonnull final String customMessage, @Nonnull final Throwable cause) {
        final String causeMessage = cause.getMessage();
        final String reason = isNotBlank(causeMessage) ? format("Reason: %s", causeMessage) : "";
        return isNotBlank(reason) ? format(customMessage + " %s", reason) : customMessage;
    }

    /**
     * Given a {@link CategoryDraft} this method attempts to resolve the parent category reference. A
     * {@link CategoryService} is also passed to the method in order to be able to fetch the actual parent category id
     * from the CTP project and replace it in the reference id field on the draft. The {@link CategorySyncOptions}
     * instance is passed to infer whether it's allowed to accept keys in UUID format or not, if not a,
     * {@link ReferenceResolutionException} will be thrown.
     *
     * <b>Note</b> This method will block to fetch the actual id from the CTP project on the first call. Later calls,
     * will utilise the service's cache to fetch the id.
     *
     * @param categoryDraft   the category draft to resolve it's references.
     * @param categoryService the service with which the parent category id is fetched.
     * @param options         the options which specifies whether it's allowed to resolve keys in UUID format.
     * @return a new Category draft instance with resolved parent category references.
     * @throws ReferenceResolutionException in case an error occurs during reference resolution. Either due to fail to
     *                                      fetch the ids from the CTP project or either due to setting an invalid key
     *                                      for references.
     */
    private static CategoryDraft resolveParentReference(@Nonnull final CategoryDraft categoryDraft,
                                                        @Nonnull final CategoryService categoryService,
                                                        @Nonnull final CategorySyncOptions options)
        throws ReferenceResolutionException {
        CategoryDraftBuilder categoryDraftBuilder = CategoryDraftBuilder.of(categoryDraft);
        final Reference<Category> parentCategoryReference = categoryDraft.getParent();
        if (parentCategoryReference != null) {
            try {
                final String parentCategoryExternalId = getKeyOnResourceIdentifierIdField(parentCategoryReference,
                    options);
                final String resolvedParentId = categoryService.fetchCachedCategoryId(parentCategoryExternalId)
                                                               .toCompletableFuture().get().orElse(null);
                categoryDraftBuilder = categoryDraftBuilder
                    .parent(Reference.of(parentCategoryReference.getTypeId(), resolvedParentId));
            } catch (ReferenceResolutionException | ExecutionException | InterruptedException exception) {
                throw new ReferenceResolutionException(buildErrorMessage(FAILED_TO_RESOLVE_PARENT, exception),
                    exception);
            }
        }
        return categoryDraftBuilder.build();
    }

    /**
     * This method fetches the id value on the passed {@link ResourceIdentifier}, if valid. If it is not valid, a
     * {@link ReferenceResolutionException} will be thrown. The validity checks are:
     * <ol>
     * <li>Checks if the id value has a UUID format and the {@link BaseSyncOptions} instance has the
     * {@code allowUuid} flag set to true, or the id value doesn't have a UUID format.</li>
     * <li>Checks if the id value is not null or not empty.</li>
     * </ol>
     *
     * If the above checks pass, the id value is returned. Otherwise a {@link ReferenceResolutionException} is thrown.
     *
     * @param resourceIdentifier the reference from which the id value is validated and returned.
     * @param options            specifies whether it's allowed to resolve keys in UUID format.
     * @return the id value on the {@link ResourceIdentifier}
     * @throws ReferenceResolutionException if any of the validation checks fail.
     */
    private static String getKeyOnResourceIdentifierIdField(@Nonnull final ResourceIdentifier resourceIdentifier,
                                                            @Nonnull final BaseSyncOptions options)
        throws ReferenceResolutionException {
        final String resourceIdentifierId = resourceIdentifier.getId();
        if (isNotBlank(resourceIdentifierId)) {
            if (!options.isUuidAllowed() && isUuid(resourceIdentifierId)) {
                throw new ReferenceResolutionException(UUID_NOT_ALLOWED);
            } else {
                return resourceIdentifierId;
            }
        } else {
            throw new ReferenceResolutionException(UNSET_ID_FIELD);
        }
    }

    /**
     * Given an id as {@link String}, this method checks whether it is in UUID format or not.
     *
     * @param id to check if it has UUID format.
     * @return true if it has UUID format, otherwise false.
     */
    private static boolean isUuid(@Nonnull final String id) {
        final String uuidRegex = "[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}";
        final Pattern regexPattern = Pattern
            .compile(uuidRegex);
        return regexPattern.matcher(id).matches();
    }
}
