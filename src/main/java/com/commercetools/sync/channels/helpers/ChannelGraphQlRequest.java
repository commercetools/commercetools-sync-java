package com.commercetools.sync.channels.helpers;

import com.commercetools.sync.commons.helpers.BaseGraphQlRequest;

import javax.annotation.Nonnull;
import java.util.Set;

public class ChannelGraphQlRequest extends BaseGraphQlRequest<ChannelGraphQlResult> {

    private static final String ENDPOINT = "channels";
    private static final long LIMIT = 10;

    public ChannelGraphQlRequest(@Nonnull final Set<String> keysToSearch) {
        super(keysToSearch, ENDPOINT, ChannelGraphQlResult.class, LIMIT);
    }
}
