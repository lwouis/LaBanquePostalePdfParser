package com.lwouis.labanquepostale_pdf_parser;

import java.util.ArrayList;

class PdfPageConfig {
  private final ArrayList<Integer> rowsToIgnore = new ArrayList<>();
  private boolean isLastPage = false;

  public ArrayList<Integer> getRowsToIgnore() {
    return rowsToIgnore;
  }

  public boolean isLastPage() {
    return isLastPage;
  }

  public void reachedLastPage() {
    isLastPage = true;
  }

  public void addRowToIgnore(Integer rowToIgnore) {
    this.rowsToIgnore.add(rowToIgnore);
  }
}
