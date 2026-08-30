package io.github.inertia4j.quarkus;

import io.github.inertia4j.core.ScriptTagTemplateRenderer;
import io.github.inertia4j.core.TemplateRenderingException;
import io.github.inertia4j.spi.TemplateRenderer;
import io.quarkus.runtime.LaunchMode;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Resolves the Inertia app shell (the HTML template carrying the {@code @PageObject@} placeholder)
 * according to how Quinoa is serving the frontend.
 *
 * <p>In production the shell is the {@code index.html} Vite built and Quinoa packaged into the
 * artifact, already carrying the hashed asset tags. In {@code quarkus:dev} that file does not
 * exist: Quinoa skips the frontend build and instead runs the Vite dev server, proxying to it every
 * request it does not recognize as a backend one. So in dev the shell has to be fetched from the
 * dev server itself &mdash; that is what makes hot reload work, since it is Vite (not us) that
 * injects the {@code /@vite/client} HMR script and rewrites the module URLs into the HTML.
 *
 * <p>Note that fetching it through the Vite dev server directly (rather than reading
 * {@code src/main/webui/index.html} off disk) is deliberate: reproducing Vite's HTML transform by
 * hand is exactly the kind of drift that silently breaks HMR.
 */
final class QuinoaTemplateRenderers {

    private static final Logger LOG = Logger.getLogger(QuinoaTemplateRenderers.class);

    /** Classpath location where Quinoa publishes the built frontend. */
    private static final String BUILT_INDEX_HTML = "META-INF/resources/index.html";

    private static final String DEV_SERVER_HOST_CONFIG = "quarkus.quinoa.dev-server.host";
    private static final String DEV_SERVER_PORT_CONFIG = "quarkus.quinoa.dev-server.port";
    private static final String DEFAULT_DEV_SERVER_HOST = "localhost";
    private static final int DEFAULT_VITE_PORT = 5173;
    private static final Duration DEV_SERVER_TIMEOUT = Duration.ofSeconds(10);

    private QuinoaTemplateRenderers() {
    }

    static TemplateRenderer resolve() throws TemplateRenderingException {
        if (LaunchMode.current() == LaunchMode.DEVELOPMENT) {
            URI indexUri = devServerIndexUri();
            LOG.infof("Inertia is reading its app shell from the Quinoa dev server at %s", indexUri);
            return ScriptTagTemplateRenderer.ofTemplateSource(() -> fetch(indexUri));
        }
        return new ScriptTagTemplateRenderer(BUILT_INDEX_HTML);
    }

    private static URI devServerIndexUri() {
        var config = ConfigProvider.getConfig();
        String host = config.getOptionalValue(DEV_SERVER_HOST_CONFIG, String.class)
            .orElse(DEFAULT_DEV_SERVER_HOST);
        int port = config.getOptionalValue(DEV_SERVER_PORT_CONFIG, Integer.class)
            .orElse(DEFAULT_VITE_PORT);
        return URI.create("http://" + host + ":" + port + "/index.html");
    }

    private static String fetch(URI indexUri) {
        HttpRequest request = HttpRequest.newBuilder(indexUri)
            .timeout(DEV_SERVER_TIMEOUT)
            .GET()
            .build();
        // HTTP/1.1 explicitly: the JDK client defaults to HTTP/2, which over cleartext means an
        // h2c upgrade handshake that the Vite dev server never answers (the request just times out).
        try (HttpClient client = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(DEV_SERVER_TIMEOUT)
            .build()) {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new IllegalStateException(
                    "Quinoa dev server answered " + response.statusCode() + " for " + indexUri);
            }
            return response.body();
        } catch (IOException | InterruptedException | IllegalStateException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new IllegalStateException(
                "Inertia could not read the app shell from the Quinoa dev server at " + indexUri
                    + ". Is the Vite dev server up on the port set in 'quarkus.quinoa.dev-server.port'?",
                e);
        }
    }
}
