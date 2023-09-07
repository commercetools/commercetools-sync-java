package com.commercetools.sync.products.utils.productvariantupdateactionutils.attributes;

import com.commercetools.api.models.product.Attribute;
import com.commercetools.sync.commons.utils.TestUtils;

public final class AttributeFixtures {

  public static final Attribute BOOLEAN_ATTRIBUTE_TRUE =
      TestUtils.readObjectFromResource("boolean-attribute-true.json", Attribute.class);
  public static final Attribute BOOLEAN_ATTRIBUTE_FALSE =
      TestUtils.readObjectFromResource("boolean-attribute-false.json", Attribute.class);
  public static final Attribute TEXT_ATTRIBUTE_BAR =
      TestUtils.readObjectFromResource("text-attribute-bar.json", Attribute.class);
  public static final Attribute TEXT_ATTRIBUTE_FOO =
      TestUtils.readObjectFromResource("text-attribute-foo.json", Attribute.class);
  public static final Attribute LTEXT_ATTRIBUTE_EN_BAR =
      TestUtils.readObjectFromResource("ltext-attribute-en-bar.json", Attribute.class);
  public static final Attribute ENUM_ATTRIBUTE_BARLABEL_BARKEY =
      TestUtils.readObjectFromResource("enum-attribute-barLabel-barKey.json", Attribute.class);
  public static final Attribute LENUM_ATTRIBUTE_EN_BAR =
      TestUtils.readObjectFromResource("lenum-attribute-en-bar.json", Attribute.class);
  public static final Attribute NUMBER_ATTRIBUTE_10 =
      TestUtils.readObjectFromResource("number-attribute-10.json", Attribute.class);
  public static final Attribute MONEY_ATTRIBUTE_EUR_2300 =
      TestUtils.readObjectFromResource("money-attribute-eur-2300.json", Attribute.class);
  public static final Attribute DATE_ATTRIBUTE_2017_11_09 =
      TestUtils.readObjectFromResource("date-attribute-2017-11-09.json", Attribute.class);
  public static final Attribute TIME_ATTRIBUTE_10_08_46 =
      TestUtils.readObjectFromResource("time-attribute-10-08-46.json", Attribute.class);
  public static final Attribute DATE_TIME_ATTRIBUTE_2016_05_20T01_02_46 =
      TestUtils.readObjectFromResource(
          "datetime-attribute-2016-05-20T01-02-46.json", Attribute.class);
  public static final Attribute PRODUCT_REFERENCE_ATTRIBUTE =
      TestUtils.readObjectFromResource("product-reference-attribute.json", Attribute.class);
  public static final Attribute CATEGORY_REFERENCE_ATTRIBUTE =
      TestUtils.readObjectFromResource("category-reference-attribute.json", Attribute.class);
  public static final Attribute LTEXT_SET_ATTRIBUTE =
      TestUtils.readObjectFromResource("ltext-set-attribute.json", Attribute.class);
  public static final Attribute PRODUCT_REFERENCE_SET_ATTRIBUTE =
      TestUtils.readObjectFromResource("product-reference-set-attribute.json", Attribute.class);
  public static final Attribute EMPTY_SET_ATTRIBUTE =
      TestUtils.readObjectFromResource("empty-set-attribute.json", Attribute.class);

  private AttributeFixtures() {}
}
