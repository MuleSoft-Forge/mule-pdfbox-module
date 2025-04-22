/*
 * Copyright 2025 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.pdfBox.internal.operation.parts;

public enum PdfBoxRemoveBlankOption {
    YES("yes");

    private final String value;

    PdfBoxRemoveBlankOption(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static boolean isYes(PdfBoxRemoveBlankOption option) {
        return YES.equals(option);
    }
}