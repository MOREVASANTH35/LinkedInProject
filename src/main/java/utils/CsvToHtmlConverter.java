package utils;
import java.util.*;
import java.io.File;
import utils.HtmlReportGenerator;
public class CsvToHtmlConverter {
    /**
     * Generate an HTML report from a CSV file. This is a generic method you can call from anywhere.
     * @param inputCsv path to input CSV
     * @param outputHtml path to output HTML
     */
    public static void generateReport(String inputCsv, String outputHtml) {
        if (inputCsv == null || inputCsv.isEmpty()) {
            throw new IllegalArgumentException("inputCsv must be provided");
        }
        if (outputHtml == null || outputHtml.isEmpty()) {
            throw new IllegalArgumentException("outputHtml must be provided");
        }
        System.out.println("Reading CSV: " + inputCsv);
        List<Map<String, String>> rows = utils.CsvUtils.readCsv(inputCsv);
        if (rows == null || rows.isEmpty()) {
            System.out.println("No data found in CSV: " + inputCsv);
            return;
        }
        // Build original headers from first row (CsvUtils preserves order via LinkedHashMap)
        Set<String> headerSet = rows.get(0).keySet();
        String[] originalHeaders = headerSet.toArray(new String[0]);
        // Compute total records
        int totalRecords = rows.size();
        // Compute negativeCount based on "Yes %" < 50% if that column exists (per-post)
        int negativeCount = 0;
        String formulaText = "Likes with 'Yes %'";
        for (Map<String, String> rowMap : rows) {
            String pct = null;
            // try case-insensitive lookup for "Yes %"
            for (String h : originalHeaders) {
                if (h.trim().equalsIgnoreCase("Yes %")) {
                    pct = rowMap.get(h);
                    break;
                }
            }
            if (pct == null) continue;
            pct = pct.replace("%", "").trim();
            try {
                double val = Double.parseDouble(pct);
                if (val < 50.0) negativeCount++;
            } catch (NumberFormatException e) {
                // ignore unparsable
            }
        }
        // Decide whether to pivot: headers become PostUrl values and rows become remaining fields
        boolean pivot = false;
        int postUrlIndex = -1;
        for (int i = 0; i < originalHeaders.length; i++) {
            if (originalHeaders[i].trim().equalsIgnoreCase("PostUrl")) {
                pivot = true;
                postUrlIndex = i;
                break;
            }
        }
        List<String[]> tableData = new ArrayList<>();
        String[] headers;
        if (pivot) {
            // New headers: first column is "Field", remaining are PostUrl values from each CSV row
            headers = new String[rows.size() + 1];
            headers[0] = "Field";
            for (int r = 0; r < rows.size(); r++) {
                String postUrlVal = rows.get(r).get(originalHeaders[postUrlIndex]);
                headers[r + 1] = postUrlVal == null ? "" : postUrlVal;
            }
            // For each original header except PostUrl, build a row where first cell is the field name
            for (String field : originalHeaders) {
                if (field.trim().equalsIgnoreCase("PostUrl")) continue;
                String[] rowArr = new String[rows.size() + 1];
                rowArr[0] = field;
                for (int r = 0; r < rows.size(); r++) {
                    String v = rows.get(r).get(field);
                    rowArr[r + 1] = v == null ? "" : v;
                }
                tableData.add(rowArr);
            }
            formulaText = "Likes with 'Yes %' per PostUrl";
        } else {
            // Fallback: keep original orientation (columns = headers)
            headers = originalHeaders;
            for (Map<String, String> rowMap : rows) {
                String[] r = new String[headers.length];
                for (int i = 0; i < headers.length; i++) {
                    String v = rowMap.get(headers[i]);
                    r[i] = v == null ? "" : v;
                }
                tableData.add(r);
            }
            formulaText = "No 'PostUrl' column found; using original orientation";
        }
        // Ensure output directory exists
        File outFile = new File(outputHtml);
        File parent = outFile.getParentFile();
        if (parent != null && !parent.exists()) parent.mkdirs();
        System.out.println("Generating HTML report: " + outputHtml);
        HtmlReportGenerator.generateHtmlReport(outputHtml, tableData, headers, totalRecords, negativeCount, formulaText);
        System.out.println("Done. Open " + outputHtml + " in a browser to view the report.");
    }
    // Small runner that converts a CSV file into an HTML report using HtmlReportGenerator
    public static void main(String[] args) {
        String inputCsv = "src/test/resources/testOutput/OutputUserLikes.csv";
        String outputHtml = "src/test/resources/testOutput/OutputUserLikes.html";
        if (args.length >= 1) inputCsv = args[0];
        if (args.length >= 2) outputHtml = args[1];
        generateReport(inputCsv, outputHtml);
    }
}