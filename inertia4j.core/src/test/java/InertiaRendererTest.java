import io.github.inertia4j.core.*;
import io.github.inertia4j.core.props.DeferProp;
import io.github.inertia4j.core.props.MergeProp;
import io.github.inertia4j.core.props.OnceProp;
import io.github.inertia4j.spi.PageObjectSerializer;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class InertiaRendererTest {
    private final PageObjectSerializer pageObjectSerializer = new DefaultPageObjectSerializer();
    private Supplier<String> versionProvider = () -> "1";

    @Test
    void render_whenVersionConflicts_returns409AndLocation() {
        versionProvider = () -> "old";

        var httpRequest = new FakeHttpRequest("GET", Map.of("X-Inertia-Version", "new"));
        var options = new InertiaRenderingOptions(false, false, "/page", "Component", null);

        HttpResponse response = render(httpRequest, options);

        assertEquals(409, response.getCode());
        assertEquals(Collections.singletonList("/page"), response.getHeaders().get("X-Inertia-Location"));
    }

    @Test
    void render_whenVersionConflicts_whenNonGet_returns200() {
        versionProvider = () -> "old";

        var httpRequest = new FakeHttpRequest("POST", Map.of("X-Inertia-Version", "new")); // Non-GET
        var options = new InertiaRenderingOptions(false, false, "/page", "Component", null);

        HttpResponse response = render(httpRequest, options);

        assertEquals(200, response.getCode());
        assertEquals(Collections.singletonList("text/html"), response.getHeaders().get("Content-Type"));
        assertFalse(response.getHeaders().containsKey("X-Inertia"));

        var expectedBody = "<!doctype html>\n" +
                "<html lang=\"en\">\n" +
                "  <body>\n" +
                "    <div id=\"app\" data-page=\"{&quot;component&quot;:&quot;Component&quot;,&quot;props&quot;:{},&quot;url&quot;:&quot;/page&quot;,&quot;version&quot;:&quot;old&quot;,&quot;encryptHistory&quot;:false,&quot;clearHistory&quot;:false}\"></div>\n" +
                "  </body>\n" +
                "</html>".trim();
        assertEquals(normalizeHtml(expectedBody), normalizeHtml(response.getBody()));
    }

    @Test
    void render_whenNoVersionHeader_returns200WithHtml() {
        var httpRequest = new FakeHttpRequest("GET", Map.of());
        var options = new InertiaRenderingOptions(
            false,
            false,
            "/page",
            "Component",
            Map.of("name", "\"An album\"", "genre", "Drum n' Bass")
        );

        HttpResponse response = render(httpRequest, options);

        assertEquals(200, response.getCode());
        assertEquals(Collections.singletonList("text/html"), response.getHeaders().get("Content-Type"));
        assertFalse(response.getHeaders().containsKey("X-Inertia"));

        var expectedBody = "<!doctype html>\n" +
                "<html lang=\"en\">\n" +
                "  <body>\n" +
                "    <div id=\"app\" data-page=\"{&quot;component&quot;:&quot;Component&quot;,&quot;props&quot;:{&quot;genre&quot;:&quot;Drum n&apos; Bass&quot;,&quot;name&quot;:&quot;\\&quot;An album\\&quot;&quot;},&quot;url&quot;:&quot;/page&quot;,&quot;version&quot;:&quot;1&quot;,&quot;encryptHistory&quot;:false,&quot;clearHistory&quot;:false}\"></div>\n" +
                "  </body>\n" +
                "</html>".trim();

        assertEquals(normalizeHtml(expectedBody), normalizeHtml(response.getBody()));
    }

    @Test
    void render_whenSameVersion_whenInitialRequest_returns200WithHtml() {
        var httpRequest = new FakeHttpRequest("GET", Map.of("X-Inertia-Version", versionProvider.get()));
        var options = new InertiaRenderingOptions(false, false, "/page", "Component", null);

        HttpResponse response = render(httpRequest, options);

        assertEquals(200, response.getCode());
        assertEquals(Collections.singletonList("text/html"), response.getHeaders().get("Content-Type"));
        assertFalse(response.getHeaders().containsKey("X-Inertia"));

        var expectedBody = "<!doctype html>\n" +
                "<html lang=\"en\">\n" +
                "  <body>\n" +
                "    <div id=\"app\" data-page=\"{&quot;component&quot;:&quot;Component&quot;,&quot;props&quot;:{},&quot;url&quot;:&quot;/page&quot;,&quot;version&quot;:&quot;1&quot;,&quot;encryptHistory&quot;:false,&quot;clearHistory&quot;:false}\"></div>\n" +
                "  </body>\n" +
                "</html>".trim();

        assertEquals(normalizeHtml(expectedBody), normalizeHtml(response.getBody()));
    }

    @Test
    void render_whenSameVersion_whenInertiaRequest_returns200WithJson() {
        var httpRequest = new FakeHttpRequest("GET", Map.of(
            "X-Inertia-Version", versionProvider.get(),
            "X-Inertia", "true"
        ));
        Map<String, Object> props = Map.of("user", "test", "status", 1);
        var options = new InertiaRenderingOptions(false, false, "/page", "Component", props);

        HttpResponse response = render(httpRequest, options);

        assertEquals(200, response.getCode());
        assertEquals(Collections.singletonList("application/json"), response.getHeaders().get("Content-Type"));
        assertEquals(Collections.singletonList("true"), response.getHeaders().get("X-Inertia"));

        var expectedJson = "{\"component\":\"Component\",\"props\":{\"status\":1,\"user\":\"test\"},\"url\":\"/page\",\"version\":\"1\",\"encryptHistory\":false,\"clearHistory\":false}".trim();
        assertEquals(expectedJson, response.getBody());
    }

    @Test
    void render_whenSameVersion_whenPartialInertiaRequest_returns200WithJson() {
        var httpRequest = new FakeHttpRequest("GET", Map.of(
            "X-Inertia-Version", versionProvider.get(),
            "X-Inertia", "true",
            "X-Inertia-Partial-Component", "Component",
            "X-Inertia-Partial-Data", "user, status"
        ));
        Map<String, Object> props = Map.of("user", "test", "status", 1, "ignored", "abc");
        var options = new InertiaRenderingOptions(false, false, "/page", "Component", props);

        HttpResponse response = render(httpRequest, options);

        assertEquals(200, response.getCode());
        assertEquals(Collections.singletonList("application/json"), response.getHeaders().get("Content-Type"));
        assertEquals(Collections.singletonList("true"), response.getHeaders().get("X-Inertia"));

        var expectedJson = "{\"component\":\"Component\",\"props\":{\"status\":1,\"user\":\"test\"},\"url\":\"/page\",\"version\":\"1\",\"encryptHistory\":false,\"clearHistory\":false}".trim();
        assertEquals(expectedJson, response.getBody());
    }

    @Test
    void render_withFullPageLoad_withNullProps_rendersEmptyObjectProps() {
        var httpRequest = new FakeHttpRequest("GET", Map.of());
        var options = new InertiaRenderingOptions(false, false, "/page", "Component", null);

        HttpResponse response = render(httpRequest, options);

        assertEquals(200, response.getCode());
        assertEquals(Collections.singletonList("text/html"), response.getHeaders().get("Content-Type"));
        assertFalse(response.getHeaders().containsKey("X-Inertia"));

        var expectedBody = "<!doctype html>\n" +
            "<html lang=\"en\">\n" +
            "  <body>\n" +
            "    <div id=\"app\" data-page=\"{&quot;component&quot;:&quot;Component&quot;,&quot;props&quot;:{},&quot;url&quot;:&quot;/page&quot;,&quot;version&quot;:&quot;1&quot;,&quot;encryptHistory&quot;:false,&quot;clearHistory&quot;:false}\"></div>\n" +
            "  </body>\n" +
            "</html>".trim();

        assertEquals(normalizeHtml(expectedBody), normalizeHtml(response.getBody()));
    }

    @Test
    void render_withJson_withNullProps_rendersEmptyObjectProps() {
        var httpRequest = new FakeHttpRequest("GET", Map.of("X-Inertia", "true"));
        var options = new InertiaRenderingOptions(false, false, "/page", "Component", null);

        HttpResponse response = render(httpRequest, options);

        assertEquals(200, response.getCode());
        assertEquals(Collections.singletonList("application/json"), response.getHeaders().get("Content-Type"));
        assert(response.getHeaders().containsKey("X-Inertia"));

        var expectedBody = "{\"component\":\"Component\",\"props\":{},\"url\":\"/page\",\"version\":\"1\",\"encryptHistory\":false,\"clearHistory\":false}";

        assertEquals(expectedBody, response.getBody());
    }

    @Test
    void render_withDeferredProp_onFullVisit_omitsValueAndAnnouncesGroup() {
        var httpRequest = new FakeHttpRequest("GET", Map.of("X-Inertia", "true"));
        Map<String, Object> props = Map.of(
            "user", "test",
            "comments", new DeferProp(() -> {
                throw new AssertionError("deferred prop nao deveria ser resolvido num full visit");
            })
        );
        var options = new InertiaRenderingOptions(false, false, "/page", "Component", props);

        HttpResponse response = render(httpRequest, options);

        var expectedJson = "{\"component\":\"Component\",\"props\":{\"user\":\"test\"},\"url\":\"/page\",\"version\":\"1\",\"encryptHistory\":false,\"clearHistory\":false,\"deferredProps\":{\"default\":[\"comments\"]}}";
        assertEquals(expectedJson, response.getBody());
    }

    @Test
    void render_withDeferredProp_onPartialReloadRequestingIt_resolvesAndIncludesValue() {
        var httpRequest = new FakeHttpRequest("GET", Map.of(
            "X-Inertia", "true",
            "X-Inertia-Partial-Component", "Component",
            "X-Inertia-Partial-Data", "comments"
        ));
        Map<String, Object> props = Map.of(
            "user", "test",
            "comments", new DeferProp(() -> List.of("Comentario 1"))
        );
        var options = new InertiaRenderingOptions(false, false, "/page", "Component", props);

        HttpResponse response = render(httpRequest, options);

        // "user" fica de fora (partial reload so pediu "comments"), e sem deferredProps no
        // corpo: o cliente ja sabe do grupo desde o full visit anterior.
        var expectedJson = "{\"component\":\"Component\",\"props\":{\"comments\":[\"Comentario 1\"]},\"url\":\"/page\",\"version\":\"1\",\"encryptHistory\":false,\"clearHistory\":false}";
        assertEquals(expectedJson, response.getBody());
    }

    @Test
    void render_withDeferredProp_onPartialReloadForAnotherProp_omitsItWithoutResolving() {
        var httpRequest = new FakeHttpRequest("GET", Map.of(
            "X-Inertia", "true",
            "X-Inertia-Partial-Component", "Component",
            "X-Inertia-Partial-Data", "user"
        ));
        Map<String, Object> props = Map.of(
            "user", "test",
            "comments", new DeferProp(() -> {
                throw new AssertionError("deferred prop de outro grupo nao deveria ser resolvido");
            })
        );
        var options = new InertiaRenderingOptions(false, false, "/page", "Component", props);

        HttpResponse response = render(httpRequest, options);

        var expectedJson = "{\"component\":\"Component\",\"props\":{\"user\":\"test\"},\"url\":\"/page\",\"version\":\"1\",\"encryptHistory\":false,\"clearHistory\":false}";
        assertEquals(expectedJson, response.getBody());
    }

    @Test
    void render_withMergeProp_appendsByDefault() {
        var httpRequest = new FakeHttpRequest("GET", Map.of("X-Inertia", "true"));
        Map<String, Object> props = Map.of("posts", new MergeProp(List.of("Post 1")));
        var options = new InertiaRenderingOptions(false, false, "/page", "Component", props);

        HttpResponse response = render(httpRequest, options);

        var expectedJson = "{\"component\":\"Component\",\"props\":{\"posts\":[\"Post 1\"]},\"url\":\"/page\",\"version\":\"1\",\"encryptHistory\":false,\"clearHistory\":false,\"mergeProps\":[\"posts\"]}";
        assertEquals(expectedJson, response.getBody());
    }

    @Test
    void render_withMergeProp_prependAndMatchOn() {
        var httpRequest = new FakeHttpRequest("GET", Map.of("X-Inertia", "true"));
        Map<String, Object> props = Map.of(
            "notifications", new MergeProp(List.of(Map.of("id", 2))).prepend().matchOn("id")
        );
        var options = new InertiaRenderingOptions(false, false, "/page", "Component", props);

        HttpResponse response = render(httpRequest, options);

        var expectedJson = "{\"component\":\"Component\",\"props\":{\"notifications\":[{\"id\":2}]},\"url\":\"/page\",\"version\":\"1\",\"encryptHistory\":false,\"clearHistory\":false,\"prependProps\":[\"notifications\"],\"matchPropsOn\":[\"notifications.id\"]}";
        assertEquals(expectedJson, response.getBody());
    }

    @Test
    void render_withMergeProp_deepMerge() {
        var httpRequest = new FakeHttpRequest("GET", Map.of("X-Inertia", "true"));
        Map<String, Object> props = Map.of("config", new MergeProp(Map.of("theme", "dark")).deepMerge());
        var options = new InertiaRenderingOptions(false, false, "/page", "Component", props);

        HttpResponse response = render(httpRequest, options);

        var expectedJson = "{\"component\":\"Component\",\"props\":{\"config\":{\"theme\":\"dark\"}},\"url\":\"/page\",\"version\":\"1\",\"encryptHistory\":false,\"clearHistory\":false,\"deepMergeProps\":[\"config\"]}";
        assertEquals(expectedJson, response.getBody());
    }

    @Test
    void render_withMergeProp_resetHeader_resolvesButOmitsFromMergeMetadata() {
        var httpRequest = new FakeHttpRequest("GET", Map.of(
            "X-Inertia", "true",
            "X-Inertia-Reset", "posts"
        ));
        Map<String, Object> props = Map.of("posts", new MergeProp(List.of("Post fresco")));
        var options = new InertiaRenderingOptions(false, false, "/page", "Component", props);

        HttpResponse response = render(httpRequest, options);

        // Valor normal, sem mergeProps: X-Inertia-Reset pede substituicao, nao merge, desta vez.
        var expectedJson = "{\"component\":\"Component\",\"props\":{\"posts\":[\"Post fresco\"]},\"url\":\"/page\",\"version\":\"1\",\"encryptHistory\":false,\"clearHistory\":false}";
        assertEquals(expectedJson, response.getBody());
    }

    @Test
    void render_withNestedMergeProp_pathIsPrefixedAndReachableViaOnly() {
        var httpRequest = new FakeHttpRequest("GET", Map.of(
            "X-Inertia", "true",
            "X-Inertia-Partial-Component", "Component",
            "X-Inertia-Partial-Data", "feed.posts"
        ));
        Map<String, Object> feed = new LinkedHashMap<>();
        feed.put("posts", new MergeProp(List.of("Post 1")));
        feed.put("title", "Feed"); // nao pedido no only-list, deve sumir da resposta
        Map<String, Object> props = Map.of("feed", feed);
        var options = new InertiaRenderingOptions(false, false, "/page", "Component", props);

        HttpResponse response = render(httpRequest, options);

        // "feed" sobrevive so pra recursao alcancar "feed.posts" (leadsToPath); "feed.title" some.
        // mergeProps carrega o path completo "feed.posts", nao so "posts".
        var expectedJson = "{\"component\":\"Component\",\"props\":{\"feed\":{\"posts\":[\"Post 1\"]}},\"url\":\"/page\",\"version\":\"1\",\"encryptHistory\":false,\"clearHistory\":false,\"mergeProps\":[\"feed.posts\"]}";
        assertEquals(expectedJson, response.getBody());
    }

    @Test
    void render_withPartialExceptHeader_excludesProp() {
        var httpRequest = new FakeHttpRequest("GET", Map.of(
            "X-Inertia", "true",
            "X-Inertia-Partial-Component", "Component",
            "X-Inertia-Partial-Except", "secret"
        ));
        Map<String, Object> props = Map.of("user", "test", "secret", "shh");
        var options = new InertiaRenderingOptions(false, false, "/page", "Component", props);

        HttpResponse response = render(httpRequest, options);

        var expectedJson = "{\"component\":\"Component\",\"props\":{\"user\":\"test\"},\"url\":\"/page\",\"version\":\"1\",\"encryptHistory\":false,\"clearHistory\":false}";
        assertEquals(expectedJson, response.getBody());
    }

    @Test
    void render_withRescuedDeferProp_onPartialReload_reportsRescuedInsteadOfThrowing() {
        var httpRequest = new FakeHttpRequest("GET", Map.of(
            "X-Inertia", "true",
            "X-Inertia-Partial-Component", "Component",
            "X-Inertia-Partial-Data", "comments"
        ));
        Map<String, Object> props = Map.of(
            "comments", new DeferProp(() -> { throw new RuntimeException("falhou"); }).rescue()
        );
        var options = new InertiaRenderingOptions(false, false, "/page", "Component", props);

        HttpResponse response = render(httpRequest, options);

        var expectedJson = "{\"component\":\"Component\",\"props\":{},\"url\":\"/page\",\"version\":\"1\",\"encryptHistory\":false,\"clearHistory\":false,\"rescuedProps\":[\"comments\"]}";
        assertEquals(expectedJson, response.getBody());
    }

    @Test
    void render_withNonRescuedDeferProp_onPartialReload_propagatesException() {
        var httpRequest = new FakeHttpRequest("GET", Map.of(
            "X-Inertia", "true",
            "X-Inertia-Partial-Component", "Component",
            "X-Inertia-Partial-Data", "comments"
        ));
        Map<String, Object> props = Map.of(
            "comments", new DeferProp(() -> { throw new RuntimeException("falhou"); })
        );
        var options = new InertiaRenderingOptions(false, false, "/page", "Component", props);

        assertThrows(RuntimeException.class, () -> render(httpRequest, options));
    }

    @Test
    void render_withMismatchedPartialComponent_treatsAsFullVisit() {
        var httpRequest = new FakeHttpRequest("GET", Map.of(
            "X-Inertia", "true",
            "X-Inertia-Partial-Component", "OutraPagina",
            "X-Inertia-Partial-Data", "user"
        ));
        Map<String, Object> props = Map.of(
            "user", "test",
            "comments", new DeferProp(() -> {
                throw new AssertionError("nao deveria resolver num full visit");
            })
        );
        var options = new InertiaRenderingOptions(false, false, "/page", "Component", props);

        HttpResponse response = render(httpRequest, options);

        // O X-Inertia-Partial-Component pedido nao bate com o componente atual: tratado como
        // full visit (only-list "user" e ignorado, "comments" ainda vira deferredProps).
        var expectedJson = "{\"component\":\"Component\",\"props\":{\"user\":\"test\"},\"url\":\"/page\",\"version\":\"1\",\"encryptHistory\":false,\"clearHistory\":false,\"deferredProps\":{\"default\":[\"comments\"]}}";
        assertEquals(expectedJson, response.getBody());
    }

    @Test
    void render_withOnceProp_announcesMetadataAndIncludesValueOnFirstLoad() {
        var httpRequest = new FakeHttpRequest("GET", Map.of("X-Inertia", "true"));
        Map<String, Object> props = Map.of("planos", new OnceProp(() -> List.of("Basico")));
        var options = new InertiaRenderingOptions(false, false, "/page", "Component", props);

        HttpResponse response = render(httpRequest, options);

        var expectedJson = "{\"component\":\"Component\",\"props\":{\"planos\":[\"Basico\"]},\"url\":\"/page\",\"version\":\"1\",\"encryptHistory\":false,\"clearHistory\":false,\"onceProps\":{\"planos\":{\"prop\":\"planos\",\"expiresAt\":null}}}";
        assertEquals(expectedJson, response.getBody());
    }

    @Test
    void render_withOnceProp_clientAlreadyHasIt_excludesValueButStillAnnouncesMetadata() {
        var httpRequest = new FakeHttpRequest("GET", Map.of(
            "X-Inertia", "true",
            "X-Inertia-Except-Once-Props", "planos"
        ));
        Map<String, Object> props = Map.of(
            "planos", new OnceProp(() -> {
                throw new AssertionError("nao deveria resolver, cliente ja avisou que tem");
            })
        );
        var options = new InertiaRenderingOptions(false, false, "/page", "Component", props);

        HttpResponse response = render(httpRequest, options);

        var expectedJson = "{\"component\":\"Component\",\"props\":{},\"url\":\"/page\",\"version\":\"1\",\"encryptHistory\":false,\"clearHistory\":false,\"onceProps\":{\"planos\":{\"prop\":\"planos\",\"expiresAt\":null}}}";
        assertEquals(expectedJson, response.getBody());
    }

    @Test
    void render_withOnceProp_customKeyViaAs_excludedByCustomKeyNotPath() {
        var httpRequest = new FakeHttpRequest("GET", Map.of(
            "X-Inertia", "true",
            "X-Inertia-Except-Once-Props", "paises"
        ));
        Map<String, Object> props = Map.of(
            "countries", new OnceProp(() -> {
                throw new AssertionError("nao deveria resolver");
            }).as("paises")
        );
        var options = new InertiaRenderingOptions(false, false, "/page", "Component", props);

        HttpResponse response = render(httpRequest, options);

        // onceProps fica keyed pela chave customizada ("paises"), nao pelo path ("countries") —
        // e e essa chave customizada que o except-once header precisa citar pra excluir.
        var expectedJson = "{\"component\":\"Component\",\"props\":{},\"url\":\"/page\",\"version\":\"1\",\"encryptHistory\":false,\"clearHistory\":false,\"onceProps\":{\"paises\":{\"prop\":\"countries\",\"expiresAt\":null}}}";
        assertEquals(expectedJson, response.getBody());
    }

    @Test
    void render_withOnceProp_fresh_alwaysResolvesEvenIfClientClaimsCached() {
        var httpRequest = new FakeHttpRequest("GET", Map.of(
            "X-Inertia", "true",
            "X-Inertia-Except-Once-Props", "planos"
        ));
        Map<String, Object> props = Map.of("planos", new OnceProp(() -> List.of("Basico")).fresh());
        var options = new InertiaRenderingOptions(false, false, "/page", "Component", props);

        HttpResponse response = render(httpRequest, options);

        var expectedJson = "{\"component\":\"Component\",\"props\":{\"planos\":[\"Basico\"]},\"url\":\"/page\",\"version\":\"1\",\"encryptHistory\":false,\"clearHistory\":false,\"onceProps\":{\"planos\":{\"prop\":\"planos\",\"expiresAt\":null}}}";
        assertEquals(expectedJson, response.getBody());
    }

    @Test
    void render_withOnceProp_until_setsExpiresAt() {
        var httpRequest = new FakeHttpRequest("GET", Map.of("X-Inertia", "true"));
        Map<String, Object> props = Map.of("planos", new OnceProp(() -> List.of("Basico")).until(86_400_000L));
        var options = new InertiaRenderingOptions(false, false, "/page", "Component", props);

        HttpResponse response = render(httpRequest, options);

        String body = response.getBody();
        assertTrue(body.contains("\"onceProps\":{\"planos\":{\"prop\":\"planos\",\"expiresAt\":"));
        assertFalse(body.contains("\"expiresAt\":null"));
    }

    @Test
    void render_withDeferredOnceProp_alreadyLoaded_notAnnouncedAsDeferred() {
        var httpRequest = new FakeHttpRequest("GET", Map.of(
            "X-Inertia", "true",
            "X-Inertia-Except-Once-Props", "comentarios"
        ));
        Map<String, Object> props = Map.of(
            "comentarios", new DeferProp(() -> {
                throw new AssertionError("nao deveria resolver nem ser anunciado como deferred");
            }).once()
        );
        var options = new InertiaRenderingOptions(false, false, "/page", "Component", props);

        HttpResponse response = render(httpRequest, options);

        // Nao aparece em deferredProps (cliente ja tem copia valida) mas ainda aparece em
        // onceProps, pra o cache do cliente continuar sabendo da chave/expiracao.
        var expectedJson = "{\"component\":\"Component\",\"props\":{},\"url\":\"/page\",\"version\":\"1\",\"encryptHistory\":false,\"clearHistory\":false,\"onceProps\":{\"comentarios\":{\"prop\":\"comentarios\",\"expiresAt\":null}}}";
        assertEquals(expectedJson, response.getBody());
    }

    @Test
    void render_withMergeableOnceProp_bothMetadataPresent() {
        var httpRequest = new FakeHttpRequest("GET", Map.of("X-Inertia", "true"));
        Map<String, Object> props = Map.of("posts", new MergeProp(List.of("Post 1")).once());
        var options = new InertiaRenderingOptions(false, false, "/page", "Component", props);

        HttpResponse response = render(httpRequest, options);

        var expectedJson = "{\"component\":\"Component\",\"props\":{\"posts\":[\"Post 1\"]},\"url\":\"/page\",\"version\":\"1\",\"encryptHistory\":false,\"clearHistory\":false,\"mergeProps\":[\"posts\"],\"onceProps\":{\"posts\":{\"prop\":\"posts\",\"expiresAt\":null}}}";
        assertEquals(expectedJson, response.getBody());
    }

    private HttpResponse render(HttpRequest request, InertiaRenderingOptions options) {
        return new InertiaRenderer(
            pageObjectSerializer,
            versionProvider,
            "template.html"
        ).render(request, options);
    }

    private static String normalizeHtml(String html) {
        return html
            .replaceAll(">\\s+<", "><")
            .trim();
    }
}
