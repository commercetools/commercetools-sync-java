package com.commercetools.sync.sdk2.commons.utils;

import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;

import com.commercetools.api.models.ResourceUpdateAction;
import com.commercetools.api.models.common.Reference;
import com.commercetools.api.models.common.ResourceIdentifier;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class CommonTypeUpdateActionUtils {

  /**
   * Compares two {@link Object} and returns a supplied {@link ResourceUpdateAction} as a result in
   * an {@link Optional}. If both the {@link Object}s have the same values, then no update action is
   * needed and hence an empty {@link Optional} is returned.
   *
   * @param oldObject the object which should be updated
   * @param newObject the object with the new information
   * @param updateActionSupplier the supplier that returns the update action to return in the
   *     optional
   * @param <S> the type of the objects to compare
   * @param <U> certain {@link ResourceUpdateAction} implementation type
   * @return A filled optional with the update action or an empty optional if the object values are
   *     identical
   */
  @Nonnull
  public static <S, U extends ResourceUpdateAction> Optional<U> buildUpdateAction(
      @Nullable final S oldObject,
      @Nullable final S newObject,
      @Nonnull final Supplier<U> updateActionSupplier) {

    return !Objects.equals(oldObject, newObject)
        ? Optional.ofNullable(updateActionSupplier.get())
        : Optional.empty();
  }

  /**
   * Compares two objects that are of type {@link Reference} and {@link ResourceIdentifier} (or a
   * type that extends it) and returns a supplied {@link ResourceUpdateAction} as a result in an
   * {@link Optional}. If both the {@link Object}s have the same values, then no update action is
   * needed and hence an empty {@link Optional} is returned.
   *
   * @param oldReference the old reference
   * @param newResourceIdentifier the new resource identifier
   * @param updateActionSupplier the supplier that returns the update action to return in the
   *     optional
   * @param <S> the type of the old reference
   * @param <U> the type of the new resource identifier
   * @param <V> concrete {@link ResourceUpdateAction} implementation type
   * @return A filled optional with the update action or an empty optional if the object values are
   *     identical
   */
  @Nonnull
  public static <S extends Reference, U extends ResourceIdentifier, V extends ResourceUpdateAction>
      Optional<V> buildUpdateActionForReferences(
          @Nullable final S oldReference,
          @Nullable final U newResourceIdentifier,
          @Nonnull final Supplier<V> updateActionSupplier) {

    return !areResourceIdentifiersEqual(oldReference, newResourceIdentifier)
        ? Optional.ofNullable(updateActionSupplier.get())
        : Optional.empty();
  }

  /**
   * Compares the ids of two objects that are of type {@link Reference} and {@link
   * ResourceIdentifier} (or a type that extends it).
   *
   * @param oldReference the old reference
   * @param newResourceIdentifier the new resource identifier
   * @param <T> the type of the old reference
   * @param <S> the type of the new resource identifier
   * @return true or false depending if the reference and resource identifier have the same id.
   */
  public static <T extends Reference, S extends ResourceIdentifier>
      boolean areResourceIdentifiersEqual(
          @Nullable final T oldReference, @Nullable final S newResourceIdentifier) {

    final String oldId = ofNullable(oldReference).map(Reference::getId).orElse(null);
    final String newId =
        ofNullable(newResourceIdentifier).map(ResourceIdentifier::getId).orElse(null);

    return Objects.equals(oldId, newId);
  }

  /**
   * Compares two {@link Object} and returns a supplied list of {@link ResourceUpdateAction} as a
   * result. If both the {@link Object}s have the same values, then no update action is needed and
   * hence an empty list is returned.
   *
   * @param oldObject the object which should be updated
   * @param newObject the object with the new information
   * @param updateActionSupplier the supplier that returns a list of update actions if the objects
   *     are different
   * @param <S> the type of the objects to compare
   * @param <U> certain {@link ResourceUpdateAction} implementation type
   * @return A filled optional with the update action or an empty optional if the object values are
   *     identical
   */
  @Nonnull
  public static <S, U extends ResourceUpdateAction> List<U> buildUpdateActions(
      @Nullable final S oldObject,
      @Nullable final S newObject,
      @Nonnull final Supplier<List<U>> updateActionSupplier) {

    return !Objects.equals(oldObject, newObject) ? updateActionSupplier.get() : emptyList();
  }

  private CommonTypeUpdateActionUtils() {}
}
