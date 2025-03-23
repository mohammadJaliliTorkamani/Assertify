package org.example;

import java.io.File;
import java.nio.file.Paths;

import static org.example.Constants.BACKUP_DIR_NAME;
import static org.example.Constants.DATE_TIME_FORMAT;

public class Preprocessor {
    private final String recordsDirPath;
    private final String repositoriesDirName;

    public Preprocessor(String repositoriesDirName) {
        this.recordsDirPath = Utils.createRecordsDirectory(BACKUP_DIR_NAME, repositoriesDirName, DATE_TIME_FORMAT);
        this.repositoriesDirName = repositoriesDirName;
    }

    public boolean apply(InputRecord record) throws Exception {
        if (record.exists(false)) {
            String repositoriesPath = recordsDirPath + File.separator + repositoriesDirName;
            String repoPath = repositoriesPath + File.separator + record.getRecordDirName();
            String relativePath = Utils.backupRepository(record, repoPath);
            record.setAlternativePath(Paths.get(repoPath, relativePath).toAbsolutePath().toString());
            return cleanContent(record);
        }

        return false;
    }

    private boolean cleanContent(InputRecord record) {
        //no preprocess operation exists on file yet
        return true;
    }

    public String getRecordsDirPath() {
        return recordsDirPath;
    }
}
