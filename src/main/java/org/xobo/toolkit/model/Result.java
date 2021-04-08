package org.xobo.toolkit.model;

import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.xobo.toolkit.StringUtils;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public class Result<T> implements Serializable {



  /**
   * 
   */
  private static final long serialVersionUID = -3077600846081264623L;

  public enum ResultStatus {
    Success, Fail, Unknown, Processing;

    private static Map<String, ResultStatus> namesMap = new HashMap<String, ResultStatus>();

    static {
      namesMap.put("S", Success);
      namesMap.put("F", Fail);
      namesMap.put("U", Unknown);
      namesMap.put("P", Processing);

    }

    @JsonCreator
    public static ResultStatus forValue(String value) {
      return namesMap.get(StringUtils.upperCase(value));
    }

    @JsonValue
    public String toValue() {
      for (Entry<String, ResultStatus> entry : namesMap.entrySet()) {
        if (entry.getValue() == this)
          return entry.getKey();
      }
      return null; // or fail
    }

  };

  // @JsonSerialize(using = ResultStatusSerializer.class)
  private ResultStatus status;

  private String code;
  // 返回描述信息(接口描述)
  private String message;
  // 使用泛型T返回前端想要的数据(任意数据类型),通常这里使用Map<String,Object>来接收也可以。
  private T data;

  public static <K> Result<K> buildOk() {
    Result<K> result = new Result<K>();
    result.status = ResultStatus.Success;
    result.code = "000000";
    return result;
  }

  public static <K> Result<K> buildOk(K data) {
    Result<K> result = new Result<K>();
    result.status = ResultStatus.Success;
    result.code = "000000";
    result.data = data;
    return result;
  }

  public static <K> Result<K> buildUnknown(String errorMessage) {
    Result<K> result = new Result<K>();
    result.status = ResultStatus.Unknown;
    result.code = "000001";
    result.message = errorMessage;
    return result;
  }

  public static <K> Result<K> buildUnknown(String errorCode, String errorMessage) {
    Result<K> result = new Result<K>();
    result.code = errorCode;
    result.message = errorMessage;
    result.status = ResultStatus.Unknown;
    return result;
  }

  public static <K> Result<K> buildError(String errorCode, String errorMessage) {
    Result<K> result = new Result<K>();
    result.code = errorCode;
    result.message = errorMessage;
    result.status = ResultStatus.Fail;
    return result;
  }

  public static <K> Result<K> buildError(String errorMessage) {
    Result<K> result = new Result<K>();
    result.code = "1";
    result.message = errorMessage;
    result.status = ResultStatus.Fail;
    return result;
  }

  public static <K> Result<K> buildProcessing(String errorMessage) {
    Result<K> result = new Result<K>();
    result.status = ResultStatus.Processing;
    result.code = "000001";
    return result;
  }

  public static <K> Result<K> buildProcessing(String errorCode, String errorMessage) {
    Result<K> result = new Result<K>();
    result.code = errorCode;
    result.message = errorMessage;
    result.status = ResultStatus.Processing;
    return result;
  }

  public ResultStatus getStatus() {
    return status;
  }

  public void setStatus(ResultStatus status) {
    this.status = status;
  }

  public String getCode() {
    return code;
  }

  public void setCode(String code) {
    this.code = code;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public T getData() {
    return data;
  }

  public void setData(T data) {
    this.data = data;
  }

  public Map<String, Object> toMap() {
    Map<String, Object> resultMap = new LinkedHashMap<String, Object>();
    resultMap.put("status", status.toValue());
    resultMap.put("code", code);
    resultMap.put("data", data);
    resultMap.put("message", message);
    return resultMap;
  }

}
