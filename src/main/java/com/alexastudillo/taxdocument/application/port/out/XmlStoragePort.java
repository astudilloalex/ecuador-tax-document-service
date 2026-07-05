package com.alexastudillo.taxdocument.application.port.out;

import com.alexastudillo.taxdocument.domain.taxdocument.AccessKey;
import java.util.Optional;

public interface XmlStoragePort {
    StoredXml storeGeneratedXml(AccessKey accessKey, byte[] content, String contentDigest);

    Optional<StoredXml> findGeneratedXml(AccessKey accessKey);

    record StoredXml(AccessKey accessKey, String contentDigest, long contentLength) {
    }
}
