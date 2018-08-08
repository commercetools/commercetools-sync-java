package com.commercetools.sync.commons.helpers;


import com.commercetools.sync.commons.BaseSyncOptions;
import com.commercetools.sync.commons.exceptions.ReferenceResolutionException;
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
        + " If you want to allow UUID values for reference keys, please use the allowUuidKeys(true) option in the"
        + " sync options.";
    public static final String BLANK_ID_VALUE_ON_RESOURCE_IDENTIFIER = "The value of the 'id' field of the Resource"
        + " Identifier/Reference is blank (null/empty).";
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
     * <p>Note: Currently the key is expected to be set on the id field of the {@link ResourceIdentifier}. This is in
     * order to make key setting consistent on both {@link io.sphere.sdk.models.Reference} and
     * {@link ResourceIdentifier}. However, this will be changed when GITHUB ISSUE#138 is resolved.
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
        validateKey(key, allowUuidKeys, BLANK_ID_VALUE_ON_RESOURCE_IDENTIFIER);
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
    private static void validateKey(@Nullable final String key, final boolean shouldAllowUuidKeys,
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
}
