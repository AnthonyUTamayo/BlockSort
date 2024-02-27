package com.anthonytamayo;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

public class FileUtils {

    public static void writeJson(String str, String path, String fileName) {
        final File file = getFile(str, path, fileName);
        try {
            if (!file.exists() && !file.createNewFile()) {
                // Handle file creation failure
                throw new RuntimeException("Failed to create file: " + fileName);
            }

            try (FileWriter fw = new FileWriter(file)) {
                fw.write(str);
            }
        } catch (IOException e) {
            // Handle IOException
            e.printStackTrace(); // Log the exception or handle it as needed
        }
    }

    private static File getFile(String str, String path, String fileName) {
        if (str == null || path == null || fileName == null || str.isEmpty() || path.isEmpty() || fileName.isEmpty()) {
            // Handle null or empty inputs
            throw new IllegalArgumentException("Invalid input: str, path, and fileName must not be null or empty.");
        }

        File dir = new File(path);
        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                // Handle directory creation failure
                throw new RuntimeException("Failed to create directories: " + path);
            }
        }

        AtomicReference<File> file = new AtomicReference<>(new File(dir, fileName));
        return file.get();
    }

    public static String readJson(String path) {
        if (path == null || path.isEmpty()) {
            // Handle null or empty path
            throw new IllegalArgumentException("Invalid input: path must not be null or empty.");
        }

        StringBuilder str = new StringBuilder();
        try (FileReader fileReader = new FileReader(path)) {
            int i;
            while ((i = fileReader.read()) != -1) {
                str.append((char) i);
            }
        } catch (IOException e) {
            // Handle IOException
            e.printStackTrace(); // Log the exception or handle it as needed
            return "";
        }
        return str.toString();
    }

    public static void main(String[] args) {
        try {
            writeJson("Your JSON String", "path/to/directory", "output.json");
            String jsonString = readJson("path/to/directory/output.json");
            System.out.println("Read JSON: " + jsonString);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}