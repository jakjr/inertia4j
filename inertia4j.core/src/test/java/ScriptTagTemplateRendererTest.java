import io.github.inertia4j.core.ScriptTagTemplateRenderer;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ScriptTagTemplateRendererTest {
    private final ScriptTagTemplateRenderer renderer =
        new ScriptTagTemplateRenderer("script-tag-template.html");

    @Test
    void render_embedsJsonAsScriptContent_notHtmlAttribute() {
        String html = renderer.render("{\"component\":\"Home\"}");

        assertTrue(html.contains(
            "<script data-page=\"app\" type=\"application/json\">{\"component\":\"Home\"}</script>"
        ));
    }

    @Test
    void render_doesNotHtmlEntityEncodeQuotes() {
        String html = renderer.render("{\"component\":\"Home\"}");

        assertFalse(html.contains("&quot;"));
    }

    @Test
    void render_escapesForwardSlashes_toPreventPrematureScriptClose() {
        String html = renderer.render("{\"component\":\"Some/Page\"}");

        assertTrue(html.contains("Some\\/Page"));
        assertFalse(html.contains("Some/Page"));
    }

    @Test
    void ofTemplateSource_reReadsTheTemplateOnEveryRender() {
        AtomicInteger reads = new AtomicInteger();
        ScriptTagTemplateRenderer dynamic = ScriptTagTemplateRenderer.ofTemplateSource(
            () -> "<html>" + reads.incrementAndGet() + ":@PageObject@</html>"
        );

        assertEquals("<html>1:{\"a\":1}</html>", dynamic.render("{\"a\":1}"));
        assertEquals("<html>2:{\"a\":1}</html>", dynamic.render("{\"a\":1}"));
    }
}
