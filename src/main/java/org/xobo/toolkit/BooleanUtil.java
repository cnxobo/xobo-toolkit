package org.xobo.toolkit;

public class BooleanUtil {
  public static boolean isTrue(String value) {
    value = StringUtils.trim(value);
    return StringUtils.isNotEmpty(value) && !"0".endsWith(value);
  }
}
