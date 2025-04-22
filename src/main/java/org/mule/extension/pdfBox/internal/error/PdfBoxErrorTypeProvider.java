package org.mule.extension.pdfBox.internal.error;

import org.mule.runtime.extension.api.annotation.error.ErrorTypeProvider;
import org.mule.runtime.extension.api.error.ErrorTypeDefinition;

import java.util.HashSet;
import java.util.Set;

public class PdfBoxErrorTypeProvider implements ErrorTypeProvider {
    @Override
    public Set<ErrorTypeDefinition> getErrorTypes() {
        HashSet<ErrorTypeDefinition> errors = new HashSet<>();
        errors.add(PdfBoxErrors.INVALID_PAGE_RANGE);
        errors.add(PdfBoxErrors.PDF_LOAD_FAILED);
        errors.add(PdfBoxErrors.TEXT_EXTRACTION_FAILED);
        errors.add(PdfBoxErrors.METADATA_EXTRACTION_FAILED);
        errors.add(PdfBoxErrors.UNSUPPORTED_PDF_FORMAT);
        errors.add(PdfBoxErrors.IO_ERROR);
        errors.add(PdfBoxErrors.PROCESSING_ERROR);
        errors.add(PdfBoxErrors.PDF_COMPRESSION_FAILED);
        errors.add(PdfBoxErrors.IMAGE_COMPRESSION_FAILED);
        errors.add(PdfBoxErrors.INVALID_PARAMETER);
        return errors;
    }
}