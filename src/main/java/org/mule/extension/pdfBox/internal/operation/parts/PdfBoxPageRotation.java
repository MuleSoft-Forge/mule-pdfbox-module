/*
 * Copyright 2025 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.pdfBox.internal.operation.parts;

public enum PdfBoxPageRotation {
    ROTATION_90("90"),
    ROTATION_180("180"),
    ROTATION_270("270");

    private final int value;

    PdfBoxPageRotation(String value) {
        this.value = Integer.parseInt(value);
    }

    public int getValue() {
        return value;
    }
}