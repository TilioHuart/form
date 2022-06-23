package fr.openent.form.core.constants;

import java.util.Arrays;
import java.util.List;

public class FolderIds {
    public static final int ID_ROOT_FOLDER = 1;
    public static final int ID_SHARED_FOLDER = 2;
    public static final int ID_ARCHIVED_FOLDER = 3;
    public static final List<Integer> BASE_FOLDER_IDS = Arrays.asList(ID_ROOT_FOLDER, ID_SHARED_FOLDER, ID_ARCHIVED_FOLDER);
    public static final List<Integer> FORBIDDEN_FOLDER_IDS = Arrays.asList(ID_SHARED_FOLDER, ID_ARCHIVED_FOLDER);

    private FolderIds() {
        throw new IllegalStateException("Utility class");
    }
}

