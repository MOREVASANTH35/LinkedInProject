package utils;

import org.apache.commons.csv.*;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public class CsvUtils {

    /* ========= READ CSV GENERIC ========= */

    public static List<Map<String, String>> readCsv(String filePath) {

        List<Map<String, String>> data = new ArrayList<>();

        try (
                Reader reader = Files.newBufferedReader(Paths.get(filePath));
                CSVParser parser = new CSVParser(
                        reader,
                        CSVFormat.DEFAULT.withFirstRecordAsHeader().withTrim()
                )
        ) {
            for (CSVRecord record : parser) {
                Map<String, String> row = new LinkedHashMap<>();
                for (String header : parser.getHeaderMap().keySet()) {
                    row.put(header, record.get(header));
                }
                data.add(row);
            }
        } catch (IOException e) {
            throw new RuntimeException("CSV read failed", e);
        }
        return data;
    }

    /* ========= WRITE CSV GENERIC ========= */

    public static void writeCsv(String filePath, List<Map<String, String>> data) {

        if (data.isEmpty()) return;

        try (
                Writer writer = Files.newBufferedWriter(Paths.get(filePath));
                CSVPrinter printer = new CSVPrinter(
                        writer,
                        CSVFormat.DEFAULT.withHeader(
                                data.get(0).keySet().toArray(new String[0])
                        )
                )
        ) {
            for (Map<String, String> row : data) {
                printer.printRecord(row.values());
            }
            printer.flush();
        } catch (IOException e) {
            throw new RuntimeException("CSV write failed", e);
        }
    }
    public static boolean copyCsvFile(String inputPath, String outputPath) {

        File inputFile = new File(inputPath);
        File outputFile = new File(outputPath);

        // Check input file
        if (!inputFile.exists()) {
            System.out.println("Input file does not exist: " + inputPath);
            return false;
        }

        // Create output directory if missing
        File parentDir = outputFile.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }

        try (
                BufferedInputStream bis = new BufferedInputStream(new FileInputStream(inputFile));
                BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(outputFile))
        ) {
            byte[] buffer = new byte[1024];
            int length;

            while ((length = bis.read(buffer)) != -1) {
                bos.write(buffer, 0, length);
            }

            bos.flush(); // ensure data is written
            return true;

        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
}
