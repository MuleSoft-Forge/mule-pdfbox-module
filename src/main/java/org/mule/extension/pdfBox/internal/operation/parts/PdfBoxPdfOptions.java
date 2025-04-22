package org.mule.extension.pdfBox.internal.operation.parts;

import org.mule.runtime.extension.api.annotation.param.ExclusiveOptionals;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.annotation.param.display.Example;
import org.mule.runtime.extension.api.annotation.param.display.Summary;

@ExclusiveOptionals(isOneRequired = true)
public class PdfBoxPdfOptions {

  public PdfBoxPdfOptions() {}

  public PdfBoxPdfOptions(PdfBoxRemoveBlankOption removeBlankPages, String pageRange) {
    this.removeBlankPages = removeBlankPages;
    this.pageRange = pageRange;
  }

  @Parameter
  @DisplayName("Remove Blank Pages")
  @Summary("Remove Blank Pages using PDFBox testing for text and images present on each page")
  @Optional
  private PdfBoxRemoveBlankOption removeBlankPages;

  @Parameter
  @DisplayName("Page Range")
  @Summary("Comma-separated list of pages or ranges (e.g., 2,4,5,9-12,15).")
  @Example("1,2,4,6-12")
  @Optional
  private String pageRange;

  public String getPageRange() {
    return pageRange;
  }

  public PdfBoxRemoveBlankOption getRemoveBlankPages() {
    return removeBlankPages;
  }
}

