package org.xobo.toolkit;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PatternUtil {
  public static String find(String content, Pattern pattern, int index) {
    Matcher m = pattern.matcher(content);
    if (m.find()) {
      return m.group(1);
    } else {
      return null;
    }
  }

  public static String find(String content, String pattern, int index) {
    Pattern p = Pattern.compile(pattern);
    return find(content, p, index);
  }
}
