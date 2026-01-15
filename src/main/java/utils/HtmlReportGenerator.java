import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class HtmlReportGenerator {

    private  static  int infoCount = 0;
    private static int passCount = 0;
    private static int failCount = 0;
    private static int skipCount = 0;
    private static int warningCount = 0;

    public static void generateHtmlReport(String filePath, List<String[]> tableData, String[] headers, int totalRecords, int negativeTestCaseCount, String formulaText) {
        StringBuilder htmlContent = new StringBuilder();


        // Get the current date and time
        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());

        htmlContent.append("<!DOCTYPE html>\n<html>\n<head>\n")
                .append("<title>Test Data Report</title>\n")
                .append("<style>\n")
                .append("body { font-family: Arial, sans-serif; margin: 20px; }\n")
                .append(".card { display: inline-block; padding: 20px; margin: 10px; background: #f8f9fa; border: 1px solid #ddd; border-radius: 8px; width: 200px; text-align: center; }\n")
                .append(".card h3 { margin: 0; font-size: 18px; color: #333; }\n")
                .append(".card p { margin: 5px 0 0; font-size: 16px; color: #555; }\n")
                .append(".timestamp { text-align: center; font-size: 14px; color: #555; margin-bottom: 20px; }\n")
                .append(".search-box { width: 100%; max-width: 600px; padding: 10px; font-size: 16px; border: 1px solid #ccc; border-radius: 5px; margin-bottom: 20px; }\n")
                .append("</style>\n")
                .append("<link rel='stylesheet' href='https://cdn.datatables.net/1.13.5/css/jquery.dataTables.min.css'>\n")
                .append("<script src='https://code.jquery.com/jquery-3.6.0.min.js'></script>\n")
                .append("<script src='https://cdn.datatables.net/1.13.5/js/jquery.dataTables.min.js'></script>\n")
                .append("<script>\n")
                .append("$(document).ready(function() {\n")
                .append("  var table = $('#dataTable').DataTable({\n")
                .append("    paging: true,\n")
                .append("    pageLength: 50,\n")
                .append("    searching: true,\n")
                .append("    columnDefs: [\n")
                .append("      { targets: [5], orderable: false }\n")
                .append("    ]\n")
                .append("  });\n")
                .append("\n")
                .append("  $('#customSearch').on('keyup', function() {\n")
                .append("    table.search(HtmlReportGenerator.value).draw();\n")
                .append("  });\n")
                .append("});\n")
                .append("</script>\n")
                .append("</head>\n<body>\n");

        // Add timestamp
        htmlContent.append("<div class='timestamp'>")
                .append("Report generated on: ").append(timestamp)
                .append("</div>\n");

        // Add stats cards
        htmlContent.append("<div style='text-align: center;'>\n")
                .append("<div class='card'>\n")
                .append("<h3>Total Records</h3>\n")
                .append("<p>").append(totalRecords).append("</p>\n")
                .append("</div>\n")
                .append("<div class='card'>\n")
                .append("<h3>Failed Test Cases</h3>\n")
                .append("<p>").append(negativeTestCaseCount).append("</p>\n")
                .append("</div>\n");

        htmlContent.append("</div>\n");
        // Add Formula
        htmlContent.append("<div style='text-align: center;'>\n")
                .append("<h3>Expected Condition Formula</h3>\n")
                .append("<p>").append(formulaText).append("</p>\n")
                .append("</div>\n");

        // Add custom search box
        htmlContent.append("<div style='text-align: center;'>\n")
                .append("<input type='text' id='customSearch' class='search-box' placeholder='Search by any field...' style='display:none;'>\n")
                .append("</div>\n");

        // Add DataTable
        htmlContent.append("<table id='dataTable' class='display'>\n");
        htmlContent.append("<thead><tr>");
        for (String header : headers) {
            htmlContent.append("<th>").append(header).append("</th>");
        }
        htmlContent.append("</tr></thead>\n<tbody>\n");
        for (String[] row : tableData) {
            htmlContent.append("<tr>");
            for (String cell : row) {
                htmlContent.append("<td>").append(cell).append("</td>");
            }
            htmlContent.append("</tr>\n");
        }
        htmlContent.append("</tbody>\n</table>\n</body>\n</html>");

        // Write HTML content to a file
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            writer.write(htmlContent.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String getGreenText(String info){
        return "<b><font color='green'>" + info+"</font></b>";
    }

    public static String getHyperLink(String hyperlink, String info){
        return "<a href = '"+hyperlink+"'><b><font color='blue'>"+info+" "+hyperlink+"</font></b></a>";
    }

}
