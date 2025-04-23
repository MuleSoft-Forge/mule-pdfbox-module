/*
 * Copyright 2025 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.pdfBox.internal.error;

import org.mule.runtime.extension.api.annotation.error.ErrorTypeProvider;
import org.mule.runtime.extension.api.error.ErrorTypeDefinition;

import java.util.HashSet;
import java.util.Set;

public class PdfBoxErrorTypeProvider implements ErrorTypeProvider {
    @Override
    public Set<ErrorTypeDefinition> getErrorTypes() {
        HashSet<ErrorTypeDefinition> errors = new HashSet<>();
        errors.add(PdfBoxErrors.PDF_INVALID_PAGE_RANGE);
        errors.add(PdfBoxErrors.PDF_LOAD_FAILED);
        errors.add(PdfBoxErrors.PDF_TEXT_EXTRACTION_FAILED);
        errors.add(PdfBoxErrors.PDF_METADATA_EXTRACTION_FAILED);
        errors.add(PdfBoxErrors.PDF_UNSUPPORTED_PDF_FORMAT);
        errors.add(PdfBoxErrors.PDF_IO_ERROR);
        errors.add(PdfBoxErrors.PDF_PROCESSING_ERROR);
        errors.add(PdfBoxErrors.PDF_COMPRESSION_FAILED);
        errors.add(PdfBoxErrors.PDF_IMAGE_COMPRESSION_FAILED);
        errors.add(PdfBoxErrors.PDF_INVALID_PARAMETER);
        return errors;
    }
}