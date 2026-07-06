package com.alexastudillo.taxdocument.application.port.out;

import com.alexastudillo.taxdocument.domain.taxdocument.AccessKey;
import io.smallrye.mutiny.Uni;
import java.util.Optional;

public interface XmlStoragePort {
    Uni<StoredXml> storeGeneratedXml(AccessKey accessKey, byte[] content, String contentDigest);

    Uni<Optional<StoredXml>> findGeneratedXml(AccessKey accessKey);

    record StoredXml(AccessKey accessKey, String contentDigest, long contentLength) {
    }
}
