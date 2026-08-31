package com.interview.service;

import com.interview.dto.VOs;
import com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughExtension;
import com.vladsch.flexmark.ext.gfm.tasklist.TaskListExtension;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.data.MutableDataSet;
import org.owasp.html.AttributePolicy;
import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class MarkdownService {

    private static final Pattern H_PATTERN = Pattern.compile("<h([1-6])\\s+id=\"([^\"]+)\">([^<]+)</h\\1>");
    private static final Pattern HEADING_PATTERN = Pattern.compile("<h([1-6])([^>]*)>(.*?)</h\\1>", Pattern.DOTALL);
    private static final Pattern ID_PATTERN = Pattern.compile("id=\"([^\"]+)\"");
    /** img src：允许 base64(data:image/*)、http(s)、站内相对路径；其余丢弃（防止 javascript:/data:text 等）。 */
    private static final AttributePolicy IMG_SRC_POLICY = (el, attr, val) -> {
        String v = val == null ? "" : val.trim();
        if (v.toLowerCase().startsWith("data:image/")) return v;
        if (v.startsWith("http://") || v.startsWith("https://") || v.startsWith("mailto:")) return v;
        if (v.startsWith("/")) return v;
        return null;
    };
    /** a href：拒绝 data: URL，防止 data:text/html 等 XSS。 */
    private static final AttributePolicy HREF_POLICY = (el, attr, val) ->
            val != null && val.trim().toLowerCase().startsWith("data:") ? null : val;
    private final PolicyFactory policy;

    public MarkdownService() {
        this.policy = new HtmlPolicyBuilder()
                .allowElements("h1", "h2", "h3", "h4", "h5", "h6", "p", "pre", "code",
                        "table", "thead", "tbody", "tr", "th", "td", "img", "a",
                        "ul", "ol", "li", "blockquote", "strong", "em", "br", "hr")
                .allowAttributes("id").onElements("h1", "h2", "h3", "h4", "h5", "h6")
                .allowAttributes("class").matching(Pattern.compile("(language-[\\w-]+|hljs)")).onElements("code")
                .allowUrlProtocols("http", "https", "mailto", "data")
                .allowAttributes("href").matching(HREF_POLICY).onElements("a")
                .allowAttributes("src").matching(IMG_SRC_POLICY).onElements("img")
                .allowAttributes("alt").onElements("img")
                .allowElements("input")
                .allowAttributes("type", "checked", "disabled").onElements("input")
                .toFactory();
    }

    public String render(String markdown) {
        MutableDataSet options = new MutableDataSet();
        options.set(Parser.EXTENSIONS, List.of(
                TablesExtension.create(),
                StrikethroughExtension.create(),
                TaskListExtension.create()
        ));
        Parser parser = Parser.builder(options).build();
        HtmlRenderer renderer = HtmlRenderer.builder(options).build();
        String html = renderer.render(parser.parse(markdown == null ? "" : markdown));
        return policy.sanitize(addHeadingIds(html));
    }

    private String addHeadingIds(String html) {
        Matcher m = HEADING_PATTERN.matcher(html);
        StringBuilder sb = new StringBuilder();
        int seq = 0;
        Map<String, Integer> usedIds = new HashMap<>();
        while (m.find()) {
            String level = m.group(1);
            String attrs = m.group(2);
            String content = m.group(3);
            String id = null;
            Matcher idm = ID_PATTERN.matcher(attrs);
            if (idm.find()) {
                id = idm.group(1);
            }
            if (id == null) {
                id = slugify(content.replaceAll("<[^>]+>", ""));
                if (id.isEmpty()) {
                    id = "sec-" + (++seq);
                }
            }
            int count = usedIds.merge(id, 1, Integer::sum);
            String finalId = count > 1 ? id + "-" + count : id;
            String finalAttrs;
            if (ID_PATTERN.matcher(attrs).find()) {
                finalAttrs = attrs.replaceAll("id=\"[^\"]*\"", "id=\"" + finalId + "\"");
            } else {
                finalAttrs = attrs + " id=\"" + finalId + "\"";
            }
            String replacement = "<h" + level + finalAttrs + ">" + content + "</h" + level + ">";
            m.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private String slugify(String text) {
        String slug = text.trim().replaceAll("[^\\p{L}\\p{N}]+", "-").replaceAll("^-+|-+$", "");
        return slug.length() > 80 ? slug.substring(0, 80) : slug;
    }

    public List<VOs.TocItemVO> extractToc(String html) {
        List<VOs.TocItemVO> toc = new ArrayList<>();
        if (html == null) {
            return toc;
        }
        Matcher m = H_PATTERN.matcher(html);
        while (m.find()) {
            toc.add(new VOs.TocItemVO(m.group(2), HtmlUtils.htmlUnescape(m.group(3)), Integer.valueOf(m.group(1))));
        }
        return toc;
    }
}
