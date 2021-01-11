package com.commercetools.sync.customobjects.helpers;

import static java.lang.String.format;

import io.sphere.sdk.customobjects.CustomObject;
import io.sphere.sdk.customobjects.CustomObjectDraft;
import java.util.Objects;
import javax.annotation.Nonnull;

/**
 * This class is only meant for the internal use of the commercetools-sync-java library.
 *
 * <p>Since there is no one unique identifier for customObjects on the commercetools API, we
 * uniquely identify a customObject as a composite id of the following fields: key, container. This
 * class models such composite key.
 */
public final class CustomObjectCompositeIdentifier {

  private final String key;
  private final String container;
  private static final String WRONG_FORMAT_ERROR_MESSAGE =
      "The custom object identifier value: \"%s\" does not have "
          + "the correct format. The correct format must have a vertical bar \"|\" character "
          + "between the container and key.";

  private CustomObjectCompositeIdentifier(
      @Nonnull final String key, @Nonnull final String container) {
    this.key = key;
    this.container = container;
  }

  /**
   * Given a {@link CustomObjectDraft}, creates a {@link CustomObjectCompositeIdentifier} using the
   * following fields from the supplied {@link CustomObjectDraft}:
   *
   * <ol>
   *   <li>{@link CustomObjectCompositeIdentifier#key}: key of {@link CustomObjectDraft#getKey()}
   *   <li>{@link CustomObjectCompositeIdentifier#container}: container of {@link
   *       CustomObjectDraft#getContainer()}
   * </ol>
   *
   * @param customObjectDraft a composite id is built using its fields.
   * @return a composite id comprised of the fields of the supplied {@link CustomObjectDraft}.
   */
  @Nonnull
  public static CustomObjectCompositeIdentifier of(
      @Nonnull final CustomObjectDraft customObjectDraft) {
    return new CustomObjectCompositeIdentifier(
        customObjectDraft.getKey(), customObjectDraft.getContainer());
  }

  /**
   * Given a {@link CustomObjectDraft}, creates a {@link CustomObjectCompositeIdentifier} using the
   * following fields from the supplied {@link CustomObject}:
   *
   * <ol>
   *   <li>{@link CustomObjectCompositeIdentifier#key}: key of {@link CustomObject#getKey()}
   *   <li>{@link CustomObjectCompositeIdentifier#container}: container of {@link
   *       CustomObject#getContainer()}
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
   * Given a {@link String} and {@link String}, creates a {@link CustomObjectCompositeIdentifier}
   * using the following fields from the supplied attributes.
   *
   * @param key key of CustomerObject to build composite Id.
   * @param container container of CustomerObject to build composite Id.
   * @return a composite id comprised of the fields of the supplied {@code key} and {@code
   *     container}.
   */
  @Nonnull
  public static CustomObjectCompositeIdentifier of(
      @Nonnull final String key, @Nonnull final String container) {
    return CustomObjectCompositeIdentifier.of(
        CustomObjectDraft.ofUnversionedUpsert(container, key, null));
  }

  /**
   * Given a {@link String} with a format "container|key", creates a {@link
   * CustomObjectCompositeIdentifier} using the following fields from the supplied identifier as
   * string.
   *
   * @param identifierAsString string format of the identifier to build composite Id.
   * @return a composite id comprised of the fields of the supplied {@code key} and {@code
   *     container}.
   */
  @Nonnull
  public static CustomObjectCompositeIdentifier of(@Nonnull final String identifierAsString) {
    final String[] containerAndKey = identifierAsString.split("\\|");
    if (containerAndKey.length == 2) {
      return of(containerAndKey[1], containerAndKey[0]);
    }
    throw new IllegalArgumentException(format(WRONG_FORMAT_ERROR_MESSAGE, identifierAsString));
  }

  public String getKey() {
    return this.key;
  }

  public String getContainer() {
    return this.container;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof CustomObjectCompositeIdentifier)) {
      return false;
    }
    final CustomObjectCompositeIdentifier that = (CustomObjectCompositeIdentifier) obj;
    return key.equals(that.key) && container.equals(that.container);
  }

  @Override
  public int hashCode() {
    return Objects.hash(key, container);
  }

  @Override
  public String toString() {
    return format("%s|%s", container, key);
  }
}
