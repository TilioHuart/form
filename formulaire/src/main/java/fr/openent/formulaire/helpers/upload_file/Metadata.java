package fr.openent.formulaire.helpers.upload_file;

import io.vertx.core.json.JsonObject;

import static fr.openent.form.core.constants.Fields.*;

public class Metadata {
    private String name;
    private String contentType;
    private String contentTransferEncoding;
    private String filename;
    private Integer size;
    private String charset;

    public Metadata(JsonObject metadata) {
        this.name = metadata.getString(NAME);
        this.contentType = metadata.getString(CONTENT_TYPE);
        this.contentTransferEncoding = metadata.getString(CONTENT_TRANSFER_ENCODING);
        this.filename = metadata.getString(FILENAME);
        this.size = metadata.getInteger(SIZE);
        this.charset = metadata.getString(CHARSET);
    }

    public Metadata(String oMetadata) {
        if (oMetadata != null) {
            JsonObject metadata = new JsonObject(oMetadata);
            this.name = metadata.getString(NAME);
            this.contentType = metadata.getString(CONTENT_TYPE);
            this.contentTransferEncoding = metadata.getString(CONTENT_TRANSFER_ENCODING);
            this.filename = metadata.getString(FILENAME);
            this.size = metadata.getInteger(SIZE);
            this.charset = metadata.getString(CHARSET);
        }
    }

    public JsonObject toJSON() {
        return new JsonObject()
                .put(NAME, this.name)
                .put(CONTENT_TYPE, this.contentType)
                .put(CONTENT_TRANSFER_ENCODING, this.contentTransferEncoding)
                .put(FILENAME, this.filename)
                .put(SIZE, this.size)
                .put(CHARSET, this.charset);
    }

    public String filename() {
        return filename;
    }
}
