package com.lwouis.labanquepostale_pdf_parser;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.giaybac.traprange.PDFTableExtractor;
import com.giaybac.traprange.entity.Table;
import com.giaybac.traprange.entity.TableCell;
import com.giaybac.traprange.entity.TableRow;
import com.google.common.primitives.Ints;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.pdfbox.pdmodel.PDDocument;

class LaBanquePostalePdfParser {

  private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy/MM/dd");
  private static final Pattern YEAR_FORMAT = Pattern.compile("([0-9]{3};?[0-9])");

  public static void generateConsolidatedReportOfOperations(String folderPath, String reportPath) throws IOException,
      ParseException {
    // turn off logs generated by dependencies
    Logger.getRootLogger().setLevel(Level.OFF);
    List<Operation> operations = parseOperationsFromAllPdf(folderPath);
    writeOperationsIntoReportFile(reportPath, operations);
  }

  private static void writeOperationsIntoReportFile(String reportPath, List<Operation> operations) throws
      FileNotFoundException, UnsupportedEncodingException {
    PrintWriter writer = new PrintWriter(reportPath, "UTF-8");
    for (Operation operation : operations) {
      writer.println(operation.toCsv(DATE_FORMAT));
    }
    writer.close();
  }

  private static List<Operation> parseOperationsFromAllPdf(String folderPath) throws IOException, ParseException {
    File[] statements = new File(folderPath).listFiles();
    List<Operation> operations = new ArrayList<>();
    for (File statement : statements) {
      String fileName = statement.getName();
      if (!fileName.substring(fileName.length() - 4, fileName.length()).equals(".pdf")) {
        continue;
      }
      operations.addAll(parseOperationsFromOnePdf(statement));
    }
    return operations;
  }

  private static List<Operation> parseOperationsFromOnePdf(File statement) throws IOException, ParseException {
    PDDocument doc = PDDocument.load(statement);
    int nPages = doc.getNumberOfPages();
    Map<Integer, int[]> rowsToIgnore = rowsToIgnore(statement, nPages);
    return parseOperations(statement, rowsToIgnore);
  }

  private static Map<Integer, int[]> rowsToIgnore(File statement, int nPages) {
    Map<Integer, int[]> rowsToIgnoreByPage = new HashMap<>();
    for (int pageId = 0; pageId < nPages; pageId++) {
      PdfPageConfig pdfPageConfig = pageConfig(statement, pageId);
      rowsToIgnoreByPage.put(pageId, Ints.toArray(pdfPageConfig.getRowsToIgnore()));
      if (pdfPageConfig.isLastPage()) {
        break;
      }
    }
    return rowsToIgnoreByPage;
  }

  private static PdfPageConfig pageConfig(File statement, int pageId) {
    PdfPageConfig pdfPageConfig = new PdfPageConfig();
    PDFTableExtractor extractor = new PDFTableExtractor();
    extractor.setSource(statement);
    extractor.addPage(pageId);
    List<Table> tables = extractor.extract();
    List<TableRow> rows = tables.get(0).getRows();
    boolean tableIsReached = false;
    boolean footerIsReached = false;
    for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
      String rowString = rows.get(rowIndex).toString();
      if (tableIsReached) {
        if (footerIsReached) {
          // in footer: ignore row
          pdfPageConfig.addRowToIgnore(rowIndex);
        } else {
          if (rowString.contains("Ilvousestconseillé") || rowString.contains("Page ")) {
            // end of table for current page
            pdfPageConfig.addRowToIgnore(rowIndex);
            footerIsReached = true;
          } else if (rowString.contains("Total") && rowString.contains("opérations")) {
            // end of table
            pdfPageConfig.addRowToIgnore(rowIndex);
            footerIsReached = true;
            pdfPageConfig.reachedLastPage();
          }
          // in table
        }
      } else {
        // in header: ignore row
        pdfPageConfig.addRowToIgnore(rowIndex);
        if (rowString.contains("Date") && rowString.contains("Opération")) {
          // end of header
          tableIsReached = true;
        }
      }
    }

    return pdfPageConfig;
  }

  private static List<Operation> parseOperations(File statement, Map<Integer, int[]> rowsToIgnore) throws
      ParseException {
    List<Operation> operations = new ArrayList<>();
    String yearOfTheOperations = null;
    for (int pageId = 0; pageId < rowsToIgnore.size(); pageId++) {
      List<TableRow> rows = extractTableFromPage(statement, rowsToIgnore, pageId);
      if (yearOfTheOperations == null) {
        yearOfTheOperations = findYearOfTheOperations(rows);
      }
      for (TableRow row : rows) {
        parseOperationFromRow(operations, yearOfTheOperations, row);
      }
    }
    return operations;
  }

  private static void parseOperationFromRow(List<Operation> operations, String yearOfTheOperations, TableRow row)
      throws ParseException {
    List<TableCell> cells = row.getCells();
    String rowString = row.toString();
    // sometimes parsing produces garbage rows; we skip those
    if (rowString.isEmpty() || rowString.equals(" ") || rowString.equals(";4")) {
      return;
    }

    if (rowString.startsWith(";")) {
      // ignore "Soit en francs" column (their row starts with empty cells)
      if (rowString.startsWith(";;;;")) {
        return;
      }
      // description split on multiple lines
      String description = cells.get(1).getContent();
      if (description.isEmpty()) {
        description = cells.get(2).getContent();
      }
      if (description.isEmpty()) {
        return;
      }
      operations.get(operations.size() - 1).appendDescription(description);
      return;
    }

    String dateString = cells.get(0).getContent();
    // date is sometimes split on the first 2 cells
    int cellOffset = 0;
    if (dateString.length() != 5) {
      dateString += cells.get(1).getContent();
      cellOffset = 1;
    }
    Date date = DATE_FORMAT.parse(yearOfTheOperations + "/" + dateString);
    String description = cells.get(cellOffset + 1).getContent();
    // some description start with an arrow which is parsed as "4"; we remove it
    description = description.replaceFirst("^4", "");
    String debitedAmount = cells.get(cellOffset + 2).getContent();
    String amountString;
    if (!debitedAmount.isEmpty()) {
      amountString = "-" + debitedAmount;
    } else {
      String creditedAmount = cells.get(cellOffset + 3).getContent();
      if (creditedAmount.isEmpty()) {
        creditedAmount = cells.get(cellOffset + 4).getContent();
      }
      amountString = "+" + creditedAmount;
    }
    // e.g. 3 000,45 -> 3000.45
    amountString = amountString.replace(",", ".").replace(" ", "");
    BigDecimal amount = new BigDecimal(amountString);
    operations.add(new Operation(date, description, amount));
  }

  private static List<TableRow> extractTableFromPage(File statement, Map<Integer, int[]> rowsToIgnore, int pageId) {
    PDFTableExtractor extractor = new PDFTableExtractor();
    extractor.setSource(statement);
    extractor.addPage(pageId);
    extractor.exceptLine(rowsToIgnore.get(pageId));
    List<Table> tables = extractor.extract();
    return tables.get(0).getRows();
  }

  private static String findYearOfTheOperations(List<TableRow> rows) {
    Matcher m = YEAR_FORMAT.matcher(rows.get(0).toString());
    // year of the operations can be on line 0 or 1
    if (!m.find()) {
      rows.remove(0);
      m = YEAR_FORMAT.matcher(rows.get(0).toString());
      m.find();
    }
    rows.remove(0);
    return m.group(1).replace(";", "");
  }
}