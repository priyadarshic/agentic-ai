package output_code;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.File;

public class FileToStringConverter {

    /**
     * Reads the content of a file into a String using UTF-8 encoding.
     *
     * @param filePath The path to the file.
     * @return The content of the file as a String.
     * @throws IOException If an I/O error occurs reading from the file.
     */
    public static String readFileToString(String filePath) throws IOException {
        Path path = Paths.get(filePath);
        return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    }

    public static void main(String[] args) {
        /*if (args.length != 1) {
            System.out.println("Usage: java FileToStringConverter <file_path>");
            return;
        }*/

        String filePath = "D:\\AgenticAI\\agent-demo\\src\\main\\java\\output_code\\DoublyLinkedList.java";
        File file = new File(filePath);



        if (!file.exists()) {
            System.err.println("Error: File does not exist: " + filePath);
            return;
        }

        try {
            String fileContent = readFileToString(filePath);
            System.out.println(fileContent);
        } catch (IOException e) {
            System.err.println("Error reading file: " + e.getMessage());
        }
    }
}