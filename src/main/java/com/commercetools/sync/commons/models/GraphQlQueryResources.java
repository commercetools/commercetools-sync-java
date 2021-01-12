package com.commercetools.sync.commons.models;

public enum GraphQlQueryResources {
  CATEGORIES("categories"),
  CHANNELS("channels"),
  CUSTOMER_GROUPS("customerGroups"),
  CUSTOMERS("customers"),
  PRODUCTS("products"),
  PRODUCT_TYPES("productTypes"),
  STATES("states"),
  TAX_CATEGORIES("taxCategories"),
  TYPES("typeDefinitions"),
  SHOPPING_LISTS("shoppingLists"),
  CUSTOM_OBJECTS("customObjects");

  private final String name;

  GraphQlQueryResources(final String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }
}
