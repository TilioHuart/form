package fr.openent.formulaire.helpers.folder_exporter;

import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.zip.Deflater;

import io.vertx.core.Promise;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.storage.Storage;
import org.entcore.common.utils.Zip;

import io.vertx.core.Future;
import io.vertx.core.file.FileSystem;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;

import static fr.openent.form.core.constants.Fields.*;

public class FolderExporterZip extends FolderExporter {
    public static final Logger log = LoggerFactory.getLogger(FolderExporterZip.class);

    public static class ZipContext extends FolderExporterContext {
        final public String zipFullPath;
        final String zipName;
        final String baseName;
        final String rootBase;

        public ZipContext(String rootBase, String basePath, String baseName) {
            super(basePath);
            this.rootBase = rootBase;
            this.baseName = baseName;
            this.zipName = this.baseName + ".zip";
            // basePath is the root folder => so up and set filename
            this.zipFullPath = Paths.get(this.basePath).resolve("..").resolve(this.zipName).normalize().toString();
        }
    }

    private Future<JsonObject> createZip(ZipContext context) {
        Promise<JsonObject> promise = Promise.promise();
        Zip.getInstance().zipFolder(context.basePath, context.zipFullPath, true, Deflater.NO_COMPRESSION, res -> {
            if (OK.equals(res.body().getString(STATUS))) promise.complete(res.body());
            else promise.fail(res.body().getString(MESSAGE));
        });
        return promise.future();
    }

    public FolderExporterZip(Storage storage, FileSystem fs, boolean throwErrors) {
        super(storage, fs, throwErrors);
    }

    public Future<ZipContext> exportToZip(Optional<JsonObject> root, List<JsonObject> rows) {
        UUID uuid = UUID.randomUUID();
        String baseName = root.isPresent() ? root.get().getString(NAME, ARCHIVE) : ARCHIVE;
        String rootBase = Paths.get(System.getProperty("java.io.tmpdir"), uuid.toString()).normalize().toString();
        String basePath = Paths.get(System.getProperty("java.io.tmpdir"), uuid.toString(), baseName).normalize().toString();
        ZipContext context = new ZipContext(rootBase, basePath, baseName);
        return this.export(context, rows)
                .compose(res -> this.createZip(context))
                .map(res -> context);
    }

    public Future<Void> sendZip(HttpServerRequest req, ZipContext context) {
        Promise<Void> promise = Promise.promise();
        try {
            final HttpServerResponse resp = req.response();
            log.info("Sending zip file...");
            resp.putHeader("Content-Disposition", "attachment; filename=\"" + context.zipName + "\"");
            resp.putHeader("Content-Type", "application/octet-stream");
            resp.putHeader("Content-Description", "File Transfer");
            resp.putHeader("Content-Transfer-Encoding", "binary");
            resp.sendFile(context.zipFullPath, promise);
        }
        catch (java.lang.IllegalStateException e) {
            promise.complete();
        }

        return promise.future();
    }

    public Future<ZipContext> exportAndSendZip(JsonObject root, List<JsonObject> rows, HttpServerRequest req, boolean clean) {
        return this.exportToZip(Optional.ofNullable(root), rows)
                .compose(res -> this.sendZip(req, res).map((r)->res))
                .compose(res -> {
                    if (clean) return removeZip(res);
                    return Future.succeededFuture(res);
                });
    }

    public Future<ZipContext> removeZip(ZipContext context){
        Promise<ZipContext> promise = Promise.promise();
        this.fs.deleteRecursive(context.rootBase,true, resRmDir -> promise.complete());
        return promise.future();
    }
}
