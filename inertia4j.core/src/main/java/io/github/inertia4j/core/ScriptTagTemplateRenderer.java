package io.github.inertia4j.core;

import io.github.inertia4j.spi.TemplateRenderer;
import org.jspecify.annotations.NullMarked;

import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A {@link TemplateRenderer} implementation for the current Inertia.js protocol (v3), which
 * embeds the initial page object as the text content of a
 * {@code <script data-page="app" type="application/json">} tag, rather than as an HTML
 * attribute (the convention {@link SimpleTemplateRenderer} still targets, matching
 * pre-v3 adapters). Per the protocol spec, this content must NOT be HTML-entity encoded &mdash;
 * script tag text isn't entity-decoded by the browser, so doing so would break {@code JSON.parse}
 * on the client &mdash; and forward slashes must be escaped so a prop value can't prematurely
 * close the {@code </script>} tag.
 *
 * @see <a href="https://inertiajs.com/the-protocol#html-responses">Inertia protocol — HTML responses</a>
 */
@NullMarked
public class ScriptTagTemplateRenderer implements TemplateRenderer {
    private final Matcher templateMatcher;

    /**
     * Constructs a ScriptTagTemplateRenderer.
     * Loads the template from the specified classpath resource path and prepares it for rendering.
     * The template is expected to contain a
     * {@code <script data-page="app" type="application/json">@PageObject@</script>} tag.
     *
     * @param templatePath Classpath path to the HTML template file (e.g., "/templates/app.html").
     * @throws TemplateRenderingException if the template file cannot be loaded or read.
     */
    public ScriptTagTemplateRenderer(
        String templatePath
    ) throws TemplateRenderingException {
        String template = loadTemplate(templatePath);

        this.templateMatcher = Pattern.compile("@PageObject@").matcher(template);
    }

    /**
     * {@inheritDoc}
     * <p>
     * This implementation replaces the <code>@PageObject@</code> placeholder in the loaded
     * template with the provided {@code pageObjectJson}, escaping forward slashes only.
     */
    @Override
    public String render(String pageObjectJson) {
        String safePageObjectJson = pageObjectJson.replace("/", "\\/");

        return templateMatcher.replaceFirst(Matcher.quoteReplacement(safePageObjectJson));
    }

    /**
     * Loads the template content from the specified classpath resource path.
     *
     * @param path The classpath path to the template file.
     * @return The content of the template file as a String.
     * @throws TemplateRenderingException if the template file cannot be found or read.
     */
    private String loadTemplate(String path) throws TemplateRenderingException {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

        try (InputStream inputStream = classLoader.getResourceAsStream(path)) {
            if (inputStream == null) {
                throw new TemplateRenderingException(path);
            }
            return new String(inputStream.readAllBytes());
        } catch (IOException e) {
            throw new TemplateRenderingException(path, e);
        }
    }
}
