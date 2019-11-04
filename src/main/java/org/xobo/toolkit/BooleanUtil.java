package org.xobo.toolkit;

import org.apache.commons.lang3.StringUtils;

public class BooleanUtil {
  public static boolean isTrue(String value) {
    value = StringUtils.trim(value);
    return StringUtils.isNotEmpty(value) && !"0".endsWith(value);
  }
}
