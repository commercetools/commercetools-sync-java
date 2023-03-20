package com.commercetools.sync.sdk2.products.utils;

import com.commercetools.api.models.category.CategoryReference;
import com.commercetools.api.models.common.LocalizedString;
import com.commercetools.api.models.product.CategoryOrderHints;
import com.commercetools.api.models.product.Product;
import com.commercetools.api.models.product.ProductData;
import com.commercetools.api.models.product.ProductPriceModeEnum;
import com.commercetools.api.models.product.ProductProjection;
import com.commercetools.api.models.product.ProductVariant;
import com.commercetools.api.models.product.SearchKeywords;
import com.commercetools.api.models.product_type.ProductTypeReference;
import com.commercetools.api.models.review.ReviewRatingStatistics;
import com.commercetools.api.models.state.StateReference;
import com.commercetools.api.models.tax_category.TaxCategoryReference;
import java.time.ZonedDateTime;
import java.util.List;
import javax.annotation.Nullable;

class ProductToProductProjectionWrapper implements ProductProjection {
  private final Product product;
  private final ProductData productData;

  ProductToProductProjectionWrapper(final Product product, final boolean staged) {
    this.product = product;
    this.productData =
        staged ? product.getMasterData().getStaged() : product.getMasterData().getCurrent();
  }

  @Override
  public Boolean getHasStagedChanges() {
    return product.getMasterData().getHasStagedChanges();
  }

  @Override
  public String getId() {
    return product.getId();
  }

  @Override
  public LocalizedString getName() {
    return productData.getName();
  }

  @Override
  public List<CategoryReference> getCategories() {
    return productData.getCategories();
  }

  @Override
  @Nullable
  public LocalizedString getDescription() {
    return productData.getDescription();
  }

  @Override
  public LocalizedString getSlug() {
    return productData.getSlug();
  }

  @Override
  @Nullable
  public LocalizedString getMetaTitle() {
    return productData.getMetaTitle();
  }

  @Override
  @Nullable
  public LocalizedString getMetaDescription() {
    return productData.getMetaDescription();
  }

  @Override
  @Nullable
  public LocalizedString getMetaKeywords() {
    return productData.getMetaKeywords();
  }

  @Override
  public ProductVariant getMasterVariant() {
    return productData.getMasterVariant();
  }

  @Override
  public List<ProductVariant> getVariants() {
    return productData.getVariants();
  }

  @Override
  public ProductTypeReference getProductType() {
    return product.getProductType();
  }

  @Override
  @Nullable
  public TaxCategoryReference getTaxCategory() {
    return product.getTaxCategory();
  }

  @Override
  public ZonedDateTime getCreatedAt() {
    return product.getCreatedAt();
  }

  @Override
  public ZonedDateTime getLastModifiedAt() {
    return product.getLastModifiedAt();
  }

  @Override
  public Long getVersion() {
    return product.getVersion();
  }

  @Override
  public Boolean getPublished() {
    return product.getMasterData().getPublished();
  }

  @Override
  public SearchKeywords getSearchKeywords() {
    return productData.getSearchKeywords();
  }

  @Override
  @Nullable
  public CategoryOrderHints getCategoryOrderHints() {
    return productData.getCategoryOrderHints();
  }

  @Override
  @Nullable
  public ReviewRatingStatistics getReviewRatingStatistics() {
    return product.getReviewRatingStatistics();
  }

  @Override
  @Nullable
  public StateReference getState() {
    return product.getState();
  }

  @Override
  @Nullable
  public String getKey() {
    return product.getKey();
  }

  @Override
  public ProductPriceModeEnum getPriceMode() {
    return product.getPriceMode();
  }

  @Override
  public void setId(String id) {}

  @Override
  public void setVersion(Long version) {}

  @Override
  public void setKey(String key) {}

  @Override
  public void setCreatedAt(ZonedDateTime createdAt) {}

  @Override
  public void setLastModifiedAt(ZonedDateTime lastModifiedAt) {}

  @Override
  public void setProductType(ProductTypeReference productType) {}

  @Override
  public void setName(LocalizedString name) {}

  @Override
  public void setDescription(LocalizedString description) {}

  @Override
  public void setSlug(LocalizedString slug) {}

  @Override
  public void setCategories(CategoryReference... categories) {}

  @Override
  public void setCategories(List<CategoryReference> categories) {}

  @Override
  public void setCategoryOrderHints(CategoryOrderHints categoryOrderHints) {}

  @Override
  public void setMetaTitle(LocalizedString metaTitle) {}

  @Override
  public void setMetaDescription(LocalizedString metaDescription) {}

  @Override
  public void setMetaKeywords(LocalizedString metaKeywords) {}

  @Override
  public void setSearchKeywords(SearchKeywords searchKeywords) {}

  @Override
  public void setHasStagedChanges(Boolean hasStagedChanges) {}

  @Override
  public void setPublished(Boolean published) {}

  @Override
  public void setMasterVariant(ProductVariant masterVariant) {}

  @Override
  public void setVariants(ProductVariant... variants) {}

  @Override
  public void setVariants(List<ProductVariant> variants) {}

  @Override
  public void setTaxCategory(TaxCategoryReference taxCategory) {}

  @Override
  public void setState(StateReference state) {}

  @Override
  public void setReviewRatingStatistics(ReviewRatingStatistics reviewRatingStatistics) {}

  @Override
  public void setPriceMode(ProductPriceModeEnum priceMode) {}
}
