package com.commercetools.sync.commons.helpers;


import com.commercetools.sync.commons.BaseSyncOptions;
import com.commercetools.sync.commons.exceptions.ReferenceResolutionException;
import com.commercetools.sync.services.TypeService;
import io.sphere.sdk.categories.CategoryDraft;
import io.sphere.sdk.models.Reference;
import io.sphere.sdk.models.ResourceIdentifier;
import io.sphere.sdk.types.CustomFieldsDraft;
import io.sphere.sdk.utils.CompletableFutureUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.regex.Pattern;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

/**
 * This class is responsible for providing an abstract implementation of reference resolution on different CTP
 * resources. For concrete reference resolution implementation of the CTP resources, this class should be extended.
 * It also provides utility methods that facilitate the reference resolution process.
 *
 * @param <T> the resource draft type for which reference resolution has to be done on.
 * @param <S> a subclass implementation of {@link BaseSyncOptions} that is used to allow/deny some specific options,
 *            specified by the user, on reference resolution.
 */
public abstract class BaseReferenceResolver<T, S extends BaseSyncOptions> {
    private static final String FAILED_TO_RESOLVE_CUSTOM_TYPE = "Failed to resolve custom type reference.";
    private static final String UUID_NOT_ALLOWED = "Found a UUID in the id field. Expecting a key without a UUID value."
        + " If you want to allow UUID values for reference keys, please use the setAllowUuid(true) option in the sync"
        + " options.";
    private static final String UNSET_ID_FIELD = "Reference 'id' field value is blank (null/empty).";
    private static final String KEY_NOT_SET_ON_EXPANSION_OR_ID_FIELD = "Key is blank (null/empty) on both expanded"
        + " reference object and reference id field.";

    private TypeService typeService;
    private S options;

    protected BaseReferenceResolver(@Nonnull final S options, @Nonnull final TypeService typeService) {
        this.options = options;
        this.typeService = typeService;
    }

    /**
     * Returns the {@link S}, which is a subclass of {@link BaseSyncOptions}, instance of {@code this} instance.
     *
     *@return  the {@link S}, which is a subclass of {@link BaseSyncOptions}, instance of {@code this} instance.
     */
    protected S getOptions() {
        return options;
    }

    /**
     * Given a draft of {@link T} (e.g. {@link CategoryDraft}) this method attempts to resolve it's custom type
     * reference to return {@link CompletionStage} which contains a new instance of the draft with the resolved
     * custom type reference. The key of the custom type is taken from the from the id field of the reference.
     *
     * <p>The method then tries to fetch the key of the custom type, optimistically from a
     * cache. If the key is is not found, the resultant draft would remain exactly the same as the passed
     * draft (without a custom type reference resolution).
     *
     * @param draft the draft to resolve it's references.
     * @return a {@link CompletionStage} that contains as a result a new draft instance with resolved custom
     *      type references or, in case an error occurs during reference resolution,
     *      a {@link ReferenceResolutionException}.
     */
    protected abstract CompletionStage<T> resolveCustomTypeReference(@Nonnull final T draft);

    /**
     * Given a {@link CustomFieldsDraft} this method fetches the custom type reference id.
     *
     * @param customFieldsDraft the custom fields object to fetch it's type key.
     * @return a {@link CompletionStage} that contains as a result a new Category draft instance with resolved custom
     *      type references or, in case an error occurs during reference resolution,
     *      a {@link ReferenceResolutionException}.
     */
    protected CompletionStage<Optional<String>> getCustomTypeId(@Nonnull final CustomFieldsDraft customFieldsDraft) {
        try {
            final String customTypeKey = getKeyFromResourceIdentifier(customFieldsDraft.getType());
            return typeService.fetchCachedTypeId(customTypeKey);
        } catch (ReferenceResolutionException exception) {
            return CompletableFutureUtils.exceptionallyCompletedFuture(exception);
        }
    }

    /**
     * This method fetches the id value on the passed {@link ResourceIdentifier}, if valid. If it is not valid, a
     * {@link ReferenceResolutionException} will be thrown. The validity checks are:
     * <ol>
     * <li>Checks if the id value has a UUID format and the {@link BaseSyncOptions} instance has the
     * {@code allowUuid} flag set to true, or the id value doesn't have a UUID format.</li>
     * <li>Checks if the id value is not null or not empty.</li>
     * </ol>
     * If the above checks pass, the id value is returned. Otherwise a {@link ReferenceResolutionException} is thrown.
     *
     * @param resourceIdentifier the reference from which the id value is validated and returned.
     * @return the id value on the {@link ResourceIdentifier}
     * @throws ReferenceResolutionException if any of the validation checks fail.
     */
    private String getKeyFromResourceIdentifier(@Nonnull final ResourceIdentifier resourceIdentifier)
        throws ReferenceResolutionException {
        final String key = resourceIdentifier.getId();
        validateKey(key, UNSET_ID_FIELD);
        return key;
    }

    /**
     * Given a key value ({@code keyFromExpansion}) which is potentially fetched from the expansion of a reference. This
     * method checks if this value is not blank, if it is, it tries to fetch the key value from the id field on the
     * reference. If it's not blank it returns it. Whether the value is fetched from the expansion or from the
     * reference's id field, this method makes sure the key is valid:
     * <ol>
     *     <li>is not blank (empty/null).</li>
     *     <li>can only have a UUID format if allowed by the {@code options} instance of this class.</li>
     * </ol>
     * If the key is not valid a {@link ReferenceResolutionException} will be thrown.
     *
     *
     * @param keyFromExpansion the key value fetched after expansion of the {@code reference}.
     * @param reference the reference object to get the key from.
     * @return the key of the referenced object.
     * @throws ReferenceResolutionException thrown if the key is not valid.
     */
    @Nonnull
    protected String getKeyFromExpansionOrReference(@Nullable final String keyFromExpansion,
                                                    @Nonnull final Reference reference)
        throws ReferenceResolutionException {
        final String key = isBlank(keyFromExpansion) ? getKeyFromReference(reference).orElse(null) :
            keyFromExpansion;
        validateKey(key, KEY_NOT_SET_ON_EXPANSION_OR_ID_FIELD);
        return key;
    }

    /**
     * Helper method that returns an {@link Optional} containing the value of the id field on the passed
     * {@link Reference} object.
     *
     * @param reference the reference to get the value of it's id field.
     * @return an optional containing the id field value of the passed reference.
     */
    @Nonnull
    private static Optional<String> getKeyFromReference(@Nonnull final Reference reference) {
        return Optional.ofNullable(reference.getId());
    }

    /**
     * Helper method that validates the given {@code key}:
     * <ul>
     * <li>if the key is blank (empty/null), the method throws a {@link ReferenceResolutionException}.</li>
     * <li>if the key is in UUID format and this is not allowed in the {@code options} instance of
     * {@code this} class, the method throws a {@link ReferenceResolutionException}. If it's allowed
     * to have UUID keys, the key is returned.</li>
     * </ul>
     *
     * @param key          the key to validate.
     * @param errorMessage the error message to pass to the {@link ReferenceResolutionException} that will be thrown
     *                     if the key is blank (null/empty).
     * @throws ReferenceResolutionException thrown if the key is not valid.
     */
    private void validateKey(@Nullable final String key, @Nonnull final String errorMessage)
        throws ReferenceResolutionException {
        if (isBlank(key)) {
            throw new ReferenceResolutionException(errorMessage);
        } else {
            if (!options.isUuidAllowed() && isUuid(key)) {
                throw new ReferenceResolutionException(UUID_NOT_ALLOWED);
            }
        }
    }

    /**
     * Given an id as {@link String}, this method checks whether if it is in UUID format or not.
     *
     * @param id to check if it is in UUID format.
     * @return true if it is in UUID format, otherwise false.
     */
    private static boolean isUuid(@Nonnull final String id) {
        final String uuidRegex = "[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}";
        final Pattern regexPattern = Pattern
            .compile(uuidRegex);
        return regexPattern.matcher(id).matches();
    }

    /**
     * Helper method to check if passed {@link Reference} instance is expanded.
     *
     * @return true if the {@link Reference} is expanded.
     */
    protected static boolean isReferenceExpanded(@Nonnull final Reference reference) {
        return reference.getObj() != null;
    }

    /**
     * Given a {@link String} {@code customMessage} and {@link Throwable} exception, this method appends the exception's
     * message, if it exists, as a reason after the {@code customMessage} {@link String} and returns the result.
     *
     * @param customMessage the main message, which would always be at the beginning of the resultant string.
     * @param cause         the exception from which it's message, if it exists, will be appended to the customMessage.
     * @return the complete error message in the form "{customMessage}. Reason: {cause.getMessage()}"
     */
    @Nonnull
    protected static String buildErrorMessage(@Nonnull final String customMessage, @Nonnull final Throwable cause) {
        final String causeMessage = cause.getMessage();
        final String reason = isNotBlank(causeMessage) ? format("Reason: %s", causeMessage) : "";
        return isNotBlank(reason) ? format(customMessage + " %s", reason) : customMessage;
    }
}
