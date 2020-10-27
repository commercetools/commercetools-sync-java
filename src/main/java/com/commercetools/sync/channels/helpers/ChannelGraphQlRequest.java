package com.commercetools.sync.channels.helpers;

import com.commercetools.sync.commons.helpers.BaseGraphQlRequest;

import javax.annotation.Nonnull;
import java.util.Set;

public class ChannelGraphQlRequest extends BaseGraphQlRequest<ChannelGraphQlRequest, ChannelGraphQlResult> {

    private static final String ENDPOINT = "channels";

    public ChannelGraphQlRequest(@Nonnull final Set<String> keysToSearch) {
        super(keysToSearch, ENDPOINT, ChannelGraphQlResult.class);
    }

    /**
     * Returns an instance of this class to be used in the superclass's generic methods. Please see the JavaDoc in the
     * overridden method for further details.
     *
     * @return an instance of this class.
     */
    @Override
    protected ChannelGraphQlRequest getThis() {
        return this;
    }
}
