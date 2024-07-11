package fr.openent.formulaire.helpers.upload_file;

import fr.wseduc.webutils.DefaultAsyncResult;
import io.vertx.core.*;
import io.vertx.core.file.FileSystem;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.storage.Storage;
import org.entcore.common.utils.FileUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static fr.wseduc.webutils.Utils.isNotEmpty;

public class FileHelper {
    private static final Logger log = LoggerFactory.getLogger(FileHelper.class);

    private FileHelper() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * This method will fetch all uploaded files from your {@link HttpServerRequest} request and upload them into your
     * storage and return each of them an object {@link Attachment}
     * <p>
     *  <b>WARNING</b><br/>
     *  Must SPECIFY a custom header where you can define a number to decide whether or not your upload should finish
     *  and complete (e.g adding "Files" as custom header as key and its value the number of file to loop/fetch will
     *  allow your uploadHandler to trigger n callback with different upload object)
     * </p>
     *
     * @param nbFilesToUpload   the total number of files expected to be uploaded
     * @param request       request HttpServerRequest
     * @param storage       Storage vertx
     * @param vertx    Vertx vertx
     *
     * @return list of {@link Attachment} (and all your files will be uploaded)
     * (process will continue in background to stream all these files in your storage)
     */
    public static Future<List<Attachment>> uploadMultipleFiles(int nbFilesToUpload, HttpServerRequest request, Storage storage, Vertx vertx) {
        request.response().setChunked(true);
        request.setExpectMultipart(true);
        Promise<List<Attachment>> promise = Promise.promise(); // Promise to sent inserted files
        AtomicBoolean responseSent= new AtomicBoolean();
        responseSent.set(false);

        // Return empty arrayList if no header is sent (meaning no files to upload)
        if (nbFilesToUpload == 0) {
            promise.complete(new ArrayList<>());
            return promise.future();
        }

        List<String> fileIds = new ArrayList<>();
        AtomicReference<List<String>> pathIds = new AtomicReference<>();
        for (int i = 0 ; i < nbFilesToUpload; i++){
            fileIds.add(UUID.randomUUID().toString());
        }

        request.pause();  // Pause the request to create folders in storage before putting files in it

        // We define the exception handler
        request.exceptionHandler(event -> {
            log.error("[Formulaire@uploadMultipleFiles] An error has occurred during http request process : " + event.getMessage());
            promise.fail(event.getMessage());
        });

        // We define the upload handler and insert files into storage
        AtomicInteger incrementFile = new AtomicInteger(0);
        List<Attachment> listMetadata = new ArrayList<>();
        request.uploadHandler(upload -> {
            String finalPath = pathIds.get().get(incrementFile.get());
            final JsonObject metadata = FileUtils.metadata(upload);
            listMetadata.add(new Attachment(fileIds.get(incrementFile.get()), new Metadata(metadata)));
            upload.streamToFileSystem(finalPath);
            incrementFile.set(incrementFile.get() + 1);

            upload.exceptionHandler(err -> {
                log.error("[Formulaire@uploadMultipleFiles] An exception has occurred during http upload process : " + err.getMessage());
                promise.fail(err.getMessage());
            });
            upload.endHandler(aVoid -> {
                if (incrementFile.get() == nbFilesToUpload && !responseSent.get()) {
                    responseSent.set(true);
                    for(Attachment at : listMetadata){
                        log.info(at.id());
                    }
                    promise.complete(listMetadata);
                }
            });
        });

        // Folders creation
        String path  = " ";
        List<Future<String>> makeFolders = new ArrayList<>();
        for (int i = 0; i < fileIds.size(); i++) {
            makeFolders.add(makeFolder(storage, vertx, fileIds, path, i));
        }

        Future.all(makeFolders)
            .onSuccess(success -> {
                pathIds.set(success.list());
                request.resume(); // once the folders created we resume request to get to "uploaded" status
            })
            .onFailure(failure -> promise.fail(failure.getMessage()));

        return promise.future();
    }

    private static Future<String> makeFolder(Storage storage, Vertx vertx, List<String> fileIds, String path, int i) {
        Promise<String> promise = Promise.promise();
        try {
            path = getFilePath(fileIds.get(i), storage.getBucket());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        String finalPath = path;
        mkdirsIfNotExists(vertx.fileSystem(), path, event -> {
            if (event.succeeded()) {
                promise.complete(finalPath);
            } else {
                promise.fail("mkdir.error: ");
            }
        });
        return promise.future();
    }

    private static String getFilePath(String file, final String bucket) throws FileNotFoundException {
        if (isNotEmpty(file)) {
            final int startIdx = file.lastIndexOf(File.separatorChar) + 1;
            final int extIdx = file.lastIndexOf('.');
            String filename = (extIdx > 0) ? file.substring(startIdx, extIdx) : file.substring(startIdx);
            if (isNotEmpty(filename)) {
                final int l = filename.length();
                if (l < 4) {
                    filename = "0000".substring(0, 4 - l) + filename;
                }
                return bucket + filename.substring(l - 2) + File.separator + filename.substring(l - 4, l - 2) +
                        File.separator + filename;
            }

        }
        throw new FileNotFoundException("Invalid file : " + file);
    }

    private static void mkdirsIfNotExists(FileSystem fileSystem, String path, final Handler<AsyncResult<Void>> handler) {
        final String dir = org.entcore.common.utils.FileUtils.getParentPath(path);
        fileSystem.exists(dir, event -> {
            if (event.succeeded()) {
                if (Boolean.FALSE.equals(event.result())) {
                    fileSystem.mkdirs(dir, handler);
                } else {
                    handler.handle(new DefaultAsyncResult<>((Void) null));
                }
            } else {
                handler.handle(new DefaultAsyncResult<>(event.cause()));
            }
        });
    }
}
