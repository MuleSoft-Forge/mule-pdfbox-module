/*
 * Copyright 2025 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.pdfBox.internal.operation;


import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.io.RandomAccessRead;
import org.apache.pdfbox.io.RandomAccessReadBuffer;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.apache.pdfbox.multipdf.Splitter;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.text.PDFTextStripper;
import org.jetbrains.annotations.NotNull;
import org.mule.extension.pdfBox.api.PdfBoxFileAttributes;
import org.mule.extension.pdfBox.internal.error.PdfBoxErrorTypeProvider;
import org.mule.extension.pdfBox.internal.error.PdfBoxErrors;
import org.mule.extension.pdfBox.internal.metadata.PdfBoxBinaryMetadataResolver;
import org.mule.extension.pdfBox.internal.operation.parts.PdfBoxPageRotation;
import org.mule.extension.pdfBox.internal.operation.parts.PdfBoxPdfOptions;
import org.mule.extension.pdfBox.internal.operation.parts.PdfBoxRemoveBlankOption;
import org.mule.runtime.extension.api.annotation.Expression;
import org.mule.runtime.extension.api.annotation.error.Throws;
import org.mule.runtime.extension.api.annotation.metadata.TypeResolver;
import org.mule.runtime.extension.api.annotation.param.Content;
import org.mule.runtime.extension.api.annotation.param.MediaType;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.annotation.param.display.Example;
import org.mule.runtime.extension.api.annotation.param.display.Summary;
import org.mule.runtime.extension.api.exception.ModuleException;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.extension.api.runtime.streaming.StreamingHelper;
import org.slf4j.Logger;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.mule.runtime.api.meta.ExpressionSupport.NOT_SUPPORTED;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Contains all operations exposed by the Apache PDFBox MuleSoft connector.
 * Operations include reading metadata, extracting text, filtering pages,
 * rotating PDF documents.
 */
public class PdfBoxOperations {

    private static final Logger LOGGER = getLogger(PdfBoxOperations.class);

    /**
     * Returns the number of pages and basic file metadata from the given PDF.
     *
     * @param pdfFile         Binary content of the PDF
     * @param streamingHelper MuleSoft streaming helper
     * @return Result containing original PDF and extracted metadata as attributes
     */
    @MediaType(value = MediaType.APPLICATION_OCTET_STREAM)
    @DisplayName("Apache PDFBox - Get Info")
    @Summary("Extracts the number of pages and file size from a PDF document.")
    @Throws(PdfBoxErrorTypeProvider.class)
    public Result<InputStream, PdfBoxFileAttributes> getPdfInfo(
            @NotNull @DisplayName("PDF File [Binary]")
            @Content @TypeResolver(PdfBoxBinaryMetadataResolver.class)
            InputStream pdfFile,
            StreamingHelper streamingHelper) throws IOException {

//        byte[] pdfBytes = readAllBytes(pdfFile);
        byte[] pdfBytes = toByteArray(pdfFile);

        long pdfSize = pdfBytes.length;

        try (PDDocument pdfDoc = Loader.loadPDF(pdfBytes)) {
            PdfBoxFileAttributes attributes = extractPdfMetadata(pdfDoc, pdfSize);

            LOGGER.info("Extracted PDF Info - Pages: {}, Size: {}, Title: {}, Author: {}",
                    pdfDoc.getNumberOfPages(), pdfSize, attributes.getTitle(), attributes.getAuthor());

            return Result.<InputStream, PdfBoxFileAttributes>builder()
                    .output(new ByteArrayInputStream(pdfBytes))
                    .mediaType(org.mule.runtime.api.metadata.MediaType.parse("application/octet-stream"))
                    .attributes(attributes)
                    .build();
        } catch (IOException e) {
            throw new ModuleException("Failed to load PDF document. It may be corrupt or invalid.", PdfBoxErrors.PDF_LOAD_FAILED, e);
        }
    }

    /**
     * Extracts text content from selected pages in a PDF based on a user-defined page range.
     *
     * @param pdfFile         Binary content of the PDF
     * @param pageRange       Comma-separated list of individual pages and ranges (e.g., 1,2,5-7)
     * @param streamingHelper MuleSoft streaming helper
     * @return Extracted text and metadata as attributes
     */
    @MediaType(value = MediaType.TEXT_PLAIN)
    @DisplayName("Apache PDFBox - Extract Text")
    @Summary("Extracts text from specific pages/ranges in a PDF, e.g., 2,4,9-11.")
    @Throws(PdfBoxErrorTypeProvider.class)
    public Result<String, PdfBoxFileAttributes> extractTextWithPageRange(
            @NotNull @DisplayName("PDF File [Binary]")
            @Content @TypeResolver(PdfBoxBinaryMetadataResolver.class)
            InputStream pdfFile,

            @DisplayName("Page Range")
            @Summary("Comma-separated list of pages or ranges (e.g., 2,4,5,9-12,15).")
            @Example("1,2,4,6-12")
            @Optional String pageRange,

            StreamingHelper streamingHelper) throws IOException {

        //byte[] pdfBytes = readAllBytes(pdfFile);
        byte[] pdfBytes = toByteArray(pdfFile);
        long pdfSize = pdfBytes.length;

        try (PDDocument pdfDoc = Loader.loadPDF(pdfBytes)) {
            int totalPages = pdfDoc.getNumberOfPages();
            Set<Integer> pageSet = parsePageRange(pageRange, totalPages);

            PDFTextStripper stripper = new PDFTextStripper();
            StringBuilder combinedText = new StringBuilder();

            for (Integer page : pageSet) {
                stripper.setStartPage(page);
                stripper.setEndPage(page);
                try {
                    combinedText.append(stripper.getText(pdfDoc)).append("\n");
                } catch (IOException e) {
                    throw new ModuleException("Error extracting text from page " + page, PdfBoxErrors.PDF_TEXT_EXTRACTION_FAILED, e);
                }
            }

            PdfBoxFileAttributes attributes = extractPdfMetadata(pdfDoc, pdfSize);

            LOGGER.info("Extracted text from pages: {}", pageSet);

            return Result.<String, PdfBoxFileAttributes>builder()
                    .output(combinedText.toString())
                    .mediaType(org.mule.runtime.api.metadata.MediaType.TEXT)
                    .attributes(attributes)
                    .build();
        } catch (IOException e) {
            throw new ModuleException("Failed to load PDF document. It may be corrupt or invalid.", PdfBoxErrors.PDF_LOAD_FAILED, e);
        }
    }

    /**
     * Filters a PDF to remove blank pages or include only selected ranges.
     *
     * @param pdfFile         Binary content of the PDF
     * @param options         Parameter group for filtering pages (blank removal or range)
     * @param streamingHelper MuleSoft streaming helper
     * @return New PDF stream with filtered pages and metadata as attributes
     */
    @MediaType(value = MediaType.APPLICATION_OCTET_STREAM)
    @Throws(PdfBoxErrorTypeProvider.class)
    @DisplayName("Apache PDFBox - Filter Pages")
    @Summary("Removes blank pages and/or keeps only selected page ranges. Returns a filtered PDF.")
    public Result<InputStream, PdfBoxFileAttributes> filterPages(
            @NotNull @DisplayName("PDF File [Binary]")
            @Content @TypeResolver(PdfBoxBinaryMetadataResolver.class)
            InputStream pdfFile,

            @ParameterGroup(name = "Filter Options [Only one choice allowed]") PdfBoxPdfOptions options,

            StreamingHelper streamingHelper) throws IOException {

        // byte[] pdfBytes = readAllBytes(pdfFile);
        byte[] pdfBytes = toByteArray(pdfFile);

        try (PDDocument original = Loader.loadPDF(pdfBytes);
             PDDocument filtered = new PDDocument()) {

            int totalPages = original.getNumberOfPages();
            Set<Integer> keepPages = parsePageRange(options.getPageRange(), totalPages);
            boolean removeBlanks = PdfBoxRemoveBlankOption.isYes(options.getRemoveBlankPages());

            for (int i = 0; i < totalPages; i++) {
                int pageIndex = i + 1;

                if (!keepPages.contains(pageIndex)) {
                    continue;
                }

                PDPage page = original.getPage(i);
                if (removeBlanks && isPageBlank(original, page)) {
                    continue;
                }

                filtered.addPage(page);
            }

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            filtered.save(outputStream);

            PdfBoxFileAttributes attributes = extractPdfMetadata(filtered, outputStream.size());

            return Result.<InputStream, PdfBoxFileAttributes>builder()
                    .output(new ByteArrayInputStream(outputStream.toByteArray()))
                    .mediaType(org.mule.runtime.api.metadata.MediaType.parse("application/octet-stream"))
                    .attributes(attributes)
                    .build();
        } catch (IOException e) {
            throw new ModuleException("Failed to process PDF: " + e.getMessage(), PdfBoxErrors.PDF_PROCESSING_ERROR, e);
        }
    }

    /**
     * Rotates specific pages in a PDF to a defined angle.
     *
     * @param pdfFile         Binary content of the PDF
     * @param pageRange       Comma-separated page list/ranges to rotate
     * @param rotationAngle   Degrees to rotate (e.g., 90, 180, 270)
     * @param streamingHelper MuleSoft streaming helper
     * @return Rotated PDF with updated metadata
     */
    @MediaType(value = MediaType.APPLICATION_OCTET_STREAM)
    @DisplayName("Apache PDFBox - Rotate Pages")
    @Summary("Rotates specific pages in a PDF document based on the provided page range and rotation angle.")
    @Throws(PdfBoxErrorTypeProvider.class)
    public Result<InputStream, PdfBoxFileAttributes> rotatePdfPages(
            @NotNull @DisplayName("PDF File [Binary]")
            @Content @TypeResolver(PdfBoxBinaryMetadataResolver.class)
            InputStream pdfFile,

            @DisplayName("Page Range")
            @Summary("Comma-separated list of pages or ranges (e.g., 2,4,5,9-12,15).")
            @Example("1,2,4,6-12")
            @Optional String pageRange,

            @DisplayName("Rotation Angle")
            @Expression(NOT_SUPPORTED)
            @Summary("The rotation angle in degrees (e.g., 90, 180, 270).")
            PdfBoxPageRotation rotationAngle,


            StreamingHelper streamingHelper) throws IOException {

        //byte[] pdfBytes = readAllBytes(pdfFile);
        byte[] pdfBytes = toByteArray(pdfFile);

        long pdfSize = pdfBytes.length;
        try (PDDocument pdfDoc = Loader.loadPDF(pdfBytes)) {
            int totalPages = pdfDoc.getNumberOfPages();
            Set<Integer> pageSet = parsePageRange(pageRange, totalPages);

            // Rotate the specified pages
            for (Integer pageNumber : pageSet) {
                if (pageNumber >= 1 && pageNumber <= totalPages) {
                    PDPage page = pdfDoc.getPage(pageNumber - 1); // Page numbers are 1-based, so adjust
                    page.setRotation(rotationAngle.getValue());
                }
            }

            // Save the rotated document to a byte array
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            pdfDoc.save(outputStream);

            // Create file attributes for the rotated PDF
            PdfBoxFileAttributes attributes = new PdfBoxFileAttributes();
            attributes.setNumberOfPages(pdfDoc.getNumberOfPages());
            attributes.setPdfSize(outputStream.size());

            return Result.<InputStream, PdfBoxFileAttributes>builder()
                    .output(new ByteArrayInputStream(outputStream.toByteArray()))
                    .mediaType(org.mule.runtime.api.metadata.MediaType.parse("application/octet-stream"))
                    .attributes(attributes)
                    .build();

        } catch (IOException e) {
            throw new ModuleException("Failed to load or process PDF document.", PdfBoxErrors.PDF_LOAD_FAILED, e);
        }
    }


    /**
     * Reads all bytes from the given InputStream into a byte array.
     *
     * @param input the InputStream to read
     * @return a byte array containing the input stream's data
     * @throws IOException if an I/O error occurs while reading
     */
    private static byte[] toByteArray(InputStream input) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int nRead;
        byte[] data = new byte[4096];
        while ((nRead = input.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }
        return buffer.toByteArray();
    }

//    private byte[] readAllBytes(InputStream input) {
//        Objects.requireNonNull(input, "Input stream cannot be null");
//        try {
//            return input.readAllBytes();
//        } catch (IOException e) {
//            throw new ModuleException("Failed to read input PDF stream.", PdfBoxErrors.PDF_IO_ERROR, e);
//        }
//    }

    /**
     * Parses a string representing individual pages and page ranges (e.g., "1,3-5,7") into a set of page numbers.
     *
     * @param pageRange   a comma-separated list of pages and ranges
     * @param totalPages  the total number of pages in the document for bounds checking
     * @return a Set of integers representing all selected page numbers
     * @throws ModuleException if the range format is invalid or out of bounds
     */
    private Set<Integer> parsePageRange(String pageRange, int totalPages) {
        Set<Integer> pages = new TreeSet<>();

        if (pageRange == null || pageRange.trim().isEmpty()) {
            for (int i = 1; i <= totalPages; i++) {
                pages.add(i);
            }
            return pages;
        }

        String sanitized = pageRange.replaceAll("\\s", "");
        String[] segments = sanitized.split(",");
        Pattern pattern = Pattern.compile("(\\d+)(?:-(\\d+))?");

        for (String segment : segments) {
            Matcher matcher = pattern.matcher(segment);

            if (!matcher.matches()) {
                throw new ModuleException(
                        "Invalid page range segment: \"" + segment + "\". Expected format like 2,4,9-12.",
                        PdfBoxErrors.PDF_INVALID_PAGE_RANGE
                );
            }

            int start = Integer.parseInt(matcher.group(1));
            int end = matcher.group(2) != null ? Integer.parseInt(matcher.group(2)) : start;

            if (start > end) {
                throw new ModuleException(
                        "Invalid page range: \"" + segment + "\". Start page must not be greater than end page.",
                        PdfBoxErrors.PDF_INVALID_PAGE_RANGE
                );
            }

            for (int i = start; i <= end; i++) {
                if (i >= 1 && i <= totalPages) {
                    pages.add(i);
                }
            }
        }

        return pages;
    }

    /**
     * Checks whether a given page in the PDF is blank (i.e., no visible text or image XObjects).
     *
     * @param doc  the parent PDDocument for context
     * @param page the page to analyze
     * @return true if the page has no text or images; false otherwise
     * @throws IOException if PDFBox fails to access or interpret the page
     */
    private boolean isPageBlank(PDDocument doc, PDPage page) throws IOException {
        int pageIndex = doc.getPages().indexOf(page) + 1;

        // 1. Check for visible text on the specific page
        PDFTextStripper stripper = new PDFTextStripper();
        stripper.setStartPage(pageIndex);
        stripper.setEndPage(pageIndex);
        String text = stripper.getText(doc);

        if (!text.trim().isEmpty()) {
            return false; // Contains visible text
        }

        // 2. Check for image/graphic XObjects
        PDResources resources = page.getResources();
        if (resources != null) {
            try {
                Iterable<COSName> xObjectNames = resources.getXObjectNames();
                if (xObjectNames != null) {
                    for (COSName cosName : xObjectNames) {
                        return false; // At least one XObject found
                    }
                }
            } catch (Exception e) {
                LOGGER.warn("Error accessing XObject names on page {}: {}", pageIndex, e.getMessage());
            }
        }

        // 3. Check for annotations on the page
        if (page.getAnnotations() != null && !page.getAnnotations().isEmpty()) {
            return false;
        }

        // 4. Check for interactive form fields on the page
        if (doc.getDocumentCatalog().getAcroForm() != null &&
                doc.getDocumentCatalog().getAcroForm().getFields() != null &&
                !doc.getDocumentCatalog().getAcroForm().getFields().isEmpty()) {

            for (org.apache.pdfbox.pdmodel.interactive.form.PDField field : doc.getDocumentCatalog().getAcroForm().getFields()) {
                if (field.getWidgets() != null) {
                    for (org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationWidget widget : field.getWidgets()) {
                        if (widget.getPage() != null && widget.getPage().equals(page)) {
                            return false;
                        }
                    }
                }
            }
        }

        return true; // Page has no text, no images, no annotations, and no form fields
    }


    /**
     * Extracts standard metadata fields (title, author, dates, keywords) from a PDF document.
     *
     * @param pdfDoc  the loaded PDDocument
     * @param pdfSize the size in bytes of the original PDF stream
     * @return a populated PdfBoxFileAttributes instance
     * @throws ModuleException if metadata cannot be retrieved
     */
    private PdfBoxFileAttributes extractPdfMetadata(PDDocument pdfDoc, long pdfSize) {
        PDDocumentInformation info = pdfDoc.getDocumentInformation();
        if (info == null) {
            throw new ModuleException("Unable to extract PDF metadata.", PdfBoxErrors.PDF_METADATA_EXTRACTION_FAILED);
        }

        PdfBoxFileAttributes attributes = new PdfBoxFileAttributes();
        attributes.setNumberOfPages(pdfDoc.getNumberOfPages());
        attributes.setPdfSize(pdfSize);
        attributes.setTitle(info.getTitle());
        attributes.setAuthor(info.getAuthor());
        attributes.setSubject(info.getSubject());
        attributes.setKeywords(info.getKeywords());

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        if (info.getCreationDate() != null) {
            attributes.setCreationDate(sdf.format(info.getCreationDate().getTime()));
        }
        if (info.getModificationDate() != null) {
            attributes.setModificationDate(sdf.format(info.getModificationDate().getTime()));
        }

        return attributes;
    }



    /**
     * Splits a PDF document into multiple documents based on a page increment.
     * Compatible with Java 1.8.
     *
     * @param pdfFile         Binary content of the PDF.
     * @param pageIncrement   Number of pages per split document. Defaults to 1.
     * @param streamingHelper MuleSoft streaming helper (currently unused but good practice to include).
     * @return A List of InputStreams, each representing a split PDF document.
     * Attributes of the original PDF are returned.
     * @throws IOException If there's an error reading/processing the PDF.
     * @throws ModuleException If splitting fails or the increment is invalid.
     */
    @DisplayName("Apache PDFBox - Split Pages")
    @Summary("Splits the PDF into multiple documents, each containing the specified number of pages.")
    // Note: Returning List<InputStream> holds all split documents in memory.
    // Consider returning PagingProvider for better memory management with large files.
    @Throws(PdfBoxErrorTypeProvider.class)
    public Result<List<InputStream>, PdfBoxFileAttributes> splitPdfByIncrement(
            @NotNull @DisplayName("PDF File [Binary]")
            @Content @TypeResolver(PdfBoxBinaryMetadataResolver.class)
            InputStream pdfFile,

            @DisplayName("Page Increment")
            @Summary("Number of pages for each split document.")
            @Optional(defaultValue="1") // Default to splitting into single pages
            Integer pageIncrement,

            StreamingHelper streamingHelper) throws IOException {

        if (pageIncrement == null || pageIncrement <= 0) {
            throw new ModuleException("Page increment must be a positive integer.", PdfBoxErrors.PDF_PROCESSING_ERROR); // Or a more specific error
        }

        // Using the toByteArray method provided in the original class
        byte[] pdfBytes = toByteArray(pdfFile);
        long originalPdfSize = pdfBytes.length;
        List<InputStream> splitDocuments = new ArrayList<>();
        PdfBoxFileAttributes originalAttributes = null;

        PDDocument document = null; // Declare outside try-finally for closing scope
        try {
            document = Loader.loadPDF(pdfBytes);
            originalAttributes = extractPdfMetadata(document, originalPdfSize); // Get metadata before splitting
            int totalPages = document.getNumberOfPages();

            if (totalPages == 0) {
                LOGGER.warn("Input PDF has 0 pages. Returning empty list of split documents.");
                // Return empty list and original attributes (which will show 0 pages)
                return Result.<List<InputStream>, PdfBoxFileAttributes>builder()
                        .output(splitDocuments)
                        .attributes(originalAttributes)
                        .build();
            }

            Splitter splitter = new Splitter();
            // splitter.setStartPage(?); // Can set start if needed
            // splitter.setEndPage(?); // Can set end if needed
            splitter.setSplitAtPage(pageIncrement); // Set the increment

            List<PDDocument> splitPDDocs = splitter.split(document);
            List<Closeable> resourcesToClose = new ArrayList<>(); // Keep track of resources

            try {
                for (PDDocument splitDoc : splitPDDocs) {
                    resourcesToClose.add(splitDoc); // Add doc to be closed later
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    resourcesToClose.add(baos); // Add stream to be closed later
                    splitDoc.save(baos);
                    splitDocuments.add(new ByteArrayInputStream(baos.toByteArray()));
                    // Note: We don't close baos here; its content is needed for the ByteArrayInputStream.
                    // ByteArrayOutputStream close() is effectively a no-op anyway.
                    // We mainly need to ensure splitDoc is closed.
                }
            } catch (IOException e) {
                // If saving fails, still need to close resources created so far
                throw new ModuleException("Failed to save a split PDF document part.", PdfBoxErrors.PDF_PROCESSING_ERROR, e);
            } finally {
                // Close all split PDDocuments *after* processing
                for (Closeable resource : resourcesToClose) {
                    // Only close PDDocuments here, BAOS close is no-op
                    if (resource instanceof PDDocument) {
                        try {
                            resource.close();
                        } catch (IOException ce) {
                            LOGGER.error("Failed to close split PDDocument resource.", ce);
                            // Log and continue closing others
                        }
                    }
                }
            }

            LOGGER.info("Successfully split PDF into {} documents with increment {}", splitDocuments.size(), pageIncrement);

            return Result.<List<InputStream>, PdfBoxFileAttributes>builder()
                    .output(splitDocuments)
                    .attributes(originalAttributes)
                    .build();

        } catch (IOException e) {
            throw new ModuleException("Failed to load or split PDF document.", PdfBoxErrors.PDF_LOAD_FAILED, e); // Re-use or add PDF_SPLIT_FAILED
        } finally {
            // Ensure the main PDDocument is closed
            if (document != null) {
                try {
                    document.close();
                } catch (IOException e) {
                    LOGGER.error("Failed to close original PDF document.", e);
                    // Log error, but can't do much more here as an exception might already be propagating
                }
            }
        }
    }


    /**
     * Merges multiple PDF documents into a single PDF document.
     * <p>
     * This operation requires at least two PDF files. It is Java 8 compatible and works with PDFBox 3.0.4.
     * Each input PDF is converted to a {@link org.apache.pdfbox.io.RandomAccessReadBuffer} to comply
     * with PDFBox's input requirements.
     * <p>
     * The resulting merged PDF retains the content of all input documents and returns metadata
     * including total number of pages, size, and optional document properties like title or author.
     * Temporary in-memory buffers are closed after the operation completes.
     *
     * @param pdfFiles          An array of PDF InputStreams to merge. Must contain at least two PDFs.
     * @param streamingHelper   MuleSoft streaming helper (not used directly but required by signature).
     * @return A Result containing the merged PDF as an InputStream and extracted {@link PdfBoxFileAttributes}.
     * @throws IOException      If reading or writing the PDFs fails.
     * @throws ModuleException  If fewer than two PDFs are provided, or if the merge fails.
     */
    @DisplayName("Apache PDFBox - Merge PDFs")
    @Summary("Merges multiple PDF files into a single PDF document.")
    @MediaType(value = MediaType.APPLICATION_OCTET_STREAM)
    @Throws(PdfBoxErrorTypeProvider.class)
    public Result<InputStream, PdfBoxFileAttributes> mergePdfs(
            @Expression
            @Content
            @NotNull
            @DisplayName("PDF Files [List of Binary]") List<InputStream> pdfFiles,
            @Summary("An array of PDF files to merge. Must contain at least two PDFs.")
            StreamingHelper streamingHelper) throws IOException {

        if (pdfFiles.size() < 2) {
            throw new ModuleException("At least two PDF files are required for merging.", PdfBoxErrors.PDF_PROCESSING_ERROR);
        }

        ByteArrayOutputStream mergedOutputStream = new ByteArrayOutputStream();
        PDFMergerUtility merger = new PDFMergerUtility();
        merger.setDestinationStream(mergedOutputStream);

        List<RandomAccessRead> buffers = new ArrayList<>();
        try {
            for (InputStream pdfStream : pdfFiles) {
                byte[] bytes = toByteArray(pdfStream);
                RandomAccessRead rar = new RandomAccessReadBuffer(bytes);
                buffers.add(rar);
                merger.addSource(rar);
            }

            merger.mergeDocuments(null);

            byte[] mergedBytes = mergedOutputStream.toByteArray();

            // ? Extract metadata from the new merged document
            try (PDDocument mergedDoc = Loader.loadPDF(mergedBytes)) {
                PdfBoxFileAttributes attributes = extractPdfMetadata(mergedDoc, mergedBytes.length);

                return Result.<InputStream, PdfBoxFileAttributes>builder()
                        .output(new ByteArrayInputStream(mergedBytes))
                        .attributes(attributes)
                        .mediaType(org.mule.runtime.api.metadata.MediaType.parse("application/octet-stream"))
                        .build();
            }

        } catch (IOException e) {
            throw new ModuleException("Failed to merge PDF files.", PdfBoxErrors.PDF_PROCESSING_ERROR, e);
        } finally {
            for (RandomAccessRead rar : buffers) {
                try {
                    rar.close();
                } catch (IOException ioe) {
                    LOGGER.warn("Failed to close RandomAccessRead buffer: {}", ioe.getMessage());
                }
            }
        }
    }



}
