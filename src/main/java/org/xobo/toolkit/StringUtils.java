package org.xobo.toolkit;

/**
 * 裁剪commons-lang3的工具栏
 * 
 * @author Bing
 *
 */
public class StringUtils {
  public static boolean isEmpty(final CharSequence cs) {
    return cs == null || cs.length() == 0;
  }

  public static boolean isNotEmpty(final CharSequence cs) {
    return !isEmpty(cs);
  }

  public static String trim(final String str) {
    return str == null ? null : str.trim();
  }

  public static String upperCase(final String str) {
    if (str == null) {
      return null;
    }
    return str.toUpperCase();
  }
}
