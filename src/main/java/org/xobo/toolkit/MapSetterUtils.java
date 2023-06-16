package org.xobo.toolkit;

import java.util.HashMap;
import java.util.Map;

public class MapSetterUtils {
  public static <K, V> Map<K, V> setDefault(Map<K, V> map, K key, V defalutValue) {
    if (map == null) {
      map = new HashMap<K, V>();
    }
    if (!map.containsKey(key)) {
      map.put(key, defalutValue);
    }
    return map;
  }
}
