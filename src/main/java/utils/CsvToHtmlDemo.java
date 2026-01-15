package utils;

import java.awt.Desktop;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Small demo that converts the test output CSV into an HTML file next to it.
 * Usage:
 *   java utils.CsvToHtmlDemo [inputCsvPath] [outputHtmlPath] [chartColumnIndexOrName] [pageTitle]
 * Examples:
 *   - no args: uses default test CSV and produces a simple HTML page (chart disabled)
 *   - specify a chartColumnIndex (0-based) to show the pictorial chart for that column
 *   - or pass a header name (case-insensitive) as the third argument to select the chart column by name
 *   - by default the demo will try to open the generated HTML in the system default browser
 */
public class CsvToHtmlDemo {
    public static void main(String[] args) throws Exception {
        Path input = Paths.get("src/test/resources/testOutput/OutputUserLikes.csv");
        Path output = Paths.get("src/test/resources/testOutput/OutputUserLikes.html");

        if (args.length >= 1) input = Paths.get(args[0]);
        if (args.length >= 2) output = Paths.get(args[1]);

        // Nice usage help
        if (args.length == 1 && ("-h".equals(args[0]) || "--help".equals(args[0]))) {
            System.out.println("Usage: java utils.CsvToHtmlDemo [inputCsvPath] [outputHtmlPath] [chartColumnIndexOrName] [pageTitle]");
            System.out.println("Defaults: input=src/test/resources/testOutput/OutputUserLikes.csv output=.../OutputUserLikes.html chartColumnIndex=-1 (disabled)");
            System.out.println("chartColumnIndexOrName can be a 0-based integer or an exact header name (case-insensitive)");
            return;
        }

        // Validate input exists
        if (!Files.exists(input)) {
            System.err.println("Input CSV not found: " + input.toAbsolutePath());
            System.err.println("Please provide an existing CSV file as the first argument.");
            return;
        }

        // Ensure output directory exists
        Path outDir = output.toAbsolutePath().getParent();
        if (outDir != null && !Files.exists(outDir)) {
            Files.createDirectories(outDir);
        }

        utils.CsvToHtmlConverter converter = new utils.CsvToHtmlConverter(',', '\"', true, "csv-table");

        int chartCol = -1; // -1 = no chart
        String title = "CSV Table";
        if (args.length >= 3) {
            String chartArg = args[2];
            // try parse as integer first
            try {
                chartCol = Integer.parseInt(chartArg);
            } catch (NumberFormatException ex) {
                // not an integer -> try resolve by header name
                try {
                    List<String> header = readCsvHeader(input, ',', '\"');
                    int idx = findHeaderIndex(header, chartArg);
                    if (idx >= 0) {
                        chartCol = idx;
                    } else {
                        System.err.println("Chart column name not found in header: '" + chartArg + "' (case-insensitive). Chart disabled.");
                        chartCol = -1;
                    }
                } catch (IOException ioex) {
                    System.err.println("Failed to read CSV header to resolve column name: " + ioex.getMessage());
                    chartCol = -1;
                }
            }
        }
        if (args.length >= 4) {
            title = args[3];
        }

        System.out.println("Using input: " + input.toAbsolutePath());
        System.out.println("Writing output: " + output.toAbsolutePath());
        System.out.println("Chart column index: " + chartCol);

        // Use the full HTML page exporter (includes styles, search, filters and chart)
        converter.convertFileToHtmlPage(input, output, StandardCharsets.UTF_8, title, chartCol);

        System.out.println("Wrote HTML page to: " + output.toAbsolutePath() + " (chartCol=" + chartCol + ")");

        // Try to open in default browser if supported
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(output.toUri());
                System.out.println("Opened generated HTML in the default browser.");
            } else {
                System.out.println("Desktop API is not supported on this platform; not opening browser.");
            }
        } catch (Throwable t) {
            System.err.println("Failed to open browser: " + t.getMessage());
        }
    }

    // Read first non-empty logical CSV row as header and return list of columns (handles quoted commas)
    private static List<String> readCsvHeader(Path p, char delimiter, char quoteChar) throws IOException {
        try (BufferedReader r = Files.newBufferedReader(p, StandardCharsets.UTF_8)) {
            String line;
            while ((line = r.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                return splitCsvLine(line, delimiter, quoteChar);
            }
        }
        return new ArrayList<>();
    }

    // Find header index by name (case-insensitive, exact match trimmed)
    private static int findHeaderIndex(List<String> header, String name) {
        if (header == null || name == null) return -1;
        String want = name.trim().toLowerCase();
        for (int i = 0; i < header.size(); i++) {
            String h = header.get(i);
            if (h != null && h.trim().toLowerCase().equals(want)) return i;
        }
        return -1;
    }

    // Split a single CSV line into columns handling quotes (simplified streaming algorithm)
    private static List<String> splitCsvLine(String line, char delimiter, char quoteChar) {
        List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (ch == quoteChar) {
                if (!inQuotes) {
                    inQuotes = true;
                } else {
                    // lookahead for escaped quote
                    if (i + 1 < line.length() && line.charAt(i + 1) == quoteChar) {
                        cur.append(quoteChar);
                        i++; // skip escaped quote
                    } else {
                        inQuotes = false;
                    }
                }
                continue;
            }
            if (ch == delimiter && !inQuotes) {
                out.add(cur.toString());
                cur.setLength(0);
                continue;
            }
            cur.append(ch);
        }
        out.add(cur.toString());
        return out;
    }
}
