package com.commercetools.sync.customobjects.helpers;

import io.sphere.sdk.customobjects.CustomObject;
import io.sphere.sdk.customobjects.CustomObjectDraft;

import javax.annotation.Nonnull;

import static java.lang.String.format;

/**
 * This class is only meant for the internal use of the commercetools-sync-java library.
 *
 * <p>Since there is no one unique identifier for customObjects on the commercetools API, we uniquely identify
 * a customObject as a composite id of the following fields: key, container. This class models such composite key.
 */
public final class CustomObjectCompositeIdentifier {

    private final String key;
    private final String container;

    private CustomObjectCompositeIdentifier(@Nonnull final String key,
                                    @Nonnull final String container) {
        this.key = key;
        this.container = container;
    }


    /**
     * Given a {@link CustomObjectDraft}, creates a {@link CustomObjectCompositeIdentifier} using the following fields
     * from the supplied {@link CustomObjectDraft}:
     * <ol>
     * <li>{@link CustomObjectCompositeIdentifier#key}: key of {@link CustomObjectDraft#getKey()} </li>
     * <li>{@link CustomObjectCompositeIdentifier#container}: container of {@link CustomObjectDraft#getContainer()}</li>

     * </ol>
     *
     * @param customObjectDraft a composite id is built using its fields.
     * @return a composite id comprised of the fields of the supplied {@link CustomObjectDraft}.
     */
    @Nonnull
    public static CustomObjectCompositeIdentifier of(@Nonnull final CustomObjectDraft customObjectDraft) {
        return new CustomObjectCompositeIdentifier(customObjectDraft.getKey(), customObjectDraft.getContainer());
    }

    /**
     * Given a {@link CustomObjectDraft}, creates a {@link CustomObjectCompositeIdentifier} using the following fields
     * from the supplied {@link CustomObject}:
     * <ol>
     * <li>{@link CustomObjectCompositeIdentifier#key}: key of {@link CustomObject#getKey()} </li>
     * <li>{@link CustomObjectCompositeIdentifier#container}: container of {@link CustomObject#getContainer()}</li>
     * </ol>
     *
     * @param customObject a composite id is built using its fields.
     * @return a composite id comprised of the fields of the supplied {@link CustomObject}.
     */
    @Nonnull
    public static CustomObjectCompositeIdentifier of(@Nonnull final CustomObject customObject) {
        return new CustomObjectCompositeIdentifier(customObject.getKey(), customObject.getContainer());
    }

    /**
     * Given a {@link String} and {@link String}, creates a {@link CustomObjectCompositeIdentifier} using the following
     * fields from the supplied attributes.
     *
     * @param key key of CustomerObject to build composite Id.
     * @param container container of CustomerObject to build composite Id.
     * @return a composite id comprised of the fields of the supplied {@code key} and {@code container}.
     */
    @Nonnull
    public static CustomObjectCompositeIdentifier of(@Nonnull final String key, @Nonnull final String container) {
        return new CustomObjectCompositeIdentifier(key, container);
    }

    public String getKey() {
        return this.key;
    }

    public String getContainer() {
        return this.container;
    }

    @Override
    public String toString() {
        return format("{key='%s', container='%s'}", key, container);
    }
}
