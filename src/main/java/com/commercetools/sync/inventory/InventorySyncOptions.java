package com.commercetools.sync.inventory;

import com.commercetools.sync.commons.BaseOptions;

import javax.annotation.Nonnull;

//TODO implement (GITHUB ISSUE #15)
//TODO document
public class InventorySyncOptions extends BaseOptions {

    public InventorySyncOptions(@Nonnull final String ctpProjectKey,
                                @Nonnull final String ctpClientId,
                                @Nonnull final String ctpClientSecret) {
        super(ctpProjectKey, ctpClientId, ctpClientSecret);
    }
}
