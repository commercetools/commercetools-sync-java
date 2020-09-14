package com.commercetools.sync.commons.helpers;


import com.commercetools.sync.commons.BaseSyncOptions;
import com.commercetools.sync.commons.exceptions.ReferenceResolutionException;
import io.sphere.sdk.models.Reference;
import io.sphere.sdk.models.ResourceIdentifier;

import javax.annotation.Nonnull;
import java.util.concurrent.CompletionStage;

import static org.apache.commons.lang3.StringUtils.isBlank;

/**
 * This class is responsible for providing an abstract implementation of reference resolution on different CTP
 * resources. For concrete reference resolution implementation of the CTP resources, this class should be extended.
 *
 * @param <S> a subclass implementation of {@link BaseSyncOptions} that is used to allow/deny some specific options,
 *            specified by the user, on reference resolution.
 */
public abstract class BaseReferenceResolver<T, S extends BaseSyncOptions> {
    public static final String BLANK_KEY_VALUE_ON_RESOURCE_IDENTIFIER = "The value of the 'key' field of the "
        + "Resource Identifier is blank (null/empty). Expecting the key of the referenced resource.";
    public static final String BLANK_ID_VALUE_ON_REFERENCE = "The value of the 'id' field of the Reference"
        + " is blank (null/empty). Expecting the key of the referenced resource.";
    protected S options;

    protected BaseReferenceResolver(@Nonnull final S options) {
        this.options = options;
    }

    /**
     * Given a draft this method attempts to resolve the all the references on the draft to
     * return a {@link CompletionStage} which contains a new instance of the draft with the resolved
     * references.
     *
     * @param draft the productDraft to resolve it's references.
     * @return a {@link CompletionStage} that contains as a result a new draft instance with resolved references
     *          or, in case an error occurs during reference resolution, a {@link ReferenceResolutionException}.
     */
    public abstract CompletionStage<T> resolveReferences(@Nonnull T draft);

    /**
     * This method gets the key value on the passed {@link Reference} from the id field, if valid. If it is not valid, a
     * {@link ReferenceResolutionException} will be thrown. The validity checks are:
     * <ul>
     * <li>Checks if the id value is not null or not empty.</li>
     * </ul>
     * If the above checks pass, the key value is returned. Otherwise a {@link ReferenceResolutionException} is thrown.
     *
     * @param reference the reference from which the key value is validated and returned.
     * @return the Id value on the {@link ResourceIdentifier}
     * @throws ReferenceResolutionException if any of the validation checks fail.
     */
    @Nonnull
    protected static <T> String getIdFromReference(@Nonnull final Reference<T> reference)
        throws ReferenceResolutionException {

        final String id = reference.getId();
        if (isBlank(id)) {
            throw new ReferenceResolutionException(BLANK_ID_VALUE_ON_REFERENCE);
        }
        return id;
    }

    /**
     * This method gets the key value on the passed {@link ResourceIdentifier}, if valid. If it is not valid, a
     * {@link ReferenceResolutionException} will be thrown. The validity checks are:
     * <ul>
     * <li>Checks if the key value is not null or not empty.</li>
     * </ul>
     * If the above checks pass, the key value is returned. Otherwise a {@link ReferenceResolutionException} is thrown.
     *
     * @param resourceIdentifier the resource identifier from which the key value is validated and returned.
     * @return the key value on the {@link ResourceIdentifier}
     * @throws ReferenceResolutionException if any of the validation checks fail.
     */
    @Nonnull
    protected static <T> String getKeyFromResourceIdentifier(@Nonnull final ResourceIdentifier<T> resourceIdentifier)
        throws ReferenceResolutionException {

        final String key = resourceIdentifier.getKey();
        if (isBlank(key)) {
            throw new ReferenceResolutionException(BLANK_KEY_VALUE_ON_RESOURCE_IDENTIFIER);
        }
        return key;
    }
}
