package com.commercetools.sync.types.helpers;

import com.commercetools.sync.commons.helpers.BaseGraphQlRequest;

import javax.annotation.Nonnull;
import java.util.Set;

public class TypeGraphQlRequest extends BaseGraphQlRequest<TypeGraphQlResult> {

    private static final String ENDPOINT = "types";
    private static final long LIMIT = 10;

    public TypeGraphQlRequest(@Nonnull final Set<String> keysToSearch) {
        super(keysToSearch, ENDPOINT, TypeGraphQlResult.class, LIMIT);
    }
}