package org.xobo.toolkit;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.common.collect.Lists;

public class JsonNodeSerializeUtil {

  public static List<String> toList(String signPropsStr) {
    List<String> signPropsOut = null;
    if (StringUtils.isEmpty(signPropsStr)) {
      signPropsOut = Collections.emptyList();
    } else {
      signPropsOut = new ArrayList<>();
      StringBuilder wordBuilder = new StringBuilder();
      for (int i = 0; i < signPropsStr.length(); i++) {
        Character c = signPropsStr.charAt(i);
        if (Character.isWhitespace(c)) {
          continue;
        } else if (Character.isJavaIdentifierPart(c)) {
          wordBuilder.append(c);
        } else {
          if (wordBuilder.length() > 0) {
            signPropsOut.add(wordBuilder.toString());
            wordBuilder.setLength(0);
          }
          if (c == '[' || c == ']')
            signPropsOut.add(c.toString());
        }
      }
      if (wordBuilder.length() > 0) {
        signPropsOut.add(wordBuilder.toString());
        wordBuilder.setLength(0);
      }
    }
    return signPropsOut;
  }

  public static void jsonNodeToStr(StringBuilder builder, JsonNode dataNode, String props) {
    jsonNodeToStr(builder, dataNode, toList(props));
  }

  public static void jsonNodeToStr(StringBuilder builder, JsonNode dataNode, List<String> props) {
    jsonNodeToStr(builder, dataNode, props, false);
  }

  public static boolean jsonNodeToStr(StringBuilder builder, JsonNode dataNode, List<String> props,
      boolean started) {
    Stack<JsonNode> nodeStack = new Stack<>();
    boolean arrayPropStarted = false;
    boolean arrayPropStartSign = false;

    List<String> arrayPropList = null;
    ArrayNode arrayNode = null;
    for (String prop : props) {
      if (prop.equals("[")) {
        arrayPropStartSign = true;
        continue;
      } else if (prop.equals("]")) {
        arrayPropStarted = false;
        if (arrayNode != null) {
          started = arrayNodetoStr(builder, arrayNode, arrayPropList, started);
          dataNode = nodeStack.pop();
        }
        continue;
      }
      if (arrayPropStartSign) {
        nodeStack.push(dataNode);
        dataNode = dataNode.get(prop);
        arrayPropList = new ArrayList<>();
        arrayNode = (ArrayNode) dataNode;
        arrayPropStarted = true;
        arrayPropStartSign = false;
        continue;
      } else if (arrayPropStarted) {
        arrayPropList.add(prop);
        continue;
      }
      JsonNode propNode = dataNode.get(prop);
      if (started) {
        builder.append("&");
      } else {
        started = true;
      }
      String nodeValue = propNode == null ? "" : propNode.asText("");
      builder.append(prop).append("=").append(nodeValue);
    }
    return started;
  }

  public static boolean arrayNodetoStr(StringBuilder builder, ArrayNode arrayNode,
      List<String> props, boolean started) {
    for (int i = 0; i < arrayNode.size(); i++) {
      JsonNode jsonNode = arrayNode.get(i);
      started = jsonNodeToStr(builder, jsonNode, props, started);
    }
    return started;
  }

  public static void main(String[] args) throws Exception {
    System.out.println(
        toList("[queryList,bizId,taskNo,[queryList,bizId,[queryList,bizId,taskNo,]taskNo,]]"));
  }

  @SuppressWarnings("unused")
  public static void test1(String[] args) throws Exception {
    List<String> props =
        Lists.newArrayList("name", "username", "[depts", "id", "deptName", "deptCode", "]",
            "address");

    Map<String, Object> result = new HashMap<>();
    result.put("name", "ZHOU Bing");
    result.put("username", "xobo");
    result.put("address", "源深路1088号平安财富大厦裙楼3楼智阳网络技术有限公司");

    List<Map<String, Object>> depts = new ArrayList<>();
    depts.add(createDept(1, "D0001", "dept01"));
    depts.add(createDept(2, "D0002", "dept02"));
    depts.add(createDept(3, "D0003", "dept03"));

    result.put("depts", depts);

    PrivateKey privateKey = RsaUtil.loadPrivateKey(
        IOUtils.toString(
            JsonNodeSerializeUtil.class.getResource("/cert/privateKey.txt").openStream()));

    PublicKey publicKey = RsaUtil.loadPublicKey(
        IOUtils
            .toString(JsonNodeSerializeUtil.class.getResource("/cert/publicKey.txt").openStream()));

    System.out.println(JSONUtil.toJSON(result));
    JsonNode jsonNode = JSONUtil.toJsonNode(result);
    StringBuilder builder = new StringBuilder();
    jsonNodeToStr(builder, jsonNode, props, false);
    String plainText = builder.toString();
    System.out.println(plainText);
    String signature = RsaUtil.sign(plainText, privateKey);
    System.out.println(signature);
  }


  static Map<String, Object> createDept(int deptId, String deptCode, String deptName) {
    Map<String, Object> result = new HashMap<>();
    result.put("id", deptId);
    result.put("deptCode", deptCode);
    result.put("deptName", deptName);
    return result;
  }

}
