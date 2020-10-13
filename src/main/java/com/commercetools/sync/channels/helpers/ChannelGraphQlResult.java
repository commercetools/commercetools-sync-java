package com.commercetools.sync.channels.helpers;

import com.commercetools.sync.commons.helpers.BaseGraphQlResult;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ChannelGraphQlResult extends BaseGraphQlResult {

    @JsonCreator
    public ChannelGraphQlResult(@JsonProperty("channels") final BaseGraphQlResult channels) {
        super(channels.getResults());
    }
}
