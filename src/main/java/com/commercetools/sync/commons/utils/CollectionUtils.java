package com.commercetools.sync.commons.utils;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class CollectionUtils {

  /**
   * Create a new collection which contains only elements which satisfy {@code includeCondition}
   * predicate.
   *
   * @param collection {@link Collection} to be filtered
   * @param includeCondition condition which verifies whether the value should be included to the
   *     final result.
   * @param <T> type of the collection items.
   * @return new filtered stream with items which satisfy {@code includeCondition} predicate. If
   *     {@code collection} is <b>null</b> or empty - empty stream is returned.
   */
  @Nonnull
  public static <T> Stream<T> filterCollection(
      @Nullable final Collection<T> collection, @Nonnull final Predicate<T> includeCondition) {

    return collection == null ? Stream.empty() : collection.stream().filter(includeCondition);
  }

  /**
   * Convert a {@code collection} to a set of values using {@code keyMapper} mapping.
   *
   * <p>If the collection has duplicate keys - only one will be stored, which one is not defined
   * though.
   *
   * @param collection {@link Collection} to convert
   * @param keyMapper function which converts the collection entry to a key.
   * @param <T> type of collection entries
   * @param <K> type of Set key
   * @return new {@link Set} which consists of items, converted from &lt;T&gt; entries to keys using
   *     {@code entryToKey} function.
   */
  @Nonnull
  public static <T, K> Set<K> collectionToSet(
      @Nullable final Collection<T> collection,
      @Nonnull final Function<? super T, ? extends K> keyMapper) {
    return emptyIfNull(collection).stream().map(keyMapper).collect(toSet());
  }

  /**
   * Convert a {@code collection} to a map using {@code keyMapper} and {@code valueMapper} mappers.
   * If keys are duplicated - only one value is stored, which one - undefined.
   *
   * @param collection {@link Collection} to convert
   * @param keyMapper function which converts the collection entry to a key
   * @param valueMapper function which converts the collection entry to a value
   * @param <T> type of collection entries
   * @param <K> type of Map key
   * @param <V> type of Map value
   * @return new {@link Map} which consists of key-value pairs, converted from &lt;T&gt; entries
   * @see #collectionToMap(Collection, Function)
   */
  @Nonnull
  public static <T, K, V> Map<K, V> collectionToMap(
      @Nullable final Collection<T> collection,
      @Nonnull final Function<? super T, ? extends K> keyMapper,
      @Nonnull final Function<? super T, ? extends V> valueMapper) {
    return emptyIfNull(collection).stream()
        .collect(toMap(keyMapper, valueMapper, (k1, k2) -> k1)); // ignore duplicates
  }

  /**
   * Same as {@link #collectionToMap(Collection, Function, Function)}, but uses entries themselves
   * as map values.
   *
   * @param collection {@link Collection} to convert
   * @param keyMapper function which converts the collection entry to a key
   * @param <T> type of collection entries
   * @param <K> type of Map key
   * @return new {@link Map} which consists of key-value pairs, converted from &lt;T&gt; entries
   * @see #collectionToMap(Collection, Function, Function)
   */
  @Nonnull
  public static <T, K> Map<K, T> collectionToMap(
      @Nullable final Collection<T> collection,
      @Nonnull final Function<? super T, ? extends K> keyMapper) {
    return collectionToMap(collection, keyMapper, value -> value);
  }

  /**
   * Safe wrapper around nullable collection instances: returns {@code collection} argument itself,
   * if the {@code collection} is non-null, otherwise returns (immutable) empty collection.
   *
   * @param collection {@link Collection} instance to process
   * @param <T> collection entities type
   * @return original {@code collection} instance, if non-null; otherwise immutable empty collection
   *     instance.
   * @see #emptyIfNull(List)
   * @see #emptyIfNull(Set)
   * @see #emptyIfNull(Map)
   */
  @Nonnull
  public static <T> Collection<T> emptyIfNull(@Nullable final Collection<T> collection) {
    return collection == null ? emptyList() : collection;
  }

  /**
   * Safe wrapper around nullable list instances: returns {@code list} argument itself, if the
   * {@code list} is non-null, otherwise returns (immutable) empty list.
   *
   * @param list {@link List} instance to process
   * @param <T> list entities type
   * @return original {@code list} instance, if non-null; otherwise immutable empty list instance.
   * @see #emptyIfNull(Collection)
   * @see #emptyIfNull(Set)
   * @see #emptyIfNull(Map)
   */
  @Nonnull
  public static <T> List<T> emptyIfNull(@Nullable final List<T> list) {
    return list == null ? emptyList() : list;
  }

  /**
   * Safe wrapper around nullable set instances: returns {@code set} argument itself, if the {@code
   * set} is non-null, otherwise returns (immutable) empty set.
   *
   * @param set {@link Set} instance to process
   * @param <T> set entities type
   * @return original {@code set} instance, if non-null; otherwise immutable empty set instance.
   * @see #emptyIfNull(Collection)
   * @see #emptyIfNull(List)
   * @see #emptyIfNull(Map)
   */
  @Nonnull
  public static <T> Set<T> emptyIfNull(@Nullable final Set<T> set) {
    return set == null ? emptySet() : set;
  }

  /**
   * Safe wrapper around nullable map instances: returns {@code map} argument itself, if the {@code
   * map} is non-null, otherwise returns (immutable) empty map.
   *
   * @param map {@link Map} instance to process
   * @param <K> map key type
   * @param <V> map value type
   * @return original {@code map} instance, if non-null; otherwise immutable empty map instance.
   * @see #emptyIfNull(Collection)
   * @see #emptyIfNull(List)
   * @see #emptyIfNull(Set)
   */
  @Nonnull
  public static <K, V> Map<K, V> emptyIfNull(@Nullable final Map<K, V> map) {
    return map == null ? emptyMap() : map;
  }

  private CollectionUtils() {}
}
