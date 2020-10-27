package com.commercetools.sync.categories.helpers;

import com.commercetools.sync.commons.helpers.BaseGraphQlRequest;

import javax.annotation.Nonnull;
import java.util.Set;

public class CategoryGraphQlRequest extends BaseGraphQlRequest<CategoryGraphQlRequest, CategoryGraphQlResult> {

    private static final String ENDPOINT = "categories";

    public CategoryGraphQlRequest(@Nonnull final Set<String> keysToSearch) {
        super(keysToSearch, ENDPOINT, CategoryGraphQlResult.class);
    }

    /**
     * Returns an instance of this class to be used in the superclass's generic methods. Please see the JavaDoc in the
     * overridden method for further details.
     *
     * @return an instance of this class.
     */
    @Override
    protected CategoryGraphQlRequest getThis() {
        return this;
    }
}