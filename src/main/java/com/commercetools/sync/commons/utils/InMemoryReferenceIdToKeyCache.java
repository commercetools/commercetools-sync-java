package com.commercetools.sync.commons.utils;

import java.util.Map;

public interface InMemoryReferenceIdToKeyCache {

  void add(String key, String value);

  void remove(String key);

  void addAll(Map<String, String> idToKeyValues);

  boolean containsKey(String key);

  String get(String key);

  Map<String, String> getMap();

  void clearCache();
}
