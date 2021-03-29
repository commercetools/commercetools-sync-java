package com.commercetools.sync.producttypes.service.impl;

import com.commercetools.sync.commons.exceptions.ReferenceReplacementException;
import com.commercetools.sync.producttypes.service.ProductTypeReferenceTransformService;
import com.commercetools.sync.producttypes.utils.ProductTypeReferenceResolutionUtils;
import com.commercetools.sync.services.impl.BaseTransformServiceImpl;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.producttypes.ProductType;
import io.sphere.sdk.producttypes.ProductTypeDraft;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nonnull;

public class ProductTypeReferenceTransformServiceImpl extends BaseTransformServiceImpl
    implements ProductTypeReferenceTransformService {

  private static final String FAILED_TO_REPLACE_REFERENCES_ON_ATTRIBUTES =
      "Failed to replace referenced resource ids with keys on the attributes of the productTypes in "
          + "the current fetched page from the source project. This page will not be synced to the target "
          + "project.";

  public ProductTypeReferenceTransformServiceImpl(
      @Nonnull final SphereClient ctpClient,
      @Nonnull final Map<String, String> referenceIdToKeyCache) {
    super(ctpClient, referenceIdToKeyCache);
  }

  @Nonnull
  @Override
  public CompletableFuture<List<ProductTypeDraft>> transformProductTypeReferences(
      @Nonnull final List<ProductType> productTypes) throws ReferenceReplacementException {

    // TODO: Collect all attributes with references (nestedAttributes, setAttributes with nested
    // type) and
    // check if the ids are already cached in referenceIdToKeyCache otherwise fetch the keys by ids
    // of the references
    // and store in cache. Then call the utility function to map productTypes to productTypeDrafts

    return CompletableFuture.completedFuture(
        ProductTypeReferenceResolutionUtils.mapToProductTypeDrafts(
            productTypes, referenceIdToKeyCache));
  }
}
