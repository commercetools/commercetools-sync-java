package com.commercetools.sync.commons.helpers;


import com.commercetools.sync.commons.BaseSyncOptions;
import com.commercetools.sync.commons.exceptions.ReferenceResolutionException;
import io.sphere.sdk.models.Reference;
import io.sphere.sdk.models.ResourceIdentifier;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.concurrent.CompletionStage;
import java.util.regex.Pattern;

import static org.apache.commons.lang3.StringUtils.isBlank;

/**
 * This class is responsible for providing an abstract implementation of reference resolution on different CTP
 * resources. For concrete reference resolution implementation of the CTP resources, this class should be extended.
 *
 * @param <S> a subclass implementation of {@link BaseSyncOptions} that is used to allow/deny some specific options,
 *            specified by the user, on reference resolution.
 */
public abstract class BaseReferenceResolver<T, S extends BaseSyncOptions> {
    private static final String UUID_NOT_ALLOWED = "Found a UUID in the id field. Expecting a key without a UUID value."
        + " If you want to allow UUID values for reference keys, please use the setAllowUuidKeys(true) option in the"
        + " sync options.";
    private static final String UNSET_ID_FIELD = "Reference 'id' field value is blank (null/empty).";
    private static final String KEY_NOT_SET_ON_EXPANSION_OR_ID_FIELD = "Key is blank (null/empty) on both expanded"
        + " reference object and reference id field.";
    private static final String UUID_REGEX = "[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}";

    protected S options;

    protected BaseReferenceResolver(@Nonnull final S options) {
        this.options = options;
    }

    /**
     * Given a draft this method attempts to resolve the all the references on the draft to
     * return a {@link CompletionStage} which contains a new instance of the draft with the resolved
     * references. The keys of the references are either taken from the expanded references or
     * taken from the id field of the references.
     *
     * @param draft the productDraft to resolve it's references.
     * @return a {@link CompletionStage} that contains as a result a new draft instance with resolved references
     *          or, in case an error occurs during reference resolution, a {@link ReferenceResolutionException}.
     */
    public abstract CompletionStage<T> resolveReferences(@Nonnull final T draft);

    /**
     * This method fetches the id value on the passed {@link ResourceIdentifier}, if valid. If it is not valid, a
     * {@link ReferenceResolutionException} will be thrown. The validity checks are:
     * <ol>
     * <li>Checks if the id value has a UUID format and the {@code allowUuidKeys} flag set to true, or the id value
     * doesn't have a UUID format.</li>
     * <li>Checks if the id value is not null or not empty.</li>
     * </ol>
     * If the above checks pass, the id value is returned. Otherwise a {@link ReferenceResolutionException} is thrown.
     *
     * @param resourceIdentifier the reference from which the id value is validated and returned.
     * @param allowUuidKeys flag that signals whether the key could be UUID format or not.
     * @return the id value on the {@link ResourceIdentifier}
     * @throws ReferenceResolutionException if any of the validation checks fail.
     */
    @Nonnull
    @SuppressWarnings("ConstantConditions") //To remove the warning on the key is null, because it can't be null.
    protected static String getKeyFromResourceIdentifier(@Nonnull final ResourceIdentifier resourceIdentifier,
                                                         final boolean allowUuidKeys)
        throws ReferenceResolutionException {
        final String key = resourceIdentifier.getId();
        validateKey(key, allowUuidKeys, UNSET_ID_FIELD);
        return key;
    }

    /**
     * Given a key value ({@code keyFromExpansion}) which is potentially fetched from the expansion of a reference. This
     * method checks if this value is not blank (empty string/null), if it is, it tries to fetch the key value from the
     * id field on the reference. If it's not blank it returns it. Whether the value is fetched from the expansion or
     * from the reference's id field, this method makes sure the key is valid:
     * <ol>
     *     <li>is not blank (empty/null).</li>
     *     <li>can only have a UUID format if allowed by the {@code options} instance of this class.</li>
     * </ol>
     * If the key is not valid a {@link ReferenceResolutionException} will be thrown.
     *
     *
     * @param allowUuidKeys flag that signals whether the key could be UUID format or not.
     * @param keyFromExpansion the key value fetched after expansion of the {@code reference}.
     * @param reference the reference object to get the key from.
     * @return the key of the referenced object.
     * @throws ReferenceResolutionException thrown if the key is not valid.
     */
    @Nonnull
    @SuppressWarnings("ConstantConditions") //To remove the warning on the key is null, because it can't be null.
    protected static String getKeyFromExpansionOrReference(final boolean allowUuidKeys,
                                                           @Nullable final String keyFromExpansion,
                                                           @Nonnull final Reference reference)
        throws ReferenceResolutionException {
        final String key = isBlank(keyFromExpansion) ? reference.getId() : keyFromExpansion;
        validateKey(key, allowUuidKeys, KEY_NOT_SET_ON_EXPANSION_OR_ID_FIELD);
        return key;
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
     * @param key                 the key to validate.
     * @param shouldAllowUuidKeys isUUID allowed
     * @param errorMessage        the error message to pass to the {@link ReferenceResolutionException} that will be
     *                            thrown if the key is blank (null/empty).
     * @throws ReferenceResolutionException thrown if the key is not valid.
     */
    private static void validateKey(@Nullable final String key,
                                    final boolean shouldAllowUuidKeys,
                                    @Nonnull final String errorMessage)
        throws ReferenceResolutionException {
        if (isBlank(key)) {
            throw new ReferenceResolutionException(errorMessage);
        } else {
            if (!shouldAllowUuidKeys && isUuid(key)) {
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
        final Pattern regexPattern = Pattern.compile(UUID_REGEX);
        return regexPattern.matcher(id).matches();
    }

    /**
     * Helper method to check if passed {@link Reference} instance is expanded.
     *
     * @param reference the reference to check.
     * @return true if the {@link Reference} is expanded.
     */
    protected static boolean isReferenceExpanded(@Nonnull final Reference reference) {
        return reference.getObj() != null;
    }
}
