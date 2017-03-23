package com.commercetools.sync.services;


import javax.annotation.Nullable;

public interface TypeService {

    @Nullable
    String getCachedTypeKeyById(@Nullable final String id);
}
