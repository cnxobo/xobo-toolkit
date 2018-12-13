package org.xobo.toolkit;

import com.thoughtworks.paranamer.AdaptiveParanamer;
import com.thoughtworks.paranamer.CachingParanamer;
import com.thoughtworks.paranamer.Paranamer;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.beanutils.ConvertUtils;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;
import java.util.*;

public class ClassUtil {
  private static Paranamer paranamer = new CachingParanamer(new AdaptiveParanamer());

  public static <T> T createNewEntity(Class<T> clazz, Object instance) {
    T obj = createNewInstance(clazz);
    try {
      BeanUtils.copyProperties(obj, instance);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    return obj;
  }

  public static <T> T createNewInstance(Class<T> clazz) {
    T obj = null;
    try {
      obj = clazz.newInstance();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    return obj;
  }


  public static Object createInstance(Type type, Map<String, Object> value) {
    Object instance = null;

    try {
      instance = type.getClass().newInstance();
      BeanUtils.populate(instance, (Map<String, ? extends Object>) value);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    return instance;
  }

  static class ClassConstructor {
    private Map<Set<String>, Constructor<?>> parametersSet =
      new HashMap<Set<String>, Constructor<?>>();
    private String singleConstructorName;

    public ClassConstructor(Class<?> clazz) {
      Constructor<?>[] constructors = clazz.getConstructors();
      for (Constructor<?> constructor : constructors) {
        String[] params = paranamer.lookupParameterNames(constructor, false);
        List<String> parameterNameList = Arrays.asList(params);
        parametersSet.put(new LinkedHashSet<String>(parameterNameList), constructor);
        if (singleConstructorName == null && parameterNameList.size() == 1) {
          singleConstructorName = parameterNameList.get(0);
        }
      }
    }

    public Object create(Object obj) throws InstantiationException, IllegalAccessException,
      IllegalArgumentException, InvocationTargetException {
      Map<String, Object> map = new HashMap<String, Object>();
      if (!(obj instanceof Map) && singleConstructorName != null) {
        map.put(singleConstructorName, obj);
      }
      return createByMap(map);
    }

    public Object createByMap(Map<String, Object> map) throws InstantiationException,
      IllegalAccessException, IllegalArgumentException, InvocationTargetException {
      Object[] initargs = null;

      Object objSet = map.keySet();
      Constructor<?> constructor = parametersSet.get(objSet);
      if (constructor == null && parametersSet.size() > 0) {
        constructor = parametersSet.values().iterator().next();
      }
      String[] paramNames = paranamer.lookupParameterNames(constructor, false);
      Class<?>[] paramTypes = constructor.getParameterTypes();
      initargs = new Object[paramTypes.length];

      for (int i = 0; i < paramTypes.length; i++) {
        initargs[i] = ConvertUtils.convert(map.get(paramNames[i]), paramTypes[i]);
      }
      Object value = constructor.newInstance(initargs);
      BeanUtils.copyProperties(value, map);
      return value;
    }

    public void add(Constructor<?> constructor) {

    }
  }


  private static Map<Class<?>, ClassConstructor> clazzConstructorMap =
    new HashMap<Class<?>, ClassConstructor>();

  @SuppressWarnings("unchecked")
  public static <T> T create(Class<T> clazz, Object target) {
    ClassConstructor classConstructor = clazzConstructorMap.get(clazz);
    if (classConstructor == null) {
      classConstructor = new ClassConstructor(clazz);
      clazzConstructorMap.put(clazz, classConstructor);
    }
    try {
      return (T) classConstructor.create(target);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

}
