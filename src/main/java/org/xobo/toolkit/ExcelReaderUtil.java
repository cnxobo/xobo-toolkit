package org.xobo.toolkit;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.ss.util.NumberToTextConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xobo.toolkit.model.ExcelHeader;

public class ExcelReaderUtil {
  private static Logger logger = LoggerFactory.getLogger(ExcelReaderUtil.class);

  @SuppressWarnings("unchecked")
  private static <T> T create(Class<T> clazz) {
    if (Map.class.equals(clazz)) {
      clazz = (Class<T>) HashMap.class;
    }
    try {
      return clazz.newInstance();
    } catch (InstantiationException | IllegalAccessException e) {
      throw new RuntimeException(e);
    }
  }

  public static Workbook getWorkBook(InputStream inputStream) {
    Workbook workbook = null;
    try {
      workbook = WorkbookFactory.create(inputStream);
    } catch (IOException | EncryptedDocumentException | InvalidFormatException e) {
      logger.error(e.getMessage());
      throw new RuntimeException(e);
    }
    return workbook;
  }

  public static Workbook getWorkBook(File file) {
    Workbook workbook = null;
    try {
      workbook = WorkbookFactory.create(file);
    } catch (IOException | EncryptedDocumentException | InvalidFormatException e) {
      logger.error(e.getMessage());
      throw new RuntimeException(e);
    }
    return workbook;
  }

  public static List<List<String>> toList(File file) throws IOException {
    return toList(file, 0, 1);
  }

  public static List<List<String>> toList(File file, int sheetIndex, int startIndex)
      throws IOException {
    return toList(file, sheetIndex, startIndex, -1);
  }

  public static List<List<String>> toList(File file, int sheetIndex, int startIndex, int endIndex)
      throws IOException {
    try (InputStream is = new FileInputStream(file)) {
      Workbook workbook = getWorkBook(is);
      return toList(workbook, sheetIndex, startIndex, endIndex);
    }
  }

  public static List<List<String>> toList(InputStream is, int sheetIndex, int startIndex,
      int endIndex) throws IOException {
    Workbook workbook = getWorkBook(is);
    return toList(workbook, sheetIndex, startIndex, endIndex);
  }


  /**
   * 读入excel文件，解析后返回
   *
   * @param endIndex TODO
   * @param file
   * @throws IOException
   */
  public static List<List<String>> toList(Workbook workbook, int sheetIndex, int startIndex,
      int endIndex) throws IOException {
    // 创建返回对象，把每行中的值作为一个数组，所有行作为一个集合返回
    List<List<String>> sheetList = new ArrayList<>();
    if (workbook != null) {
      // 获得当前sheet工作表
      Sheet sheet = workbook.getSheetAt(sheetIndex);
      if (sheet == null) {
        return Collections.emptyList();
      }
      // 获得当前sheet的结束行
      int lastRowNum = endIndex > 0 ? endIndex : sheet.getLastRowNum();
      // 循环除了第一行的所有行
      for (int rowNum = startIndex; rowNum <= lastRowNum; rowNum++) {
        // 获得当前行
        Row row = sheet.getRow(rowNum);
        if (row == null) {
          continue;
        }
        // 获得当前行的开始列
        int firstCellNum = row.getFirstCellNum();
        // 获得当前行最后一列列数
        int lastCellNum = row.getLastCellNum();
        
        List<String> rowValues = new ArrayList<>(lastCellNum);
        // 循环当前行
        for (int cellNum = firstCellNum; cellNum < lastCellNum; cellNum++) {
          Cell cell = row.getCell(cellNum);
          rowValues.add(getCellValue(cell));
        }
        sheetList.add(rowValues);
      }
    }
    return sheetList;
  }

  @SuppressWarnings("rawtypes")
  public static List<Map> toMap(File file, int sheetIndex, int startIndex, int endIndex,
      String[] properties) throws IOException {
    return toPOJO(file, sheetIndex, startIndex, endIndex, Map.class, properties);
  }

  @SuppressWarnings("rawtypes")
  public static List<Map> toMap(File file, String[] properties) throws IOException {
    return toMap(file, 0, 1, -1, properties);
  }


  public static <T> List<T> toPOJO(File file, int sheetIndex, int startIndex, int endIndex,
      Class<T> clazz, Map<Integer, String> propertyMap) throws IOException {

    List<List<String>> sheetList = null;
    try (InputStream is = new FileInputStream(file)) {
      Workbook workbook = getWorkBook(is);
      sheetList = toList(workbook, sheetIndex, startIndex, endIndex);
    }

    if (CollectionUtils.isEmpty(sheetList)) {
      return Collections.emptyList();
    }

    List<T> objList = new ArrayList<>(sheetList.size());

    for (List<String> list : sheetList) {
      T targetObj = create(clazz);
      objList.add(targetObj);


      for (int i = 0; i < list.size(); i++) {
        String property = propertyMap.get(i);
        if (StringUtils.isNotEmpty(property)) {
          try {
            BeanUtils.setProperty(targetObj, property, list.get(i));
          } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
          }
        }
      }
    }

    return objList;
  }

  public static <T> List<T> toPOJO(File file, int sheetIndex, int startIndex, int endIndex,
      Class<T> clazz, String[] properties) throws IOException {
    Map<Integer, String> propertyMap = new HashMap<>();
    for (int i = 0; i < properties.length; i++) {
      propertyMap.put(i, properties[i]);
    }
    return toPOJO(file, sheetIndex, startIndex, endIndex, clazz, propertyMap);
  }

  public static List<ExcelHeader> loadExcelHeadList(File file, int startRow) {
    try {
      return loadExcelHeadList(new FileInputStream(file), startRow);
    } catch (FileNotFoundException e) {
      throw new RuntimeException(e);
    }
  }

  public static List<ExcelHeader> loadExcelHeadList(InputStream is, int startRow) {
    List<List<String>> result = null;
    try (InputStream inputStream = new BufferedInputStream(is)) {
      result = ExcelReaderUtil.toList(inputStream, 0, startRow, startRow + 1);
      if (result.isEmpty()) {
        return null;
      }

      List<ExcelHeader> excelHeaderList = new ArrayList<>();
      List<String> row = result.get(0);
      List<String> valueRow = result.size() > 1 ? result.get(1) : row;
      for (int i = 0; i < row.size(); i++) {
        excelHeaderList.add(new ExcelHeader(i, row.get(i), valueRow.get(i)));
      }
      return excelHeaderList;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static String getCellValue(Cell cell) {
    String ret = "";
    if (cell == null) {
      return ret;
    }
    switch (cell.getCellTypeEnum()) {
      case BLANK:
        ret = "";
        break;
      case BOOLEAN:
        ret = String.valueOf(cell.getBooleanCellValue());
        break;
      case ERROR:
        ret = null;
        break;
      case FORMULA:
        Workbook wb = cell.getSheet().getWorkbook();
        CreationHelper crateHelper = wb.getCreationHelper();
        FormulaEvaluator evaluator = crateHelper.createFormulaEvaluator();
        ret = getCellValue(evaluator.evaluateInCell(cell));
        break;
      case NUMERIC:
        if (DateUtil.isCellDateFormatted(cell)) {
          Date theDate = cell.getDateCellValue();
          DateFormat format = new SimpleDateFormat(DATE_OUTPUT_PATTERNS);
          ret = format.format(theDate);
        } else {
          ret = NumberToTextConverter.toText(cell.getNumericCellValue());
        }
        break;
      case STRING:
        ret = cell.getRichStringCellValue().getString();
        break;
      default:
        ret = null;
    }

    return null != ret ? ret.trim() : null; // 有必要自行trim
  }

  public final static String DATE_OUTPUT_PATTERNS = "yyyy-MM-dd HH:mm:ss";

}
