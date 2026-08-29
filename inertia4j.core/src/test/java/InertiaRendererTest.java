import io.github.inertia4j.core.*;
import io.github.inertia4j.core.props.AlwaysProp;
import io.github.inertia4j.core.props.DeferProp;
import io.github.inertia4j.core.props.MergeProp;
import io.github.inertia4j.core.props.OnceProp;
import io.github.inertia4j.core.props.OptionalProp;
import io.github.inertia4j.core.props.ScrollPage;
import io.github.inertia4j.core.props.ScrollProp;
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

    @Test
    void render_withEmptyPartialDataHeader_isTreatedAsNoOnlyFilter() {
        var httpRequest = new FakeHttpRequest("GET", Map.of(
            "X-Inertia", "true",
            "X-Inertia-Partial-Component", "Component",
            "X-Inertia-Partial-Data", "  ,  "
        ));
        Map<String, Object> props = Map.of("user", "test");
        var options = new InertiaRenderingOptions(false, false, "/page", "Component", props);

        HttpResponse response = render(httpRequest, options);

        // Um only-list vazio nao significa "o cliente pediu zero props" (o que esvaziaria a
        // pagina inteira) — significa que nao veio filtro nenhum, igual ao `?: null` do
        // PropsResolver::parseHeader() do Laravel.
        var expectedJson = "{\"component\":\"Component\",\"props\":{\"user\":\"test\"},\"url\":\"/page\",\"version\":\"1\",\"encryptHistory\":false,\"clearHistory\":false}";
        assertEquals(expectedJson, response.getBody());
    }

    @Test
    void render_withPropWrapperInsideList_resolvesItAndPrefixesPathWithIndex() {
        var httpRequest = new FakeHttpRequest("GET", Map.of("X-Inertia", "true"));
        Map<String, Object> props = Map.of(
            "secoes", List.of(
                Map.of("titulo", "Primeira"),
                Map.of("titulo", "Segunda", "itens", new MergeProp(List.of("Item 1")))
            )
        );
        var options = new InertiaRenderingOptions(false, false, "/page", "Component", props);

        HttpResponse response = render(httpRequest, options);

        // O PropsResolver do Laravel recursa em qualquer array (PHP nao separa lista de mapa),
        // entao um wrapper de prop dentro de uma lista tambem resolve — e seu path carrega o
        // indice, como as chaves numericas que o PHP produziria.
        var expectedJson = "{\"component\":\"Component\",\"props\":{\"secoes\":[{\"titulo\":\"Primeira\"},{\"itens\":[\"Item 1\"],\"titulo\":\"Segunda\"}]},\"url\":\"/page\",\"version\":\"1\",\"encryptHistory\":false,\"clearHistory\":false,\"mergeProps\":[\"secoes.1.itens\"]}";
        assertEquals(expectedJson, response.getBody());
    }

    @Test
    void render_withDeferredPropInsideList_isAnnouncedByIndexedPathAndNotResolved() {
        var httpRequest = new FakeHttpRequest("GET", Map.of("X-Inertia", "true"));
        Map<String, Object> props = Map.of(
            "widgets", List.of(new DeferProp(() -> {
                throw new AssertionError("deferred prop aninhado numa lista nao deveria resolver");
            }))
        );
        var options = new InertiaRenderingOptions(false, false, "/page", "Component", props);

        HttpResponse response = render(httpRequest, options);

        var expectedJson = "{\"component\":\"Component\",\"props\":{\"widgets\":[]},\"url\":\"/page\",\"version\":\"1\",\"encryptHistory\":false,\"clearHistory\":false,\"deferredProps\":{\"default\":[\"widgets.0\"]}}";
        assertEquals(expectedJson, response.getBody());
    }

    @Test
    void render_withScrollProp_mergesAtWrapperAndAnnouncesPagination() {
        var httpRequest = new FakeHttpRequest("GET", Map.of("X-Inertia", "true"));
        Map<String, Object> props = Map.of(
            "posts", new ScrollProp(
                () -> Map.of("data", List.of("Post 1")),
                ScrollPage.numbered("pagina", 1, true)
            )
        );
        var options = new InertiaRenderingOptions(false, false, "/page", "Component", props);

        HttpResponse response = render(httpRequest, options);

        // O merge acontece em "posts.data" (o wrapper), nao em "posts": a lista de itens cresce
        // enquanto os campos de paginacao ao redor dela sao substituidos pelos desta resposta.
        // previousPage null por estar na primeira pagina; nextPage 2 porque ainda ha mais.
        var expectedJson = "{\"component\":\"Component\",\"props\":{\"posts\":{\"data\":[\"Post 1\"]}},\"url\":\"/page\",\"version\":\"1\",\"encryptHistory\":false,\"clearHistory\":false,\"mergeProps\":[\"posts.data\"],\"scrollProps\":{\"posts\":{\"pageName\":\"pagina\",\"previousPage\":null,\"nextPage\":2,\"currentPage\":1,\"reset\":false}}}";
        assertEquals(expectedJson, response.getBody());
    }

    @Test
    void render_withScrollProp_prependIntent_prependsInsteadOfAppending() {
        var httpRequest = new FakeHttpRequest("GET", Map.of(
            "X-Inertia", "true",
            "X-Inertia-Partial-Component", "Component",
            "X-Inertia-Partial-Data", "posts",
            "X-Inertia-Infinite-Scroll-Merge-Intent", "prepend"
        ));
        Map<String, Object> props = Map.of(
            "posts", new ScrollProp(
                () -> Map.of("data", List.of("Post 0")),
                ScrollPage.numbered("pagina", 2, true)
            )
        );
        var options = new InertiaRenderingOptions(false, false, "/page", "Component", props);

        HttpResponse response = render(httpRequest, options);

        // Rolando pra cima o cliente manda "prepend" e o mesmo prop passa a instruir prependProps.
        var expectedJson = "{\"component\":\"Component\",\"props\":{\"posts\":{\"data\":[\"Post 0\"]}},\"url\":\"/page\",\"version\":\"1\",\"encryptHistory\":false,\"clearHistory\":false,\"prependProps\":[\"posts.data\"],\"scrollProps\":{\"posts\":{\"pageName\":\"pagina\",\"previousPage\":1,\"nextPage\":3,\"currentPage\":2,\"reset\":false}}}";
        assertEquals(expectedJson, response.getBody());
    }

    @Test
    void render_withScrollProp_lastPage_announcesNullNextPage() {
        var httpRequest = new FakeHttpRequest("GET", Map.of("X-Inertia", "true"));
        Map<String, Object> props = Map.of(
            "posts", new ScrollProp(
                () -> Map.of("data", List.of("Post 9")),
                ScrollPage.numbered("pagina", 3, false)
            )
        );
        var options = new InertiaRenderingOptions(false, false, "/page", "Component", props);

        HttpResponse response = render(httpRequest, options);

        // nextPage null e como o cliente sabe que acabou — nao ha flag "temMais" no protocolo.
        assertTrue(response.getBody().contains("\"scrollProps\":{\"posts\":{\"pageName\":\"pagina\",\"previousPage\":2,\"nextPage\":null,\"currentPage\":3,\"reset\":false}}"));
    }

    @Test
    void render_withScrollProp_resetHeader_flagsResetAndDropsMergeInstruction() {
        var httpRequest = new FakeHttpRequest("GET", Map.of(
            "X-Inertia", "true",
            "X-Inertia-Partial-Component", "Component",
            "X-Inertia-Partial-Data", "posts",
            "X-Inertia-Reset", "posts"
        ));
        Map<String, Object> props = Map.of(
            "posts", new ScrollProp(
                () -> Map.of("data", List.of("Post 1")),
                ScrollPage.numbered("pagina", 1, true)
            )
        );
        var options = new InertiaRenderingOptions(false, false, "/page", "Component", props);

        HttpResponse response = render(httpRequest, options);

        // X-Inertia-Reset zera a instrucao de merge (o cliente substitui) e o scrollProps sinaliza
        // reset:true pro componente de scroll infinito recomecar a paginacao.
        var expectedJson = "{\"component\":\"Component\",\"props\":{\"posts\":{\"data\":[\"Post 1\"]}},\"url\":\"/page\",\"version\":\"1\",\"encryptHistory\":false,\"clearHistory\":false,\"scrollProps\":{\"posts\":{\"pageName\":\"pagina\",\"previousPage\":null,\"nextPage\":2,\"currentPage\":1,\"reset\":true}}}";
        assertEquals(expectedJson, response.getBody());
    }

    @Test
    void render_withScrollProp_matchOn_prefixesPathsWithPropNotWrapper() {
        var httpRequest = new FakeHttpRequest("GET", Map.of("X-Inertia", "true"));
        Map<String, Object> props = Map.of(
            "posts", new ScrollProp(
                () -> Map.of("data", List.of(Map.of("id", 1))),
                ScrollPage.numbered("pagina", 1, false)
            ).matchOn("data.id")
        );
        var options = new InertiaRenderingOptions(false, false, "/page", "Component", props);

        HttpResponse response = render(httpRequest, options);

        // matchPropsOn e sempre "<path do prop>.<path relativo>" — o wrapper nao entra sozinho,
        // por isso o relativo aqui precisa ser "data.id" e nao so "id".
        assertTrue(response.getBody().contains("\"matchPropsOn\":[\"posts.data.id\"]"));
    }

    @Test
    void render_withDeferredScrollProp_onFullVisit_announcesGroupWithoutResolving() {
        var httpRequest = new FakeHttpRequest("GET", Map.of("X-Inertia", "true"));
        Map<String, Object> props = Map.of(
            "posts", new ScrollProp(
                () -> { throw new AssertionError("scroll prop deferido nao deveria resolver"); },
                ScrollPage.numbered("pagina", 1, true)
            ).defer()
        );
        var options = new InertiaRenderingOptions(false, false, "/page", "Component", props);

        HttpResponse response = render(httpRequest, options);

        // Sem scrollProps: o cursor so e anunciado quando o valor de fato vai na resposta. O
        // mergeProps sai no path raiz ("posts", nao "posts.data") porque a intencao de merge so e
        // aplicada na hora de resolver — mesma sequencia do PropsResolver.php real.
        var expectedJson = "{\"component\":\"Component\",\"props\":{},\"url\":\"/page\",\"version\":\"1\",\"encryptHistory\":false,\"clearHistory\":false,\"deferredProps\":{\"default\":[\"posts\"]},\"mergeProps\":[\"posts\"]}";
        assertEquals(expectedJson, response.getBody());
    }

    @Test
    void render_withScrollProp_metadataDerivedFromResolvedValue() {
        var httpRequest = new FakeHttpRequest("GET", Map.of("X-Inertia", "true"));
        Map<String, Object> pagina = Map.of("data", List.of("Post 1"), "numero", 4);
        Map<String, Object> props = Map.of(
            "posts", new ScrollProp(
                () -> pagina,
                "data",
                resolvido -> {
                    @SuppressWarnings("unchecked")
                    int numero = (int) ((Map<String, Object>) resolvido).get("numero");
                    return ScrollPage.numbered("pagina", numero, false);
                }
            )
        );
        var options = new InertiaRenderingOptions(false, false, "/page", "Component", props);

        HttpResponse response = render(httpRequest, options);

        // O provedor recebe o valor ja resolvido, entao um objeto de paginacao que carrega o
        // proprio estado nao precisa ser repetido a mao no render.
        assertTrue(response.getBody().contains("\"currentPage\":4"));
    }

    @Test
    void render_withScrollProp_onPartialReloadForAnotherProp_isNotAnnouncedAtAll() {
        var httpRequest = new FakeHttpRequest("GET", Map.of(
            "X-Inertia", "true",
            "X-Inertia-Partial-Component", "Component",
            "X-Inertia-Partial-Data", "user"
        ));
        Map<String, Object> props = Map.of(
            "user", "test",
            "posts", new ScrollProp(
                () -> { throw new AssertionError("prop fora do only-list nao deveria resolver"); },
                ScrollPage.numbered("pagina", 1, true)
            )
        );
        var options = new InertiaRenderingOptions(false, false, "/page", "Component", props);

        HttpResponse response = render(httpRequest, options);

        var expectedJson = "{\"component\":\"Component\",\"props\":{\"user\":\"test\"},\"url\":\"/page\",\"version\":\"1\",\"encryptHistory\":false,\"clearHistory\":false}";
        assertEquals(expectedJson, response.getBody());
    }

    @Test
    void render_withAlwaysProp_onFullVisit_resolvesLikeAnOrdinaryProp() {
        var httpRequest = new FakeHttpRequest("GET", Map.of("X-Inertia", "true"));
        Map<String, Object> props = Map.of("flash", new AlwaysProp(() -> "mensagem"));
        var options = new InertiaRenderingOptions(false, false, "/page", "Component", props);

        HttpResponse response = render(httpRequest, options);

        var expectedJson = "{\"component\":\"Component\",\"props\":{\"flash\":\"mensagem\"},\"url\":\"/page\",\"version\":\"1\",\"encryptHistory\":false,\"clearHistory\":false}";
        assertEquals(expectedJson, response.getBody());
    }

    @Test
    void render_withAlwaysProp_onPartialReloadForAnotherProp_isIncludedAnyway() {
        var httpRequest = new FakeHttpRequest("GET", Map.of(
            "X-Inertia", "true",
            "X-Inertia-Partial-Component", "Component",
            "X-Inertia-Partial-Data", "user"
        ));
        Map<String, Object> props = new LinkedHashMap<>();
        props.put("user", "test");
        props.put("flash", new AlwaysProp(() -> "mensagem"));
        var options = new InertiaRenderingOptions(false, false, "/page", "Component", props);

        HttpResponse response = render(httpRequest, options);

        // "flash" nao esta no only-list ("user"), mas AlwaysProp ignora o filtro por inteiro —
        // o mesmo `$prop instanceof AlwaysProp` que faz shouldIncludeInPartialResponse() devolver
        // true de cara no Laravel (e o `return true if prop.is_a?(AlwaysProp)` no Rails).
        var expectedJson = "{\"component\":\"Component\",\"props\":{\"flash\":\"mensagem\",\"user\":\"test\"},\"url\":\"/page\",\"version\":\"1\",\"encryptHistory\":false,\"clearHistory\":false}";
        assertEquals(expectedJson, response.getBody());
    }

    @Test
    void render_withAlwaysProp_onPartialReloadThatExceptsIt_isIncludedAnyway() {
        var httpRequest = new FakeHttpRequest("GET", Map.of(
            "X-Inertia", "true",
            "X-Inertia-Partial-Component", "Component",
            "X-Inertia-Partial-Except", "flash"
        ));
        Map<String, Object> props = new LinkedHashMap<>();
        props.put("user", "test");
        props.put("flash", new AlwaysProp(() -> "mensagem"));
        var options = new InertiaRenderingOptions(false, false, "/page", "Component", props);

        HttpResponse response = render(httpRequest, options);

        // AlwaysProp bypassa o only E o except — X-Inertia-Partial-Except nomeando "flash" nao
        // tem efeito nenhum sobre ele, diferente de um prop comum.
        var expectedJson = "{\"component\":\"Component\",\"props\":{\"flash\":\"mensagem\",\"user\":\"test\"},\"url\":\"/page\",\"version\":\"1\",\"encryptHistory\":false,\"clearHistory\":false}";
        assertEquals(expectedJson, response.getBody());
    }

    @Test
    void render_withOptionalProp_onFullVisit_isExcludedAndNotAnnouncedAnywhere() {
        var httpRequest = new FakeHttpRequest("GET", Map.of("X-Inertia", "true"));
        Map<String, Object> props = Map.of(
            "user", "test",
            "relatorio", new OptionalProp(() -> {
                throw new AssertionError("optional prop nao deveria resolver num full visit");
            })
        );
        var options = new InertiaRenderingOptions(false, false, "/page", "Component", props);

        HttpResponse response = render(httpRequest, options);

        // Ao contrario de um DeferProp, nao aparece nem em "props" nem em "deferredProps" — o
        // cliente nao tem absolutamente nenhuma pista de que "relatorio" existe.
        var expectedJson = "{\"component\":\"Component\",\"props\":{\"user\":\"test\"},\"url\":\"/page\",\"version\":\"1\",\"encryptHistory\":false,\"clearHistory\":false}";
        assertEquals(expectedJson, response.getBody());
    }

    @Test
    void render_withOptionalProp_onPartialReloadRequestingItExplicitly_resolvesIt() {
        var httpRequest = new FakeHttpRequest("GET", Map.of(
            "X-Inertia", "true",
            "X-Inertia-Partial-Component", "Component",
            "X-Inertia-Partial-Data", "relatorio"
        ));
        Map<String, Object> props = Map.of(
            "user", "test",
            "relatorio", new OptionalProp(() -> "relatorio completo")
        );
        var options = new InertiaRenderingOptions(false, false, "/page", "Component", props);

        HttpResponse response = render(httpRequest, options);

        var expectedJson = "{\"component\":\"Component\",\"props\":{\"relatorio\":\"relatorio completo\"},\"url\":\"/page\",\"version\":\"1\",\"encryptHistory\":false,\"clearHistory\":false}";
        assertEquals(expectedJson, response.getBody());
    }

    @Test
    void render_withOptionalProp_onPartialReloadForAnotherProp_isExcludedLikeAnOrdinaryProp() {
        var httpRequest = new FakeHttpRequest("GET", Map.of(
            "X-Inertia", "true",
            "X-Inertia-Partial-Component", "Component",
            "X-Inertia-Partial-Data", "user"
        ));
        Map<String, Object> props = Map.of(
            "user", "test",
            "relatorio", new OptionalProp(() -> {
                throw new AssertionError("prop fora do only-list nao deveria resolver");
            })
        );
        var options = new InertiaRenderingOptions(false, false, "/page", "Component", props);

        HttpResponse response = render(httpRequest, options);

        var expectedJson = "{\"component\":\"Component\",\"props\":{\"user\":\"test\"},\"url\":\"/page\",\"version\":\"1\",\"encryptHistory\":false,\"clearHistory\":false}";
        assertEquals(expectedJson, response.getBody());
    }

    @Test
    void render_withOptionalOnceProp_onFullVisit_announcesOnceMetadataWithoutDeferredEntry() {
        var httpRequest = new FakeHttpRequest("GET", Map.of("X-Inertia", "true"));
        Map<String, Object> props = Map.of(
            "relatorio", new OptionalProp(() -> {
                throw new AssertionError("optional prop nao deveria resolver num full visit");
            }).once()
        );
        var options = new InertiaRenderingOptions(false, false, "/page", "Component", props);

        HttpResponse response = render(httpRequest, options);

        // onceProps aparece (pro cliente saber a chave/expiracao caso venha a pedir "relatorio"
        // depois), mas nao ha "deferredProps" nenhum — diferente de um DeferProp().once(), que
        // apareceria em deferredProps mesmo assim.
        var expectedJson = "{\"component\":\"Component\",\"props\":{},\"url\":\"/page\",\"version\":\"1\",\"encryptHistory\":false,\"clearHistory\":false,\"onceProps\":{\"relatorio\":{\"prop\":\"relatorio\",\"expiresAt\":null}}}";
        assertEquals(expectedJson, response.getBody());
    }

    @Test
    void render_withOptionalOnceProp_clientAlreadyClaimsCache_stillAnnouncesOnceMetadataUnconditionally() {
        var httpRequest = new FakeHttpRequest("GET", Map.of(
            "X-Inertia", "true",
            "X-Inertia-Except-Once-Props", "relatorio"
        ));
        Map<String, Object> props = Map.of(
            "relatorio", new OptionalProp(() -> {
                throw new AssertionError("optional prop nao deveria resolver num full visit");
            }).once()
        );
        var options = new InertiaRenderingOptions(false, false, "/page", "Component", props);

        HttpResponse response = render(httpRequest, options);

        // O branch de OptionalProp no full visit nunca consulta wasAlreadyLoadedByClient() antes
        // de anunciar onceProps (mirror do excludeIgnoredProp() real: so a entrada em
        // deferredProps e condicionada a isso, o once e incondicional). Presente ou nao o header
        // X-Inertia-Except-Once-Props, o resultado e o mesmo — e exatamente esse "nao muda nada"
        // que este teste fixa: uma "otimizacao" tentadora de so anunciar once quando o cliente
        // ainda nao tem cache quebraria isso.
        var expectedJson = "{\"component\":\"Component\",\"props\":{},\"url\":\"/page\",\"version\":\"1\",\"encryptHistory\":false,\"clearHistory\":false,\"onceProps\":{\"relatorio\":{\"prop\":\"relatorio\",\"expiresAt\":null}}}";
        assertEquals(expectedJson, response.getBody());
    }

    @Test
    void render_withAlwaysProp_nestedUnderFilteredOutParent_isDroppedEntirely() {
        var httpRequest = new FakeHttpRequest("GET", Map.of(
            "X-Inertia", "true",
            "X-Inertia-Partial-Component", "Component",
            "X-Inertia-Partial-Data", "user"
        ));
        Map<String, Object> layout = new LinkedHashMap<>();
        layout.put("flash", new AlwaysProp(() -> {
            throw new AssertionError("nao deveria resolver - o pai 'layout' nem e alcancado");
        }));
        Map<String, Object> props = new LinkedHashMap<>();
        props.put("user", "test");
        props.put("layout", layout);
        var options = new InertiaRenderingOptions(false, false, "/page", "Component", props);

        HttpResponse response = render(httpRequest, options);

        // O bypass do AlwaysProp e por-prop, nao por-subarvore: "layout" em si nao bate nem leva
        // a "user" (o only-list), entao a recursao nunca chega no filho AlwaysProp pra sequer
        // considerar o bypass — mirror exato de shouldIncludeInPartialResponse()/keep_prop? reais,
        // que so consultam o class-check depois de a propria entrada ja estar sendo visitada.
        var expectedJson = "{\"component\":\"Component\",\"props\":{\"user\":\"test\"},\"url\":\"/page\",\"version\":\"1\",\"encryptHistory\":false,\"clearHistory\":false}";
        assertEquals(expectedJson, response.getBody());
    }

    @Test
    void render_withAlwaysProp_nestedUnderReachableParent_bypassesFilterAtDepth() {
        var httpRequest = new FakeHttpRequest("GET", Map.of(
            "X-Inertia", "true",
            "X-Inertia-Partial-Component", "Component",
            "X-Inertia-Partial-Data", "layout.outro"
        ));
        Map<String, Object> layout = new LinkedHashMap<>();
        layout.put("flash", new AlwaysProp(() -> "mensagem"));
        layout.put("titulo", "Feed"); // prop comum, nao pedida - deve sumir
        layout.put("outro", "valor");
        Map<String, Object> props = Map.of("layout", layout);
        var options = new InertiaRenderingOptions(false, false, "/page", "Component", props);

        HttpResponse response = render(httpRequest, options);

        // "layout" sobrevive so pra recursao alcancar "layout.outro" (leadsToPath); dentro dele,
        // "layout.flash" aparece mesmo sem ser o alvo do only (bypass do AlwaysProp funciona a
        // qualquer profundidade), mas "layout.titulo" some normalmente por nao estar no only-list.
        var expectedJson = "{\"component\":\"Component\",\"props\":{\"layout\":{\"flash\":\"mensagem\",\"outro\":\"valor\"}},\"url\":\"/page\",\"version\":\"1\",\"encryptHistory\":false,\"clearHistory\":false}";
        assertEquals(expectedJson, response.getBody());
    }

    @Test
    void render_withOptionalPropInsideNestedMap_onFullVisit_isExcluded() {
        var httpRequest = new FakeHttpRequest("GET", Map.of("X-Inertia", "true"));
        Map<String, Object> layout = new LinkedHashMap<>();
        layout.put("titulo", "Feed");
        layout.put("relatorio", new OptionalProp(() -> {
            throw new AssertionError("optional prop aninhado num map nao deveria resolver");
        }));
        Map<String, Object> props = Map.of("layout", layout);
        var options = new InertiaRenderingOptions(false, false, "/page", "Component", props);

        HttpResponse response = render(httpRequest, options);

        var expectedJson = "{\"component\":\"Component\",\"props\":{\"layout\":{\"titulo\":\"Feed\"}},\"url\":\"/page\",\"version\":\"1\",\"encryptHistory\":false,\"clearHistory\":false}";
        assertEquals(expectedJson, response.getBody());
    }

    @Test
    void render_withOptionalPropInsideList_onFullVisit_isExcludedAndListCompacts() {
        var httpRequest = new FakeHttpRequest("GET", Map.of("X-Inertia", "true"));
        Map<String, Object> props = Map.of(
            "widgets", List.of(
                "a",
                new OptionalProp(() -> {
                    throw new AssertionError("optional prop aninhado numa lista nao deveria resolver");
                }),
                "c"
            )
        );
        var options = new InertiaRenderingOptions(false, false, "/page", "Component", props);

        HttpResponse response = render(httpRequest, options);

        // Diferente de um DeferProp aninhado numa lista (que ainda e anunciado, so por indice, em
        // deferredProps), um OptionalProp exclui o item por completo - a lista so compacta os que
        // sobraram, sem deixar buraco/indice pulado.
        var expectedJson = "{\"component\":\"Component\",\"props\":{\"widgets\":[\"a\",\"c\"]},\"url\":\"/page\",\"version\":\"1\",\"encryptHistory\":false,\"clearHistory\":false}";
        assertEquals(expectedJson, response.getBody());
    }

    @Test
    void render_withScrollProp_once_throwsUnsupportedOperationException() {
        var prop = new ScrollProp(
            () -> Map.of("data", List.of("Post 1")),
            ScrollPage.numbered("pagina", 1, false)
        );

        // Real ScrollProp.php/scroll_prop.rb nunca implementam Onceable - cachear uma pagina de
        // scroll faria o cliente parar de pedir scrollProps/mergeProps pra ela, travando o scroll
        // infinito em silencio. Bloqueado no unico ponto de entrada que poderia ligar isso.
        assertThrows(UnsupportedOperationException.class, prop::once);
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
