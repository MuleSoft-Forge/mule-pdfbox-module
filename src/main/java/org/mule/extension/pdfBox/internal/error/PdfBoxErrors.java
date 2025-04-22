package org.mule.extension.pdfBox.internal.error;

import org.mule.runtime.extension.api.error.ErrorTypeDefinition;

public enum PdfBoxErrors implements ErrorTypeDefinition<PdfBoxErrors> {

    INVALID_PAGE_RANGE,
    PDF_LOAD_FAILED,
    TEXT_EXTRACTION_FAILED,
    METADATA_EXTRACTION_FAILED,
    UNSUPPORTED_PDF_FORMAT,
    PROCESSING_ERROR,
    PDF_COMPRESSION_FAILED,
    IMAGE_COMPRESSION_FAILED,
    INVALID_PARAMETER,
    IO_ERROR;


    @Override
    public String toString() {
        return name();
    }
}