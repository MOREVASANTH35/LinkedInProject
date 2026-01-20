package utils;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
public class HtmlReportGenerator {
    public static void generateHtmlReport(String filePath, List<String[]> tableData, String[] headers, int totalRecords, int negativeTestCaseCount, String formulaText) {
        StringBuilder html = new StringBuilder();
        // Build unique values per column (preserve insertion order)
        List<LinkedHashSet<String>> uniques = new ArrayList<>();
        int cols = headers.length;
        for (int c = 0; c < cols; c++) {
            uniques.add(new LinkedHashSet<>());
        }
        for (String[] row : tableData) {
            for (int c = 0; c < cols; c++) {
                String v = c < row.length && row[c] != null ? row[c] : "";
                uniques.get(c).add(v);
            }
        }
        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        html.append("<!doctype html>\n<html>\n<head>\n<meta charset=\"utf-8\">\n<title>Test Data Report</title>\n");
        html.append("<style>\n");
        html.append("body{font-family:Arial,Helvetica,sans-serif;margin:20px;}\n");
        html.append(".card{display:inline-block;padding:16px;margin:8px;background:#f8f9fa;border:1px solid #ddd;border-radius:8px;width:200px;text-align:center;}\n");
        html.append(".timestamp{ text-align:center;margin-bottom:12px;color:#555;}\n");
        html.append("table.display{border-collapse:collapse;width:100%;margin-top:16px;}\n");
        html.append("table.display th, table.display td{border:1px solid #ddd;padding:8px;vertical-align:top;}\n");
        html.append("table.display th{background:#f2f2f2;text-align:left;position:relative;padding-right:8px;}\n");
        html.append(".filter-select{width:100%;box-sizing:border-box;margin-top:6px;}\n");
        html.append(".filter-actions{margin-top:6px;text-align:right;}\n");
        html.append(".header-link small{font-size:0.85em;color:#007bff;}\n");
        html.append(".yes-cell{color:green;font-weight:bold;}\n");
        html.append(".no-cell{color:red;font-weight:bold;}\n");
        html.append("</style>\n");
        // Inline JS for filtering (vanilla) - no dependency on jQuery/DataTables
        html.append("<script>\n");
        html.append("function applyColumnFilters(){\n");
        html.append("  var table = document.getElementById('dataTable'); if(!table) return; var tbody = table.tBodies[0]; var rows = Array.prototype.slice.call(tbody.rows);\n");
        html.append("  var selects = document.querySelectorAll('.col-filter'); var filters = [];\n");
        html.append("  selects.forEach(function(s){ var vals = Array.prototype.slice.call(s.selectedOptions).map(function(o){ return o.value; }); filters.push(vals); });\n");
        html.append("  rows.forEach(function(r){ var show = true; for(var c=0;c<filters.length;c++){ var sel = filters[c]; if(sel.length===0) continue; var cell = r.cells[c]; var text = cell?cell.textContent.trim():''; if(sel.indexOf(text)===-1){ show=false; break; } } r.style.display = show? '':'none'; });\n");
        html.append("}\n");
        html.append("function clearFilter(col){ var sel = document.querySelector('.col-filter[data-col=\"'+col+'\"]'); if(!sel) return; Array.prototype.forEach.call(sel.options,function(o){ o.selected=false; }); applyColumnFilters(); }\n");
        html.append("document.addEventListener('DOMContentLoaded', function(){ var selects = document.querySelectorAll('.col-filter'); selects.forEach(function(s){ s.addEventListener('change', applyColumnFilters); }); var clears=document.querySelectorAll('.clear-filter'); clears.forEach(function(b){ b.addEventListener('click', function(){ clearFilter(this.getAttribute('data-col')); }); }); });\n");
        html.append("</script>\n");
        html.append("</head>\n<body>\n");
        html.append("<div class='timestamp'>Report generated on: ").append(timestamp).append("</div>\n");
        html.append("<div style='text-align:center;'>\n<div class='card'><h3>Total Records</h3><p>").append(totalRecords).append("</p></div>\n");
        html.append("<div class='card'><h3>Failed Test Cases</h3><p>").append(negativeTestCaseCount).append("</p></div>\n</div>\n");
        html.append("<div style='text-align:center;'><h3>Expected Condition Formula</h3><p>").append(escapeHtml(formulaText)).append("</p></div>\n");
        // Table with a second header row containing selects for filtering (server-populated)
        html.append("<table id='dataTable' class='display'>\n<thead>\n<tr>");
        for (String h : headers) {
            html.append("<th>").append(escapeHtml(h)).append("</th>");
        }
        html.append("</tr>\n<tr class='filter-row'>");
        for (int c = 0; c < cols; c++) {
            LinkedHashSet<String> set = uniques.get(c);
            html.append("<th>");
            html.append("<select class='col-filter filter-select' multiple size='5' data-col='"+c+"'>");
            for (String v : set) {
                html.append("<option value='").append(escapeHtml(v)).append("'>").append(escapeHtml(v.length() > 80 ? v.substring(0,77) + "..." : v)).append("</option>");
            }
            html.append("</select>");
            html.append("<div class='filter-actions'><button type='button' class='clear-filter' data-col='"+c+"'>Clear</button></div>");
            html.append("</th>");
        }
        html.append("</tr>\n</thead>\n<tbody>\n");
        for (String[] row : tableData) {
            html.append("<tr>");
            for (int c = 0; c < cols; c++) {
                String cell = c < row.length ? row[c] : "";
                String escaped = escapeHtml(cell);
                if (cell != null && cell.trim().equalsIgnoreCase("YES")) {
                    html.append("<td class='yes-cell'>").append(escaped).append("</td>");
                } else if (cell != null && cell.trim().equalsIgnoreCase("NO")) {
                    html.append("<td class='no-cell'>").append(escaped).append("</td>");
                } else {
                    html.append("<td>").append(escaped).append("</td>");
                }
            }
            html.append("</tr>\n");
        }
        html.append("</tbody>\n</table>\n</body>\n</html>");
        try (BufferedWriter w = new BufferedWriter(new FileWriter(filePath))) {
            w.write(html.toString());
        } catch (IOException e) {
            throw new RuntimeException("HTML write failed", e);
        }
    }
    private static String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;").replace("'","&#39;");
    }
}