/*
 * Copyright 2025 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.pdfBox.internal.operation;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSName;
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
import org.mule.extension.pdfBox.internal.operation.parts.PdfBoxPdfOptions;
import org.mule.extension.pdfBox.internal.operation.parts.PdfBoxRemoveBlankOption;
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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
            @Summary("The rotation angle in degrees (e.g., 90, 180, 270).")
            @Optional(defaultValue = "90") int rotationAngle,

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
                    page.setRotation(rotationAngle);
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
     * Utility method to read all bytes from an InputStream (Java 8 compatible).
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

    private boolean isPageBlank(PDDocument doc, PDPage page) throws IOException {
        int pageIndex = doc.getPages().indexOf(page) + 1;

        // 1. Check for visible text
        PDFTextStripper stripper = new PDFTextStripper();
        String text = stripper.getText(doc);

        // 2. Check for annotations or images
        PDResources resources = page.getResources();
        if (resources != null) {
            Iterable<COSName> xObjectNames = resources.getXObjectNames();
            int count = 0;

            // Iterate over the Iterable to count XObjects
            for (COSName cosName : xObjectNames) {
                count++;
            }

            // If there are any XObjects (images, graphics), the page is not blank
            if (count > 0) {
                return false; // The page is not blank because it contains XObjects
            }
        }

        return text.trim().isEmpty(); // Return true if text is empty
    }

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

}
