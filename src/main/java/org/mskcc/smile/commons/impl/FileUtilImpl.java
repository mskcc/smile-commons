package org.mskcc.smile.commons.impl;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import org.mskcc.smile.commons.FileUtil;
import org.springframework.stereotype.Component;

@Component
public class FileUtilImpl implements FileUtil {

    /**
     * Returns and/or creates file with header.
     * @param filepath
     * @param header
     * @return File
     * @throws IOException
     */
    @Override
    public File getOrCreateFileWithHeader(String filepath, String header) throws IOException {
        File f = new File(filepath);
        Boolean fileCreated = Boolean.FALSE;
        if (!f.exists()) {
            f.createNewFile();
            fileCreated = Boolean.TRUE;
        }
        BufferedWriter fileWriter = new BufferedWriter(new FileWriter(f, true));
        if (fileCreated) {
            fileWriter.write(header);
        }
        fileWriter.close();
        return f;
    }

    /**
     * Writes line to file.
     * @param file
     * @param line
     * @throws IOException
     */
    @Override
    public void writeToFile(File file, String line) throws IOException {
        BufferedWriter fileWriter = new BufferedWriter(new FileWriter(file, true));
        fileWriter.write(line);
        fileWriter.flush();
        fileWriter.close();
    }
}
