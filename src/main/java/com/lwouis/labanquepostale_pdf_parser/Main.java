package com.lwouis.labanquepostale_pdf_parser;

import java.io.IOException;
import java.net.URISyntaxException;
import java.text.ParseException;

class Main {
  public static void main(String[] args) throws IOException, URISyntaxException, ParseException {
    LaBanquePostalePdfParser.generateConsolidatedReportOfOperations("./input/", "./output/report.csv");
  }
}
