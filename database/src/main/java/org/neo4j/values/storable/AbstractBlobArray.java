package org.neo4j.values.storable;

import cn.pandadb.commons.blob.Blob;

public abstract class AbstractBlobArray extends NonPrimitiveArray<Blob> {
    Blob[] _blobs;

    AbstractBlobArray(Blob[] blobs) {
        this._blobs = blobs;
    }

    public Blob[] value() {
        return this._blobs;
    }
}
