package com.lwouis.labanquepostale_pdf_parser;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.util.Date;

public class Operation {
  private final Date date;
  private final BigDecimal amount;
  private String description;

  public Operation(Date date, String description, BigDecimal amount) {
    this.date = date;
    this.description = "\"" + description + "\"";
    this.amount = amount;
  }

  public void appendDescription(String descriptionToAppend) {
    description = description.substring(0, description.length() - 1) + "\n" + descriptionToAppend + "\"";
  }

  public String toCsv(DateFormat dateFormat) {
    return String.format("%s;%s;%s", amount.toString(), dateFormat.format(date), description);
  }
}
