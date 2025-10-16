package asyncAPI.model;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

public class HtmlUtils {
    public static String cleanHtml(String html) {
        Document doc = Jsoup.parse(html);
        return doc.body().text().trim();
    }
    
    public static String formatDescription(String description) {
        String cleanDesc = cleanHtml(description);
        if (cleanDesc.length() <= 1000) {
            return cleanDesc;
        }

        return cleanDesc.substring(0, 1000) + "...";
    }
}