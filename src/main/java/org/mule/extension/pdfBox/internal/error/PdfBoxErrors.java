/*
 * Copyright 2025 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.pdfBox.internal.error;

import org.mule.runtime.extension.api.error.ErrorTypeDefinition;

public enum PdfBoxErrors implements ErrorTypeDefinition<PdfBoxErrors> {

    PDF_INVALID_PAGE_RANGE,
    PDF_LOAD_FAILED,
    PDF_TEXT_EXTRACTION_FAILED,
    PDF_METADATA_EXTRACTION_FAILED,
    PDF_UNSUPPORTED_PDF_FORMAT,
    PDF_PROCESSING_ERROR,
    PDF_COMPRESSION_FAILED,
    PDF_IMAGE_COMPRESSION_FAILED,
    PDF_INVALID_PARAMETER,
    PDF_IO_ERROR;


    @Override
    public String toString() {
        return name();
    }
}