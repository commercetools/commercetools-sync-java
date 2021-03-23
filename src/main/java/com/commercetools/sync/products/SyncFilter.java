package com.commercetools.sync.products;

import static java.util.Arrays.asList;
import static java.util.Optional.ofNullable;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import javax.annotation.Nonnull;

/**
 * Defines either a blacklist or a whitelist for filtering certain update action groups ({@link
 * ActionGroup}).
 *
 * <p>The action groups can be a list of any of the values of the enum {@link ActionGroup}, namely:
 *
 * <ul>
 *   <li>ATTRIBUTES
 *   <li>PRICES
 *   <li>IMAGES
 *   <li>CATEGORIES
 *   <li>.. and others
 * </ul>
 *
 * <p>The {@code includeOnly} flag defines whether the list is to be blacklisted (@code false) or
 * whitelisted (@code true). A blacklist means that <b>everything but</b> these action groups will
 * be synced. A whitelist means that <b>only</b> these action groups will be synced.
 */
public final class SyncFilter {

  /** Defines which attributes to calculate update actions for. */
  private final Set<ActionGroup> actionGroups;

  /** Defines the filter type: blacklist (false) or whitelist (true). */
  private final boolean includeOnly;

  private static SyncFilter defaultSyncFilter = null;

  private SyncFilter(final boolean includeOnly, @Nonnull final ActionGroup[] actionGroups) {
    this.includeOnly = includeOnly;
    this.actionGroups = new HashSet<>(asList(actionGroups));
  }

  private SyncFilter() {
    this.includeOnly = false;
    this.actionGroups = Collections.emptySet();
  }

  @Nonnull
  public static SyncFilter ofWhiteList(@Nonnull final ActionGroup... actionGroups) {
    return new SyncFilter(true, actionGroups);
  }

  @Nonnull
  public static SyncFilter ofBlackList(@Nonnull final ActionGroup... actionGroups) {
    return new SyncFilter(false, actionGroups);
  }

  @Nonnull
  public static SyncFilter of() {
    defaultSyncFilter = ofNullable(defaultSyncFilter).orElseGet(SyncFilter::new);
    return defaultSyncFilter;
  }

  /**
   * Checks if the supplied {@link ActionGroup} passes {@code this} filter.
   *
   * <p>Passing the filter has an XOR logic as follows:
   *
   * <table>
   * <caption>syncFilter filtering logic</caption>
   * <tr>
   * <th> includeOnly </th> <th> actionGroups contains actionGroup </th> <th> passes filter </th>
   * </tr>
   * <tr><td>false</td><td>false</td><td>true (actionGroup is not in blacklist)</td></tr>
   * <tr><td>false</td><td>true</td><td>false (actionGroup is in blacklist)</td></tr>
   * <tr><td>true</td><td>false</td><td>false (actionGroup is not in whitelist)</td></tr>
   * <tr><td>true</td><td>true</td><td>true (actionGroup is in whitelist)</td></tr>
   * </table>
   *
   * @param actionGroup the supplied action group to be tested if it passes the current filter.
   * @return true if the {@link ActionGroup} passes {@code this} filter, otherwise false.
   */
  public boolean filterActionGroup(@Nonnull final ActionGroup actionGroup) {
    return includeOnly == actionGroups.contains(actionGroup);
  }
}
