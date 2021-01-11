package com.commercetools.sync.commons.utils;

import com.commercetools.sync.products.ActionGroup;
import com.commercetools.sync.products.SyncFilter;
import java.util.function.Supplier;
import javax.annotation.Nonnull;

public final class FilterUtils {

  /**
   * Given a {@link SyncFilter}, a {@link ActionGroup} and 2 {@link Supplier}. This method checks if
   * the action group passes the criteria of the syncFiler, i.e. either of the following cases:
   *
   * <ul>
   *   <li>The {@link SyncFilter} is null.
   *   <li>Passes the {@link SyncFilter}.
   * </ul>
   *
   * <p>If the action group passes the filter, then the first supplier is executed, if not then the
   * second supplier is executed.
   *
   * @param syncFilter defines the blacklisting/whitelisted action groups.
   * @param actionGroup the action group under check.
   * @param resultIfFilteredIn the supplier to be executed if the action group passes the filter.
   * @param resultIfFilteredOut the supplier to be executed if the action group doesn't pass the
   *     filter.
   * @param <T> the type of the result of the suppliers and, in turn, the return of this method.
   * @return the result of the executed supplier.
   */
  @Nonnull
  public static <T> T executeSupplierIfPassesFilter(
      @Nonnull final SyncFilter syncFilter,
      @Nonnull final ActionGroup actionGroup,
      @Nonnull final Supplier<T> resultIfFilteredIn,
      @Nonnull final Supplier<T> resultIfFilteredOut) {
    // If syncFilter is null or the action group passes the filter, execute resultIfFilteredIn
    // supplier.
    final Supplier<T> resultantSupplier =
        syncFilter.filterActionGroup(actionGroup) ? resultIfFilteredIn : resultIfFilteredOut;

    return resultantSupplier.get();
  }

  private FilterUtils() {}
}
