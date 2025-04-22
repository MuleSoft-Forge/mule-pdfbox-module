package org.mule.extension.pdfBox.internal.extension;

import org.mule.extension.pdfBox.internal.config.PdfBoxConfiguration;
import org.mule.extension.pdfBox.internal.error.PdfBoxErrors;
import org.mule.extension.pdfBox.internal.operation.PdfBoxOperations;
import org.mule.runtime.api.meta.Category;

import org.mule.runtime.extension.api.annotation.Extension;
import org.mule.runtime.extension.api.annotation.error.ErrorTypes;
import org.mule.sdk.api.annotation.Configurations;
import org.mule.sdk.api.annotation.JavaVersionSupport;
import org.mule.sdk.api.annotation.MinMuleVersion;
import org.mule.sdk.api.annotation.Operations;
import org.mule.sdk.api.annotation.dsl.xml.Xml;
import org.mule.sdk.api.meta.JavaVersion;

/**
 * Main class for the MuleSoftForge Apache PDFBox Connector Extension.
 *
 * <p>This class is the entry point for the connector and is used to declare:
 * <ul>
 *   <li>Basic metadata such as the name and XML prefix</li>
 *   <li>Associated configuration class</li>
 *   <li>Supported Java version and minimum Mule runtime version</li>
 *   <li>Available operations class</li>
 *   <li>Declared error types</li>
 * </ul>
 *
 * <p>This extension leverages the Apache PDFBox library to perform PDF-related
 * utilities, such as text extraction, page filtering, compression, and rotation.
 *
 * @author Open Source
 */
@Extension(
        name = "MuleSoftForge Apache PDFBox",             // The display name of the extension
        category = Category.COMMUNITY                     // Community category in Anypoint Studio
)
@Xml(prefix = "pdfbox")                                // XML prefix used in Mule config files
@Configurations(PdfBoxConfiguration.class)             // Configuration class
@JavaVersionSupport({ JavaVersion.JAVA_17 })           // Java version support
@MinMuleVersion("4.9.0")                               // Minimum Mule runtime version required
@Operations(PdfBoxOperations.class)                    // Class containing operations
@ErrorTypes(PdfBoxErrors.class)                        // Declares reusable error types
public class PdfBoxExtension {
    // No implementation is needed here. Mule SDK uses annotations to wire everything.
}
