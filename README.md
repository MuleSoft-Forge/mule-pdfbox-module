# mule-pdfbox-module
MuleSoft Apache PDFBox Module to Manipulate PDF
# MuleSoft PDF Utilities Connector (Java SDK)

A lightweight MuleSoft connector that enables PDF manipulation using Apache PDFBox. This module provides a set of high-performance operations to extract information, manipulate pages, and split documents inside Mule flows.

## 📦 Features

- **Extract PDF Info**: Retrieve metadata such as author, title, subject, and number of pages.
- **Extract Text by Page Range**: Extract visible text from a specified range of pages.
- **Filter Pages**: Remove blank pages and/or keep only selected page ranges.
- **Rotate Pages**: Rotate a range of pages clockwise or counterclockwise.
- **Split Pages**: Split a PDF into individual single-page PDF files.
- **Merge PDFs**: Merge array of PDFs into individual single PDF file.
- **Image To PDF**: Simple Image to PDF.

## 🧰 Built With

- [Apache PDFBox](https://pdfbox.apache.org/)
- MuleSoft Java SDK (for Mule 4)

## 🚀 Operations

### `extractPdfInfo`
**Description**: Extracts metadata and document properties.  
**Input**: PDF file as `InputStream`  
**Output**:
```json
{
  "title": "Sample",
  "author": "John Doe",
  "subject": "Contracts",
  "keywords": "MuleSoft,PDF",
  "version": "1.4",
  "encrypted": false,
  "numberOfPages": 5
}
```

---

### `extractTextByPageRange`
**Description**: Extracts plain text from a specified page range.  
**Inputs**:
- PDF `InputStream`
- Optional `startPage` and `endPage`  
  **Output**: Extracted text as `String`

---

### `filterPages`
**Description**: Removes blank pages and/or filters based on page range.  
**Inputs**:
- PDF `InputStream`
- `removeBlankPages` (boolean)
- Optional `startPage` and `endPage`  
  **Output**: Filtered PDF as `InputStream`

---

### `rotatePages`
**Description**: Rotates a specific range of pages clockwise or counterclockwise.  
**Inputs**:
- PDF `InputStream`
- `startPage`, `endPage`
- `clockwise` (boolean)  
  **Output**: Rotated PDF as `InputStream`

---

### `splitPages`
**Description**: Splits a multi-page PDF into a list of single-page PDF files.  
**Input**: PDF `InputStream`  
**Output**: List of `InputStream`s, one per page

---

### `mergePDFs`
**Description**: Merge array of PDFs into individual single PDF file.  
**Input**: PDF Array of `InputStream`s, one per PDF
**Output**: PDF `InputStream`

---

### `imageToPdf`
**Description**: Convert an Image to PDF.  
**Input**: An Image
**Output**: PDF `InputStream`

---

## 📂 Usage

This connector is designed for use in Mule 4 Java SDK-based modules. Register the operations in your `Extension` class and call them from flows or other operations using standard SDK syntax.

---

## ⚠️ Limitations

- No image compression or DPI downsampling.
- Does not preserve digital signatures if present.
- Very large PDFs may consume significant memory during processing.

---
