package com.commercetools.sync.commons.helpers;

import static org.apache.commons.lang3.StringUtils.isBlank;

import com.commercetools.api.models.common.Reference;
import com.commercetools.api.models.common.ResourceIdentifier;
import com.commercetools.sync.commons.BaseSyncOptions;
import com.commercetools.sync.commons.exceptions.ReferenceResolutionException;
import com.commercetools.sync.services.impl.BaseTransformServiceImpl;
import io.vrap.rmf.base.client.Draft;
import java.util.concurrent.CompletionStage;
import javax.annotation.Nonnull;

/**
 * This class is responsible for providing an abstract implementation of reference resolution on
 * different CTP resources. For concrete reference resolution implementation of the CTP resources,
 * this class should be extended.
 *
 * @param <SyncOptionsT> a subclass implementation of {@link
 *     com.commercetools.sync.commons.BaseSyncOptions} that is used to allow/deny some specific
 *     options, specified by the user, on reference resolution.
 */
public abstract class BaseReferenceResolver<
    ResourceDraftT extends Draft<ResourceDraftT>, SyncOptionsT extends BaseSyncOptions> {
  public static final String BLANK_KEY_VALUE_ON_RESOURCE_IDENTIFIER =
      "The value of the 'key' field of the "
          + "Resource Identifier is blank (null/empty). Expecting the key of the referenced resource.";
  public static final String BLANK_ID_VALUE_ON_REFERENCE =
      "The value of the 'id' field of the Reference"
          + " is blank (null/empty). Expecting the key of the referenced resource.";
  public static final String SELF_REFERENCING_ID_PLACE_HOLDER = "SELF_REFERENCE_ID";
  protected SyncOptionsT options;

  protected BaseReferenceResolver(@Nonnull final SyncOptionsT options) {
    this.options = options;
  }

  /**
   * Given a draft this method attempts to resolve the all the references on the draft to return a
   * {@link CompletionStage} which contains a new instance of the draft with the resolved
   * references.
   *
   * @param draft the productDraft to resolve it's references.
   * @return a {@link CompletionStage} that contains as a result a new draft instance with resolved
   *     references or, in case an error occurs during reference resolution, a {@link
   *     com.commercetools.sync.commons.exceptions.ReferenceResolutionException}.
   */
  public abstract CompletionStage<ResourceDraftT> resolveReferences(@Nonnull ResourceDraftT draft);

  /**
   * This method gets the key value on the passed {@link Reference} from the id field, if valid. If
   * it is not valid, a {@link
   * com.commercetools.sync.commons.exceptions.ReferenceResolutionException} will be thrown. The
   * validity checks are:
   *
   * <ul>
   *   <li>Checks if the id value is not null or not empty.
   * </ul>
   *
   * If the above checks pass, the key value is returned. Otherwise a {@link
   * com.commercetools.sync.commons.exceptions.ReferenceResolutionException} is thrown.
   *
   * @param reference the reference from which the key value is validated and returned.
   * @param <T> the type of the reference.
   * @return the Id value on the {@link ResourceIdentifier}
   * @throws com.commercetools.sync.commons.exceptions.ReferenceResolutionException if any of the
   *     validation checks fail.
   */
  @Nonnull
  protected static <T> String getIdFromReference(@Nonnull final Reference reference)
      throws ReferenceResolutionException {

    final String id = reference.getId();
    if (isBlank(id)) {
      throw new ReferenceResolutionException(BLANK_ID_VALUE_ON_REFERENCE);
    }
    return id;
  }

  /**
   * This method gets the key value on the passed {@link ResourceIdentifier}, if valid. If it is not
   * valid, a {@link ReferenceResolutionException} will be thrown. The validity checks are:
   *
   * <ul>
   *   <li>Checks if the key value is not null or not empty.
   *   <li>Checks if the key value is not equal to KEY_IS_NOT_SET(fetched from cache).
   * </ul>
   *
   * If the above checks pass, the key value is returned. Otherwise a {@link
   * ReferenceResolutionException} is thrown.
   *
   * @param resourceIdentifier the resource identifier from which the key value is validated and
   *     returned.
   * @return the key value on the {@link ResourceIdentifier}
   * @throws ReferenceResolutionException if any of the validation checks fail.
   */
  @Nonnull
  protected static String getKeyFromResourceIdentifier(
      @Nonnull final ResourceIdentifier resourceIdentifier) throws ReferenceResolutionException {

    final String key = resourceIdentifier.getKey();
    if (isBlank(key) || BaseTransformServiceImpl.KEY_IS_NOT_SET_PLACE_HOLDER.equals(key)) {
      throw new ReferenceResolutionException(BLANK_KEY_VALUE_ON_RESOURCE_IDENTIFIER);
    }
    return key;
  }
}
