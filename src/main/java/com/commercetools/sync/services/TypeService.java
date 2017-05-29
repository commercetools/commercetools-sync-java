package com.commercetools.sync.services;


import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

public interface TypeService {
    @Nonnull
    CompletionStage<Optional<String>> fetchCachedTypeId(@Nullable final String key);
}
