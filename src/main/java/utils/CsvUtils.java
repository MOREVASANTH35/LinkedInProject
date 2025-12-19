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
}
