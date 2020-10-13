package com.commercetools.sync.categories.helpers;

import com.commercetools.sync.commons.helpers.BaseGraphQlRequest;

import javax.annotation.Nonnull;
import java.util.Set;

public class CategoryGraphQlRequest extends BaseGraphQlRequest<CategoryGraphQlResult> {

    private static final String ENDPOINT = "categories";
    private static final long LIMIT = 10;

    public CategoryGraphQlRequest(@Nonnull final Set<String> keysToSearch) {
        super(keysToSearch, ENDPOINT, CategoryGraphQlResult.class, LIMIT);
    }
}