package com.commercetools.sync.internals.helpers;

import com.neovisionaries.i18n.CountryCode;
import io.sphere.sdk.customobjects.CustomObject;
import io.sphere.sdk.customobjects.CustomObjectDraft;
import io.sphere.sdk.models.Reference;
import io.sphere.sdk.models.ResourceIdentifier;
import io.sphere.sdk.products.Price;
import io.sphere.sdk.products.PriceDraft;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.ZonedDateTime;
import java.util.Objects;

import static java.util.Optional.ofNullable;

/**
 * This class is only meant for the internal use of the commercetools-sync-java library.
 *
 * <p>Since there is no one unique identifier for customObjects on the commercetools API, we uniquely identify
 * a customObject as a composite id of the following fields: key, container. This class models such composite key.
 */
public final class CustomObjectCompositeId {

    private final String key;
    private final String container;

    private CustomObjectCompositeId(@Nonnull final String key,
                                    @Nonnull final String container) {
        this.key = key;
        this.container = container;
    }


    /**
     * Given a {@link CustomObjectDraft}, creates a {@link CustomObjectCompositeId} using the following fields from the
     * supplied {@link CustomObjectDraft}:
     * <ol>
     * <li>{@link CustomObjectCompositeId#key}: key of {@link CustomObjectDraft#getKey()} </li>
     * <li>{@link CustomObjectCompositeId#container}: container of {@link CustomObjectDraft#getContainer()}</li>

     * </ol>
     *
     * @param customObjectDraft a composite id is built using its fields.
     * @return a composite id comprised of the fields of the supplied {@link CustomObjectDraft}.
     */
    @Nonnull
    public static CustomObjectCompositeId of(@Nonnull final CustomObjectDraft customObjectDraft) {
        return new CustomObjectCompositeId(customObjectDraft.getKey(), customObjectDraft.getContainer());
    }

    /**
     * Given a {@link CustomObjectDraft}, creates a {@link CustomObjectCompositeId} using the following fields from the
     * supplied {@link CustomObject}:
     * <ol>
     * <li>{@link CustomObjectCompositeId#key}: key of {@link CustomObject#getKey()} </li>
     * <li>{@link CustomObjectCompositeId#container}: container of {@link CustomObject#getContainer()}</li>
     * </ol>
     *
     * @param customObject a composite id is built using its fields.
     * @return a composite id comprised of the fields of the supplied {@link CustomObject}.
     */
    @Nonnull
    public static CustomObjectCompositeId of(@Nonnull final CustomObject customObject) {
        return new CustomObjectCompositeId(customObject.getKey(), customObject.getContainer());
    }

    /**
     * Given a {@link String} and {@link String}, creates a {@link CustomObjectCompositeId} using the following fields from the
     * supplied attributes.
     * <ol>
     * <li>{@link CustomObjectCompositeId#key}: key of {@link CustomObject#getKey()} </li>
     * <li>{@link CustomObjectCompositeId#container}: container of {@link CustomObject#getContainer()}</li>
     * </ol>
     *
     * @param key key of CustomerObject to build composite Id.
     * @param container container of CustomerObject to build composite Id.
     * @return a composite id comprised of the fields of the supplied {@link CustomObject}.
     */
    @Nonnull
    public static CustomObjectCompositeId of(@Nonnull final String key, @Nonnull final String container) {
        return new CustomObjectCompositeId(key, container);
    }

    public String getKey() {
        return this.key;
    }

    public String getContainer() {
        return this.container;
    }

    @Override
    public boolean equals(final Object otherObject) {
        if (this == otherObject) {
            return true;
        }
        if (!(otherObject instanceof CustomObjectCompositeId)) {
            return false;
        }
        final CustomObjectCompositeId that = (CustomObjectCompositeId) otherObject;
        return  Objects.equals(key, that.key)
            && Objects.equals(container, that.container);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key, container);
    }
}
