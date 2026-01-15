package utils;

import java.io.IOException;
import java.io.PushbackReader;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Small, dependency-free CSV -> HTML table converter.
 * Handles quoted fields (including escaped quotes and newlines inside quotes) using a streaming parser.
 *
 * New: provides a full-page HTML exporter with styles, a search bar for filtering, scrollable table area,
 * column filters and a simple pictorial bar-chart based on a chosen column.
 */
public class CsvToHtmlConverter {
    private final char delimiter;
    private final char quoteChar;
    private final boolean firstRowIsHeader;
    private final String tableClass; // optional CSS class attribute for the table

    public CsvToHtmlConverter() {
        this(',', '"', true, null);
    }

    public CsvToHtmlConverter(char delimiter, char quoteChar, boolean firstRowIsHeader, String tableClass) {
        this.delimiter = delimiter;
        this.quoteChar = quoteChar;
        this.firstRowIsHeader = firstRowIsHeader;
        this.tableClass = tableClass;
    }

    // Convert CSV provided as a String into an HTML table string
    public String convert(String csv) throws IOException {
        try (Reader r = new java.io.StringReader(csv)) {
            java.io.StringWriter w = new java.io.StringWriter();
            convert(r, w);
            return w.toString();
        }
    }

    // Convert from any Reader to a Writer (streaming)
    public void convert(Reader reader, Writer writer) throws IOException {
        try (PushbackReader r = new PushbackReader(reader, 2)) {
            writer.write("<table");
            if (tableClass != null && !tableClass.isEmpty()) {
                writer.write(" class=\"");
                writer.write(escapeHtml(tableClass));
                writer.write("\"");
            }
            writer.write(">\n");

            boolean inQuotes = false;
            StringBuilder field = new StringBuilder();
            List<String> row = new ArrayList<>();
            boolean isFirstRow = true;

            int chi;
            while ((chi = r.read()) != -1) {
                char ch = (char) chi;

                if (ch == quoteChar) {
                    if (!inQuotes) {
                        inQuotes = true;
                    } else {
                        // possible escaped quote ("") or end of quoted section
                        int next = r.read();
                        if (next == quoteChar) {
                            // escaped quote
                            field.append(quoteChar);
                        } else {
                            // end quote
                            inQuotes = false;
                            if (next != -1) r.unread(next);
                        }
                    }
                    continue;
                }

                if (ch == delimiter && !inQuotes) {
                    row.add(field.toString());
                    field.setLength(0);
                    continue;
                }

                if ((ch == '\n' || ch == '\r') && !inQuotes) {
                    // handle CRLF (\r\n)
                    if (ch == '\r') {
                        int next = r.read();
                        if (next != '\n' && next != -1) {
                            r.unread(next);
                        }
                    }
                    // finish current field and row
                    row.add(field.toString());
                    field.setLength(0);

                    writeRow(writer, row, firstRowIsHeader && isFirstRow);
                    isFirstRow = false;
                    row.clear();
                    continue;
                }

                // normal character
                field.append(ch);
            }

            // EOF - flush last field/row if present
            if (field.length() > 0 || !row.isEmpty()) {
                row.add(field.toString());
                writeRow(writer, row, firstRowIsHeader && isFirstRow);
            }

            writer.write("</table>\n");
            writer.flush();
        }
    }

    // New: produce a full HTML page (styles + search/filter + chart) from the CSV reader
    public void convertToHtmlPage(Reader reader, Writer writer, String title, int chartColumnIndex) throws IOException {
        List<List<String>> rows = parseAll(reader);
        writeHtmlPage(writer, rows, title == null ? "CSV Table" : title, chartColumnIndex);
    }

    // Convert from a CSV string and write straight to a file
    public void convertToFile(String csv, Path out, Charset cs) throws IOException {
        try (Reader r = new java.io.StringReader(csv);
             Writer w = Files.newBufferedWriter(out, cs)) {
            convert(r, w);
        }
    }

    // Convenience: read file from disk and convert to HTML file (simple table only)
    public void convertFileToFile(Path in, Path out, Charset cs) throws IOException {
        try (Reader r = Files.newBufferedReader(in, cs);
             Writer w = Files.newBufferedWriter(out, cs)) {
            convert(r, w);
        }
    }

    // Convenience: read file from disk and write full HTML page with chart/filter
    public void convertFileToHtmlPage(Path in, Path out, Charset cs, String title, int chartColumnIndex) throws IOException {
        try (Reader r = Files.newBufferedReader(in, cs);
             Writer w = Files.newBufferedWriter(out, cs)) {
            convertToHtmlPage(r, w, title, chartColumnIndex);
        }
    }

    // Parse entire CSV into memory (list of rows) - used for page generation with chart/filter
    private List<List<String>> parseAll(Reader reader) throws IOException {
        List<List<String>> result = new ArrayList<>();
        try (PushbackReader r = new PushbackReader(reader, 2)) {
            boolean inQuotes = false;
            StringBuilder field = new StringBuilder();
            List<String> row = new ArrayList<>();
            int chi;
            while ((chi = r.read()) != -1) {
                char ch = (char) chi;
                if (ch == quoteChar) {
                    if (!inQuotes) {
                        inQuotes = true;
                    } else {
                        int next = r.read();
                        if (next == quoteChar) {
                            field.append(quoteChar);
                        } else {
                            inQuotes = false;
                            if (next != -1) r.unread(next);
                        }
                    }
                    continue;
                }
                if (ch == delimiter && !inQuotes) {
                    row.add(field.toString());
                    field.setLength(0);
                    continue;
                }
                if ((ch == '\n' || ch == '\r') && !inQuotes) {
                    if (ch == '\r') {
                        int next = r.read();
                        if (next != '\n' && next != -1) r.unread(next);
                    }
                    row.add(field.toString());
                    field.setLength(0);
                    result.add(row);
                    row = new ArrayList<>();
                    continue;
                }
                field.append(ch);
            }
            if (field.length() > 0 || !row.isEmpty()) {
                row.add(field.toString());
                result.add(row);
            }
        }
        return result;
    }

    // Build full HTML page with styles, search input, scrollable table and a simple pictorial bar-chart
    private void writeHtmlPage(Writer writer, List<List<String>> rows, String title, int chartColumnIndex) throws IOException {
        // compute start row and column count
        int startRow = 0;
        if (!rows.isEmpty() && firstRowIsHeader) startRow = 1;
        List<String> header = null;
        if (!rows.isEmpty()) header = rows.get(0);
        int colCount = 0;
        if (header != null) colCount = header.size();
        for (int i = startRow; i < rows.size(); i++) {
            colCount = Math.max(colCount, rows.get(i).size());
        }

        // build unique values per column for filter dropdowns (preserve insertion order)
        List<Set<String>> uniquePerCol = new ArrayList<>(colCount);
        for (int c = 0; c < colCount; c++) uniquePerCol.add(new LinkedHashSet<>());
        for (int i = startRow; i < rows.size(); i++) {
            List<String> r = rows.get(i);
            for (int c = 0; c < colCount; c++) {
                String v = c < r.size() ? r.get(c) : "";
                uniquePerCol.get(c).add(v == null ? "" : v);
            }
        }

        // build unique values per row for row-level select filters
        List<Set<String>> uniquePerRow = new ArrayList<>();
        for (int i = startRow; i < rows.size(); i++) {
            Set<String> s = new LinkedHashSet<>();
            List<String> r = rows.get(i);
            for (int c = 0; c < colCount; c++) {
                String v = c < r.size() ? r.get(c) : "";
                s.add(v == null ? "" : v);
            }
            uniquePerRow.add(s);
        }

        // Render HTML (text blocks used for readability)
        writer.write("""
<!doctype html>
<html lang="en">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width,initial-scale=1">
<title>""");
        writer.write(escapeHtml(title));
        writer.write("""
</title>
<style>
/* Colors and layout */
body{font-family:Arial,Helvetica,sans-serif;margin:16px;background:#f5f7fb;color:#102a43}
.topbar{display:flex;justify-content:space-between;align-items:center;gap:12px;margin-bottom:12px}
.controls-left{display:flex;align-items:center;gap:8px}
.controls-right{display:flex;align-items:center;gap:10px}
.search{padding:8px;border-radius:6px;border:1px solid #b6c2d9;width:320px;background:white}
.small{padding:6px;border-radius:6px;border:1px solid #d0d7e6;background:white}
.table-wrap{max-height:520px;overflow:auto;border:1px solid #e1e8f0;border-radius:6px;background:white}
table.csv{border-collapse:collapse;width:100%}
.csv thead th{position:sticky;top:0;background:linear-gradient(180deg,#0a74da 0%,#0b63c6 100%);color:white;padding:10px;text-align:left;box-shadow:0 2px 0 rgba(10,116,218,0.08)}
.csv thead tr.filters th{background:#f1f5f9;color:#102a43;font-weight:normal;padding:6px}
.csv th,.csv td{padding:8px;border-bottom:1px solid #eef3fb}
.csv tbody tr:nth-child(odd){background:#ffffff}
.csv tbody tr:nth-child(even){background:#fbfdff}
.csv tbody tr:hover{background:#fffbe6}
.col-filter select{width:100%;padding:6px;border-radius:4px;border:1px solid #c9d7ee;background:#fff}
.row-filter select{width:100%;padding:6px;border-radius:4px;border:1px solid #c9d7ee;background:#fff}
.badge{display:inline-block;padding:4px 8px;border-radius:12px;background:#e6eef8;color:#0a63c6;margin-left:8px}
.chart{margin-top:12px;display:flex;flex-direction:column;gap:8px}
.bar{height:20px;background:#4caf50;color:white;padding:2px 6px;border-radius:4px;display:flex;align-items:center;min-width:24px}
.bar-label{width:220px;font-size:13px;color:#102a43}
.bar-wrap{display:flex;gap:8px;align-items:center}
.empty-msg{color:#627d98;padding:8px}
.setting-label{font-size:13px;color:#102a43}
</style>
</head>
<body>
""");

        // Top bar: search at top-left, filter settings top-right
        int dataRowCount = Math.max(0, rows.size() - startRow);
        writer.write("<div class=\"topbar\">\n<div class=\"controls-left\">\n<input id=\"tableSearch\" class=\"search\" placeholder=\"Global search...\" aria-label=\"Global search\">\n<button id=\"clearFilters\" class=\"small\">Clear</button>\n<span id=\"rowCount\" class=\"badge\">Rows: ");
        writer.write(String.valueOf(dataRowCount));
        writer.write("</span>\n</div>\n<div class=\"controls-right\">\n<label class=\"setting-label\">Mode:</label>\n<select id=\"filterMode\" class=\"small\">\n<option value=\"local\">Local (this table)</option>\n<option value=\"global\">Global (all tables)</option>\n</select>\n<label class=\"setting-label\">Match:</label>\n<select id=\"matchType\" class=\"small\">\n<option value=\"contains\">Contains</option>\n<option value=\"starts\">Starts with</option>\n<option value=\"regex\">Regex</option>\n</select>\n<label><input type=\"checkbox\" id=\"caseSensitive\"> Case</label>\n<label class=\"setting-label\">Debounce ms:</label><input id=\"debounceMs\" class=\"small\" type=\"number\" value=\"150\" min=\"0\" style=\"width:80px\">\n<button id=\"saveSettings\" class=\"small\">Save</button>\n</div>\n</div>\n");

        // Chart container
        writer.write("<div id=\"chartArea\" class=\"chart\"></div>\n");

        // Legend explaining the row-checkbox filters (accessible)
        writer.write("<div id=\"legend\" style=\"margin-bottom:8px;background:#ffffff;padding:10px;border:1px solid #e6eef8;border-radius:6px;color:#102a43;font-size:13px\">\n");
        writer.write("<strong>How row filters work:</strong> Use the checkboxes in the \"Row\" column to select values for that data row.\n");
        writer.write("Selecting one or more values in a row means: columns are kept visible only when they match the selected values for every active row filter (AND across rows).\n");
        writer.write("Checked values also participate in the global row-match filter — rows containing any checked value will remain visible.\n");
        writer.write("<div style=\"margin-top:6px;color:#516a86;font-size:12px\">Tip: focus a row's checkbox area and use Up/Down/Home/End to move between choices; Space/Enter toggles a checkbox. Press '/' to focus the global search.</div>\n");
        writer.write("</div>\n");

        // Table
        writer.write("<div class=\"table-wrap\">\n<table id=\"csvTable\" class=\"csv\">\n<thead>\n<tr>\n");
        // leading header cell for row controls (e.g., row selector / row filter)
        writer.write("<th style=\"width:56px;text-align:center\">Row</th>");
        // header row (shifted by one because of the leading 'Row' column)
        if (header != null) {
            for (int c = 0; c < colCount; c++) {
                String h = c < header.size() ? header.get(c) : "";
                writer.write("<th>"); writer.write(escapeHtml(h)); writer.write("</th>");
            }
        } else {
            for (int c = 0; c < colCount; c++) writer.write("<th></th>");
        }
        writer.write("\n</tr>\n");

        // filters row (second header row)
        writer.write("<tr class=\"filters\">\n");
        // leading filter cell: row-specific filter input + select-all + show-selected-only
        writer.write("<th>");
        writer.write("<div style=\"display:flex;gap:6px;align-items:center;justify-content:center\">\n");
        writer.write("<input id=\"rowFilter\" class=\"small\" placeholder=\"Row filter...\" style=\"width:120px\">\n");
        writer.write("<label style=\"font-size:11px;display:flex;align-items:center;gap:4px\"><input type=\"checkbox\" id=\"showSelectedOnly\">Selected</label>\n");
        writer.write("<label title=\"Select all rows\" style=\"font-size:11px;display:flex;align-items:center;gap:4px\"><input type=\"checkbox\" id=\"selectAllRows\"></label>\n");
        writer.write("</div>");
        writer.write("</th>\n");
        for (int c = 0; c < colCount; c++) {
            writer.write("<th>");
            writer.write("<div class=\"col-filter\">\n<select data-col=\"" + c + "\" onchange=\"filterTable()\">\n<option value=\"__ALL__\">All</option>\n");
            for (String opt : uniquePerCol.get(c)) {
                writer.write("<option value=\"" + escapeHtml(opt) + "\">" + escapeHtml(opt) + "</option>\n");
            }
            writer.write("</select>\n</div>");
            writer.write("</th>\n");
        }
        writer.write("</tr>\n</thead>\n<tbody>\n");

        for (int i = startRow; i < rows.size(); i++) {
            List<String> row = rows.get(i);
            writer.write("<tr>\n");
            // leading per-row controls: selector checkbox + per-row select filter (values from the row)
            writer.write("<td style=\"text-align:center\">\n");
            writer.write("  <div style=\"display:flex;flex-direction:column;align-items:center;gap:6px\">\n");
            writer.write("    <input type=\"checkbox\" class=\"row-selector\" title=\"Select row\">\n");
            // render per-row filter as a list of checkboxes (multi-select via checkboxes)
            writer.write("    <div class=\"row-filter\" role=\"group\" aria-label=\"Row filters for row " + (i - startRow + 1) + "\" aria-describedby=\"legend\" tabindex=0 style=\"max-height:120px;overflow:auto;padding:4px;outline:none\">\n");
            // populate checkboxes from uniquePerRow; include aria-label for each checkbox
            Set<String> opts = uniquePerRow.get(i - startRow);
            for (String o : opts) {
                String v = escapeHtml(o);
                writer.write("<label style=\"display:block;font-size:12px;white-space:nowrap\"><input type=\"checkbox\" class=\"row-filter-checkbox\" data-row=\"" + (i - startRow) + "\" value=\"" + v + "\" aria-label=\"Filter value '" + v + "' for row " + (i - startRow + 1) + "\"> " + v + "</label>\n");
            }
            writer.write("</div>\n");
            writer.write("  </div>\n");
            writer.write("</td>");
            for (int c = 0; c < colCount; c++) {
                String cell = c < row.size() ? row.get(c) : "";
                writer.write("<td>"); writer.write(escapeHtml(cell)); writer.write("</td>");
            }
            writer.write("</tr>\n");
        }
        if (rows.size() == startRow) {
            // add 1 for the leading 'Row' selector column
            writer.write("<tr><td class=\"empty-msg\" colspan=\"" + (colCount + 1) + "\">No data rows</td></tr>\n");
        }
        writer.write("</tbody>\n</table>\n</div>\n");

        // JavaScript: filtering and chart with dynamic settings, persistence, global/local modes
        // Use a Java text block for the script (safer and readable). Insert the
        // chart column index into the JS and the column offset for the leading
        // row-selector column using String.replace so we avoid breaking the
        // Java source with raw quotes/newlines.
        // compute excluded column indices based on header names so they are always visible
        List<String> toExcludeNames = List.of("PostUrl", "Total", "Yes %", "Executed At (IST)");
        String excludedColsArray;
        if (header != null) {
            StringBuilder sb = new StringBuilder();
            sb.append("[");
            boolean firstEx = true;
            for (int c = 0; c < colCount; c++) {
                String hn = c < header.size() ? header.get(c) : "";
                if (hn != null) {
                    String t = hn.trim();
                    for (String en : toExcludeNames) {
                        if (t.equals(en)) {
                            if (!firstEx) sb.append(',');
                            sb.append(c);
                            firstEx = false;
                            break;
                        }
                    }
                }
            }
            sb.append("]");
            excludedColsArray = sb.toString();
        } else {
            excludedColsArray = "[]";
        }
         String script = """
<script>
+(function(){
  // helpers
  function escapeReg(s){ try{ return new RegExp(s); }catch(e){ return null; } }
  function debounce(fn,ms){ var t; return function(){ var args=arguments; clearTimeout(t); t=setTimeout(function(){ fn.apply(null,args); }, ms); }; }
  function loadSettings(){ try{ var raw=localStorage.getItem('csvFilterSettings'); return raw?JSON.parse(raw):null; }catch(e){return null;} }
  function saveSettings(obj){ try{ localStorage.setItem('csvFilterSettings', JSON.stringify(obj)); }catch(e){} }

  var searchEl = document.getElementById('tableSearch');
  var rowFilterEl = document.getElementById('rowFilter');
  var modeEl = document.getElementById('filterMode');
  var caseEl = document.getElementById('caseSensitive');
  var matchEl = document.getElementById('matchType');
  var debounceEl = document.getElementById('debounceMs');
  var clearBtn = document.getElementById('clearFilters');
  var saveBtn = document.getElementById('saveSettings');
  var showSelEl = document.getElementById('showSelectedOnly');
  var selectAllEl = document.getElementById('selectAllRows');

  // excludedCols array injected by the Java generator (indices of data columns to always keep visible)
  var excludedCols = [];
  try { excludedCols = %EXCLUDED_COLS%; if(!Array.isArray(excludedCols)) excludedCols = []; } catch(e) { excludedCols = []; }

   // initialize from storage if present
   var saved = loadSettings();
   if(saved){ if(saved.q) searchEl.value = saved.q; if(saved.mode) modeEl.value = saved.mode; if(saved.case) caseEl.checked=saved.case; if(saved.match) matchEl.value=saved.match; if(saved.debounce) debounceEl.value=saved.debounce; }
   if(saved){ if(saved.rowQ) rowFilterEl.value = saved.rowQ; if(saved.showSelectedOnly) showSelEl.checked = saved.showSelectedOnly; }

  // main filter logic (applies to single table element)
  function applyFilterToTable(table, settings){ var tbody = table.tBodies[0]; if(!tbody) return 0; var rows = Array.from(tbody.rows); var selects = Array.from(table.querySelectorAll('.col-filter select')); var offset = %COL_OFFSET%; var shown=0; 
    // ensure excludedCols is always defined (robust against template replacement failures)
    var excludedCols;
    try { excludedCols = %EXCLUDED_COLS%; if(!Array.isArray(excludedCols)) excludedCols = []; } catch(e) { excludedCols = []; }

    // collect active row-filter selections (row-index -> values[]) from checkboxes that should be used to hide/show columns
    var rowsMap = {};
    Array.prototype.slice.call(table.querySelectorAll('.row-filter-checkbox')).forEach(function(cb){
      var r = cb.dataset.row ? parseInt(cb.dataset.row,10) : -1;
      if(r < 0) return;
      if(!rowsMap[r]) rowsMap[r] = [];
      if(cb.checked) rowsMap[r].push(cb.value);
    });
    var activeRowFilters = Object.keys(rowsMap).map(function(k){ return {row: parseInt(k,10), vals: rowsMap[k]}; }).filter(function(o){ return o.vals && o.vals.length>0; });

    // apply column visibility based on activeRowFilters: a data column remains visible only if for every active row-filter
    // the cell at that (row, column) equals the requested value. This is an AND across row-filters.
    (function applyColumnVisibility(){ var colCount = table.tHead.querySelectorAll('tr:first-child th').length - offset; var allRows = Array.from(table.querySelectorAll('thead tr, tbody tr')); for(var col=0; col<colCount; col++){ 
        // if this data column is in the excluded list, ensure visible and skip filtering
        if(Array.isArray(excludedCols) && excludedCols.indexOf(col) !== -1){ allRows.forEach(function(r){ var c = r.cells[col + offset]; if(c){ c.style.display = ''; } }); continue; }
        var hide = false; if(activeRowFilters.length>0){ for(var ri=0; ri<activeRowFilters.length; ri++){ var rf = activeRowFilters[ri]; var bodyRow = rows[rf.row]; if(!bodyRow){ hide=true; break; } var cell = bodyRow.cells[col + offset]; var cellText = cell? (cell.innerText||cell.textContent||'').trim() : ''; if(!settings.case){ cellText = cellText.toLowerCase(); }
             // rf.vals is an array; column must match at least one of the values for this row-filter
             var matches = rf.vals.some(function(w){ var ww = settings.case? w : w.toLowerCase(); return cellText === ww; });
             if(!matches){ hide = true; break; }
           } }
         // show/hide this column's cells across all rows (thead & tbody)
         allRows.forEach(function(r){ var c = r.cells[col + offset]; if(c){ c.style.display = hide? 'none' : ''; } }); } })();

    // collect active row-filter selected values (legacy global row-filter behavior) as a flat array from checked checkboxes
    var activeRowVals = Array.prototype.slice.call(table.querySelectorAll('.row-filter-checkbox:checked')).map(function(cb){ return cb.value; });
    rows.forEach(function(r){ var rowText = r.innerText || r.textContent || ''; var ok = true; // selected-only filter
    if(settings.showSelectedOnly){ var cb = r.querySelector('.row-selector'); if(!cb || !cb.checked){ ok=false; } }
    // row-specific quick filter (text input in header)
    if(ok && settings.rowQ){ var q = settings.rowQ; var textToTest = settings.case? rowText : rowText.toLowerCase(); if(!settings.case) q = q.toLowerCase(); if(settings.match==='contains'){ if(textToTest.indexOf(q)===-1) ok=false; } else if(settings.match==='starts'){ if(textToTest.indexOf(q)!==0) ok=false; } else if(settings.match==='regex'){ var re = (function(){ try{ return new RegExp(settings.rowQ); }catch(e){ return null; } })(); if(!re || !re.test(settings.case? rowText : rowText.toLowerCase())) ok=false; } }
    // global search / other filters
    if(ok){ var qg = settings.q || ''; if(qg){ var t = settings.case? rowText : rowText.toLowerCase(); if(!settings.case) qg = qg.toLowerCase(); if(settings.match==='contains'){ if(t.indexOf(qg)===-1) ok=false; } else if(settings.match==='starts'){ if(t.indexOf(qg)!==0) ok=false; } else if(settings.match==='regex'){ var re2 = (function(){ try{ return new RegExp(settings.q); }catch(e){ return null; } })(); if(!re2 || !re2.test(settings.case? rowText : rowText.toLowerCase())) ok=false; } }
    }
    // column dropdown filters (equality)
    if(ok){ for(var i=0;i<selects.length;i++){ var v = selects[i].value; if(v && v!=='__ALL__'){ var cell = r.cells[i + offset]; var cellText = cell? (cell.innerText||cell.textContent).trim() : ''; if(!settings.case){ cellText = cellText.toLowerCase(); v = v.toLowerCase(); } if(cellText !== v){ ok=false; break; } } } }
    // legacy row-select (if any): if user selected values in the per-row selects, show rows that contain ANY of the selected values
    if(ok && activeRowVals.length>0){ var rowCells = Array.from(r.cells).slice(%COL_OFFSET%); var matchedAny = false; for(var j=0;j<rowCells.length;j++){ var ct = (rowCells[j].innerText||rowCells[j].textContent||'').trim(); for(var k=0;k<activeRowVals.length;k++){ var wanted = activeRowVals[k]; if(!settings.case){ ct = ct.toLowerCase(); wanted = wanted.toLowerCase(); } if(ct === wanted){ matchedAny = true; break; } } if(matchedAny) break; } if(!matchedAny) ok = false; }
    if(ok){ r.style.display=''; shown++; } else { r.style.display='none'; } }); return shown; }

  function gatherSettings(){ return { q: searchEl.value || '', mode: modeEl.value || 'local', case: caseEl.checked, match: matchEl.value || 'contains', debounce: parseInt(debounceEl.value||'150',10)||0 }; }
  // extend settings with row filter and selection
  function gatherSettingsExtended(){ var s = gatherSettings(); s.rowQ = rowFilterEl.value || ''; s.showSelectedOnly = !!showSelEl.checked; return s; }

  // apply filters depending on mode
  function filterTable(){ var settings = gatherSettingsExtended(); if(settings.mode==='global'){ var tables = Array.from(document.querySelectorAll('table.csv')); var totalShown = 0; tables.forEach(function(t){ totalShown += applyFilterToTable(t, settings); }); document.getElementById('rowCount').innerText = 'Rows: ' + totalShown; } else { var t = document.getElementById('csvTable'); if(!t) return; var shown = applyFilterToTable(t, settings); document.getElementById('rowCount').innerText = 'Rows: ' + shown; } updateChart(); }

  // debounce wrapper
  var debouncedFilter = debounce(function(){ filterTable(); }, parseInt(debounceEl.value||'150',10));

  // wire events
  searchEl.addEventListener('input', function(){ debouncedFilter(); });
  rowFilterEl.addEventListener('input', function(){ debouncedFilter(); });
  modeEl.addEventListener('change', filterTable);
  caseEl.addEventListener('change', filterTable);
  matchEl.addEventListener('change', filterTable);
  debounceEl.addEventListener('change', function(){ debouncedFilter = debounce(function(){ filterTable(); }, parseInt(debounceEl.value||'150',10)); });
  document.querySelectorAll('.col-filter select').forEach(function(s){ s.addEventListener('change', filterTable); });
  clearBtn.addEventListener('click', function(){ searchEl.value=''; document.querySelectorAll('.col-filter select').forEach(function(s){ s.value='__ALL__'; }); document.querySelectorAll('.row-filter-checkbox').forEach(function(s){ s.checked=false; }); document.querySelectorAll('.row-selector').forEach(function(cb){ cb.checked=false; }); filterTable(); });
  saveBtn.addEventListener('click', function(){ saveSettings(gatherSettingsExtended()); alert('Filter settings saved'); });
  showSelEl.addEventListener('change', filterTable);
  selectAllEl.addEventListener('change', function(){ var on = !!selectAllEl.checked; document.querySelectorAll('.row-selector').forEach(function(cb){ cb.checked = on; }); filterTable(); });
  document.querySelectorAll('.row-selector').forEach(function(cb){ cb.addEventListener('change', function(){ if(showSelEl.checked) filterTable(); }); });
  // wire row-filter-checkbox events (recompute column visibility + filters when changed)
  document.querySelectorAll('.row-filter-checkbox').forEach(function(s){ s.addEventListener('change', function(){ filterTable(); }); });
  
  // keyboard navigation for row-filter groups: ArrowUp/ArrowDown/Home/End to move focus, Space/Enter toggles
  function wireRowFilterKeyboard(){
    Array.prototype.slice.call(document.querySelectorAll('.row-filter')).forEach(function(group){
      group.addEventListener('keydown', function(e){
        var checkboxes = Array.prototype.slice.call(group.querySelectorAll('.row-filter-checkbox'));
        if(!checkboxes.length) return;
        var active = document.activeElement;
        var idx = checkboxes.indexOf(active);
        if(e.key === 'ArrowDown'){
          e.preventDefault();
          var next = (idx < 0) ? 0 : Math.min(checkboxes.length-1, idx+1);
          checkboxes[next].focus();
        } else if(e.key === 'ArrowUp'){
          e.preventDefault();
          var prev = (idx < 0) ? 0 : Math.max(0, idx-1);
          checkboxes[prev].focus();
        } else if(e.key === 'Home'){
          e.preventDefault(); checkboxes[0].focus();
        } else if(e.key === 'End'){
          e.preventDefault(); checkboxes[checkboxes.length-1].focus();
        } else if(e.key === ' ' || e.key === 'Enter'){
          // toggle focused checkbox if any
          if(idx >= 0){ e.preventDefault(); checkboxes[idx].checked = !checkboxes[idx].checked; filterTable(); }
        }
      }, false);
      // make the checkboxes tabbable in order
      Array.prototype.slice.call(group.querySelectorAll('.row-filter-checkbox')).forEach(function(cb){ cb.setAttribute('tabindex','0'); });
    });
  }
  wireRowFilterKeyboard();

  // keyboard shortcut '/' focuses global search (ignore when focusing inputs)
  document.addEventListener('keydown', function(e){
    if(e.key === '/' && e.target && (e.target.tagName !== 'INPUT' && e.target.tagName !== 'TEXTAREA')){
      e.preventDefault(); searchEl.focus();
    }
  });

  // make rowCount updates announced to assistive tech
  document.getElementById('rowCount').setAttribute('role','status');

  // initial apply
  filterTable();
  // ensure keyboard wiring after initial render
  wireRowFilterKeyboard();
})();
</script>
""".replace("%CHART_COL%", Integer.toString(chartColumnIndex)).replace("%COL_OFFSET%", Integer.toString(1)).replace("%EXCLUDED_COLS%", excludedColsArray);

         writer.write(script);

         writer.write("</body>\n</html>\n");
         writer.flush();
     }

     private void writeRow(Writer writer, List<String> row, boolean header) throws IOException {
        writer.write("  <tr>\n");
        for (String cell : row) {
            String esc = escapeHtml(cell);
            if (header) {
                writer.write("    <th>");
                writer.write(esc);
                writer.write("</th>\n");
            } else {
                writer.write("    <td>");
                writer.write(esc);
                writer.write("</td>\n");
            }
        }
        writer.write("  </tr>\n");
    }

    // Minimal HTML escaping sufficient for table cell contents
    private String escapeHtml(String s) {
        if (s == null || s.isEmpty()) return "";
        StringBuilder out = new StringBuilder(s.length() + 16);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '&': out.append("&amp;"); break;
                case '<': out.append("&lt;"); break;
                case '>': out.append("&gt;"); break;
                case '"': out.append("&quot;"); break;
                case '\'': out.append("&#39;"); break;
                default: out.append(c);
            }
        }
        return out.toString();
    }
}
