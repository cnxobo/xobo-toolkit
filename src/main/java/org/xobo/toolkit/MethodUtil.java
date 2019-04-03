package org.xobo.toolkit;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.beanutils.ConvertUtils;
import org.springframework.util.ReflectionUtils;

import com.bstek.dorado.data.provider.Page;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thoughtworks.paranamer.AdaptiveParanamer;
import com.thoughtworks.paranamer.CachingParanamer;
import com.thoughtworks.paranamer.Paranamer;

public class MethodUtil {
  private static Paranamer paranamer = new CachingParanamer(new AdaptiveParanamer());
  public static final int MATCH_BY_NAME = 1;
  public static final int MATCH_BY_ORDER = 2;


  public static void main(String[] args)
      throws InstantiationException, IllegalAccessException, IllegalArgumentException,
      InvocationTargetException, SecurityException, NoSuchMethodException {


    Map<String, Object> parameterMap = new HashMap<String, Object>();
    parameterMap.put("pageSize", 10);
    parameterMap.put("pageNo", 5);

    List<String> list = new ArrayList<String>();
    list.add("Bing");
    list.add("Zhou");
    parameterMap.put("userDepts", list);
    parameterMap.put("list", list);
    parameterMap.put("parameter", new HashMap<Object, Object>());

    invokeMethod(new MethodUtil(), "testList", parameterMap);

    System.out.println(ClassUtil.create(Page.class, parameterMap));

    Object obj = JSONUtil.toObject("[1,2,3,\"Bing\"]");
    System.out.println(obj);
    Class<?> page = Page.class;
    Constructor<?>[] constructors = page.getConstructors();
    for (Constructor<?> constructor : constructors) {
      System.out.println(constructor);
      String[] params = paranamer.lookupParameterNames(constructor, false);
      for (String string : params) {
        System.out.println(string);
      }
    }


  }

  /**
   * 反射调用指定方法
   *
   * @param target
   * @param methodName
   * @param parameter
   * @return
   * @throws IllegalArgumentException
   * @throws IllegalAccessException
   * @throws InvocationTargetException
   * @throws SecurityException
   * @throws NoSuchMethodException
   * @throws InstantiationException
   */
  public static Object invokeMethod(Object target, String methodName, Map<String, Object> parameter)
      throws IllegalArgumentException, IllegalAccessException, InvocationTargetException,
      SecurityException, NoSuchMethodException, InstantiationException {
    Class<?>[] paramTypes = null;
    Method method =
        ReflectionUtils.findMethod(BeanReflectionUtil.getClass(target), methodName, paramTypes);
    return invokeMethod(target, method, parameter);
  }

  public static Object invokeMethod(Object target, String methodName, JsonNode rootNode) {
    return invokeMethod(BeanReflectionUtil.getClass(target), target, methodName, rootNode,
        MATCH_BY_NAME);
  }

  public static Object invokeMethodByOrder(Object target, String methodName, JsonNode rootNode) {
    return invokeMethod(BeanReflectionUtil.getClass(target), target, methodName, rootNode,
        MATCH_BY_ORDER);
  }

  public static Object invokeMethodByOrder(Class<?> clazz, Object target, String methodName,
      JsonNode rootNode) {
    return invokeMethod(clazz, target, methodName, rootNode, MATCH_BY_ORDER);
  }

  public static Object invokeMethod(Class<?> clazz, Object target, String methodName,
      JsonNode rootNode, int matchType) {

    clazz = getOriginClassProxyByDubbo(clazz);
    Class<?>[] paramTypes = null;
    Method method = ReflectionUtils.findMethod(clazz, methodName, paramTypes);
    return invokeMethod(target, method, rootNode, matchType);
  }


  public static Class<?> getOriginClassProxyByDubbo(Class<?> clazz) {
    String className = clazz.getName();
    if (className.startsWith("com.alibaba.dubbo.common.bytecode.proxy")) {
      Class<?>[] clazzs = clazz.getInterfaces();
      for (Class<?> interfaceClazz : clazzs) {
        String interfaceClazzName = interfaceClazz.getName();
        if (!interfaceClazzName.startsWith("com.alibaba.dubbo")) {
          return interfaceClazz;
        }
      }
    }
    return clazz;
  }

  @SuppressWarnings("unchecked")
  public static Object invokeMethod(Object target, Method method, Map<String, Object> parameter) {
    String[] parameterNames = paranamer.lookupParameterNames(method);
    Type[] parametersType = method.getGenericParameterTypes();
    Object[] realArgs = new Object[parametersType.length];

    for (int i = 0; i < parameterNames.length; i++) {
      String name = parameterNames[i];
      Type ptype = parametersType[i];
      Object value;
      if (parameterNames.length == 1 && ptype instanceof Class<?>
          && ((Class<?>) ptype).isAssignableFrom(Map.class)) {
        value = parameter;
      } else {
        value = parameter.get(name);
      }
      if (value == null) {
        realArgs[i] = null;
        continue;
      }

      if (ptype instanceof Class<?>) {
        Class<?> clazz = (Class<?>) ptype;
        if (!clazz.isAssignableFrom(value.getClass())) {
          value = ConvertUtils.convert(value, clazz);
        }
        if (!clazz.isPrimitive() && !clazz.isAssignableFrom(value.getClass())
            && !clazz.getPackage().getName().startsWith("java") && value instanceof Map) {
          value = ClassUtil.create(clazz, (Map<String, Object>) value);
        } else if (Collection.class.isAssignableFrom(value.getClass())) {
          Collection<Object> newValues = new ArrayList<Object>();
          Class<?> subClazz = null;
          if (ptype instanceof ParameterizedType) {
            subClazz = (Class<?>) ((ParameterizedType) ptype).getActualTypeArguments()[0];
          }
          if (subClazz != null) {
            for (Object object : (Collection<Object>) value) {
              newValues.add(ClassUtil.create(subClazz, object));
            }
          }
        }
      } else if (ptype instanceof ParameterizedType) {
        ParameterizedType parameterizedType = (ParameterizedType) ptype;
        Type[] typeArguments = parameterizedType.getActualTypeArguments();
        Type rawType = parameterizedType.getRawType();
        Collection<Object> collection = new ArrayList<Object>();
        if (rawType instanceof Class<?>) {
          if (Collection.class.isAssignableFrom((Class<?>) rawType)) {
            if (typeArguments.length > 0) {
              Type typeArgument = typeArguments[0];
              Class<?> rt = (Class<?>) typeArgument;

              for (Object object : (Collection<Object>) value) {
                Object entity = ClassUtil.create(rt, object);
                collection.add(entity);
              }
            }
            value = collection;
          }
        } else if (Map.class.isAssignableFrom((Class<?>) rawType)
            && !typeArguments[1].equals(Object.class)) {
          if (value instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) value;
            Map<String, Object> newMap = new HashMap<String, Object>();
            for (Entry<String, Object> entry : map.entrySet()) {

            }
          }

        }
      }
      realArgs[i] = value;
    }
    try {
      return method.invoke(target, realArgs);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public static Object invokeMethod(Object target, Method method, JsonNode rootNode,
      int matchType) {
    String[] parameterNames = paranamer.lookupParameterNames(method);

    ObjectMapper mapper = new ObjectMapper();
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    Type[] parametersType = method.getGenericParameterTypes();
    Object[] realArgs = new Object[parametersType.length];

    if (rootNode != null) {
      for (int i = 0; i < parameterNames.length; i++) {
        String name = parameterNames[i];
        JsonNode valueNode = null;
        if (matchType == MATCH_BY_ORDER) {
          if (rootNode.isArray()) {
            valueNode = rootNode.get(i);
          } else {
            valueNode = rootNode;
          }
        } else if (matchType == MATCH_BY_NAME) {
          valueNode = rootNode.get(name);
        }

        if (valueNode == null && parameterNames.length == 1 && rootNode.size() > 0) {
          valueNode = rootNode;
        }

        Object value;
        try {
          value = convertJsonNodeToValueOfTargetType(mapper, valueNode, parametersType[i]);
        } catch (IOException e) {
          throw new RuntimeException(e);
        }

        realArgs[i] = value;
      }
    }
    try {
      return method.invoke(target, realArgs);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

  }

  public static Object convertJsonNodeToValueOfTargetType(ObjectMapper mapper, JsonNode valueNode,
      Type type) throws JsonParseException, JsonMappingException, IOException {
    if (valueNode == null) {
      return null;
    }

    Object value = null;
    if (type instanceof Class<?>) {
      value = mapper.readValue(valueNode.toString(), (Class<?>) type);
    } else if (type instanceof ParameterizedType) {
      ParameterizedType parameterizedType = (ParameterizedType) type;
      Type[] typeArguments = parameterizedType.getActualTypeArguments();
      Type rawType = parameterizedType.getRawType();
      if (typeArguments.length > 0) {
        Type typeArgument = typeArguments[0];
        Class<?> rt;
        if (Class.class.isAssignableFrom(typeArgument.getClass())) {
          rt = (Class<?>) typeArgument;
        } else {
          rt = Object.class;
        }
        if (rawType instanceof Class<?>) {
          if (Collection.class.isAssignableFrom((Class<?>) rawType)) {
            if (typeArguments.length > 0) {
              JavaType javaType = mapper.getTypeFactory().constructParametrizedType(ArrayList.class,
                  List.class, rt);
              value = mapper.readValue(valueNode.toString(), javaType);
            }
          } else if (Map.class.isAssignableFrom((Class<?>) rawType)) {
            JavaType javaType = mapper.getTypeFactory()
                .constructParametrizedType(LinkedHashMap.class, Map.class, rt, Object.class);
            value = mapper.readValue(valueNode.toString(), javaType);
          } else {
            JavaType javaType = mapper.getTypeFactory()
                .constructParametrizedType(LinkedHashMap.class, Map.class, rt, Object.class);
            Map<String, Object> result = mapper.readValue(valueNode.toString(), javaType);
            value = ClassUtil.createInstance(rawType, result);
          }
        }

      }
    }
    return value;
  }
}
