package org.fiz.ise.gwifi.model;

import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by wojlukas on 5/20/16.
 */
public class NewsgroupsArticle implements Serializable {
    private static SimpleDateFormat[] dateFormats = new SimpleDateFormat[]{
            new SimpleDateFormat("dd MMM yyyy HH:mm:ss z"),
            new SimpleDateFormat("dd MMM yyyy HH:mm:ss"),
            new SimpleDateFormat("dd MMM yyyy HH:mm z"),
            new SimpleDateFormat("E, dd MMM yyyy HH:mm:ss z"),
            new SimpleDateFormat("E, dd MMM yyyy HH:mm:ss"),
            new SimpleDateFormat("E, dd MMM yyyy HH:mm z"),
            new SimpleDateFormat("E, dd MMM yy HH:mm:ss z")
    };
    private Map<String, String> headers = new HashMap<>();
    private String rawText;
    private String label;
    private Date date;

    public NewsgroupsArticle() {
    }

    public String getRawText() {
        return rawText;
    }

    public void setRawText(String rawText) {
        this.rawText = rawText;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public Date getDate() {
        if (date == null) {
            String dateString = headers.get("Date");
            if (dateString != null) {
                dateString = dateString.trim();

                /*
                    some articles have the UT timezone format, which is not parsed by
                    SimpleDateFormat, hence replacing it with UTC
                 */
                dateString = dateString.replaceFirst("UT$", "UTC");
                date = tryToParseDate(dateString);
            }
        }
        return date;
    }

    private Date tryToParseDate(String dateString) {
        for (SimpleDateFormat dateFormat : dateFormats) {
            try {
                Date parsedDate = dateFormat.parse(dateString);
                return parsedDate;
            }
            catch (ParseException e) {
            }
        }
        throw new RuntimeException("Date format of " + dateString + " unknown!");
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public void addHeader(String key, String value) {
        this.headers.put(key, value);
    }

    public String getHeader(String key) {
        return headers.get(key);
    }
}