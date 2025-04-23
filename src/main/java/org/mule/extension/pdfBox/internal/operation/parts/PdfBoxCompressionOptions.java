/*
 * Copyright 2025 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.pdfBox.internal.operation.parts;


import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.annotation.param.display.Summary;


public class PdfBoxCompressionOptions {
  @Parameter
  @DisplayName("Image Quality")
  @Summary("JPEG compression quality (0.0 - 1.0, where 1.0 is highest quality and largest size).")
  @Optional(defaultValue = "0.7")
  private double imageQuality;

  @Parameter
  @DisplayName("Strip Metadata")
  @Summary("If true, removes metadata (author, title, etc.) to reduce file size.")
  @Optional(defaultValue = "true")
  private boolean stripMetadata;

  @Parameter
  @Optional(defaultValue = "150")
  @DisplayName("Target DPI")
  @Summary("The resolution to which images should be downsamplied (dots per inch). Common values: 72, 96, 150, 300.")
  private Integer dpi;

  public double getImageQuality() {
    return imageQuality;
  }

  public boolean getStripMetadata() {
    return stripMetadata;
  }

  public Integer getDpi() {
    return dpi;
  }
}

