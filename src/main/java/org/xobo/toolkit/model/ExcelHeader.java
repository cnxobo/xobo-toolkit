package org.xobo.toolkit.model;

public class ExcelHeader {
  private int index;
  private String label;
  private String value;

  public ExcelHeader() {}

  public ExcelHeader(int index, String label, String value) {
    this.index = index;
    this.label = label;
    this.value = value;
  }

  public int getIndex() {
    return index;
  }

  public void setIndex(int index) {
    this.index = index;
  }

  public String getLabel() {
    return label;
  }

  public void setLabel(String label) {
    this.label = label;
  }

  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.value = value;
  }



}
