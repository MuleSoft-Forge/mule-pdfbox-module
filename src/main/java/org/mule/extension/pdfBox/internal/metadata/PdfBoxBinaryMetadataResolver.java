/*
 * Copyright 2025 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.pdfBox.internal.metadata;

import org.mule.extension.pdfBox.internal.extension.PdfBoxExtension;
import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.metadata.MetadataContext;
import org.mule.runtime.api.metadata.MetadataResolvingException;
import org.mule.runtime.api.metadata.resolving.InputTypeResolver;
import org.mule.runtime.api.metadata.resolving.OutputTypeResolver;

public class PdfBoxBinaryMetadataResolver implements InputTypeResolver<Void>, OutputTypeResolver<Void> {

    @Override
    public String getCategoryName() {
        return PdfBoxExtension.class.getName();
    }
    
    @Override
    public String getResolverName() {
        return PdfBoxBinaryMetadataResolver.class.getName();
    }

    @Override
    public MetadataType getOutputType(MetadataContext context, Void key)
            throws MetadataResolvingException, ConnectionException {
        return context.getTypeBuilder().binaryType().build();
    }

    @Override
    public MetadataType getInputMetadata(MetadataContext context, Void key)
            throws MetadataResolvingException, ConnectionException {
        
        return context.getTypeBuilder().binaryType().build();
    }

}
