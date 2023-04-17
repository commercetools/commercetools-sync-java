package com.commercetools.sync.sdk2.products.utils.productvariantupdateactionutils.attributes;

import static com.commercetools.sync.sdk2.products.ProductSyncMockUtils.createObjectFromResource;

import com.commercetools.api.models.product.Attribute;

public final class AttributeFixtures {

  public static final Attribute BOOLEAN_ATTRIBUTE_TRUE =
      createObjectFromResource("boolean-attribute-true.json", Attribute.class);
  public static final Attribute BOOLEAN_ATTRIBUTE_FALSE =
      createObjectFromResource("boolean-attribute-false.json", Attribute.class);
  public static final Attribute TEXT_ATTRIBUTE_BAR =
      createObjectFromResource("text-attribute-bar.json", Attribute.class);
  public static final Attribute TEXT_ATTRIBUTE_FOO =
      createObjectFromResource("text-attribute-foo.json", Attribute.class);
  public static final Attribute LTEXT_ATTRIBUTE_EN_BAR =
      createObjectFromResource("ltext-attribute-en-bar.json", Attribute.class);
  public static final Attribute ENUM_ATTRIBUTE_BARLABEL_BARKEY =
      createObjectFromResource("enum-attribute-barLabel-barKey.json", Attribute.class);
  public static final Attribute LENUM_ATTRIBUTE_EN_BAR =
      createObjectFromResource("lenum-attribute-en-bar.json", Attribute.class);
  public static final Attribute NUMBER_ATTRIBUTE_10 =
      createObjectFromResource("number-attribute-10.json", Attribute.class);
  public static final Attribute MONEY_ATTRIBUTE_EUR_2300 =
      createObjectFromResource("money-attribute-eur-2300.json", Attribute.class);
  public static final Attribute DATE_ATTRIBUTE_2017_11_09 =
      createObjectFromResource("date-attribute-2017-11-09.json", Attribute.class);
  public static final Attribute TIME_ATTRIBUTE_10_08_46 =
      createObjectFromResource("time-attribute-10-08-46.json", Attribute.class);
  public static final Attribute DATE_TIME_ATTRIBUTE_2016_05_20T01_02_46 =
      createObjectFromResource("datetime-attribute-2016-05-20T01-02-46.json", Attribute.class);
  public static final Attribute PRODUCT_REFERENCE_ATTRIBUTE =
      createObjectFromResource("product-reference-attribute.json", Attribute.class);
  public static final Attribute CATEGORY_REFERENCE_ATTRIBUTE =
      createObjectFromResource("category-reference-attribute.json", Attribute.class);
  public static final Attribute LTEXT_SET_ATTRIBUTE =
      createObjectFromResource("ltext-set-attribute.json", Attribute.class);
  public static final Attribute PRODUCT_REFERENCE_SET_ATTRIBUTE =
      createObjectFromResource("product-reference-set-attribute.json", Attribute.class);
  public static final Attribute EMPTY_SET_ATTRIBUTE =
      createObjectFromResource("empty-set-attribute.json", Attribute.class);

  private AttributeFixtures() {}
}