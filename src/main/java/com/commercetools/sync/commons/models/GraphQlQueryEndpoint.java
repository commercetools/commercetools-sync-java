package com.commercetools.sync.commons.models;

public enum GraphQlQueryEndpoint {
    CATEGORIES("categories"),
    CHANNELS("channels"),
    CUSTOMER_GROUPS("customerGroups"),
    CUSTOMERS("customers"),
    PRODUCTS("products"),
    PRODUCT_TYPES("productTypes"),
    STATES("states"),
    TAX_CATEGORIES("taxCategories"),
    TYPES("types");

    private final String name;

    GraphQlQueryEndpoint(final String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
