package io.github.inertia4j.quarkus;

import io.github.inertia4j.core.HttpRequest;
import io.github.inertia4j.core.HttpResponse;
import io.github.inertia4j.core.InertiaRenderer;
import io.github.inertia4j.core.InertiaRenderingOptions;
import io.github.inertia4j.core.JacksonPageObjectSerializer;
import io.github.inertia4j.core.TemplateRenderingException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.Response;

/**
 * Wraps the framework-agnostic {@link InertiaRenderer} and adapts its {@link HttpResponse}
 * to a JAX-RS {@link Response}. Where the base HTML template (the app shell) comes from depends on
 * how Quinoa is serving the frontend &mdash; see {@link QuinoaTemplateRenderers}.
 */
@ApplicationScoped
class InertiaQuarkusRenderer {
    private final InertiaRenderer coreRenderer;

    InertiaQuarkusRenderer() throws TemplateRenderingException {
        this.coreRenderer = new InertiaRenderer(
            new JacksonPageObjectSerializer(),
            () -> "1",
            QuinoaTemplateRenderers.resolve()
        );
    }

    Response render(HttpRequest request, InertiaRenderingOptions options) {
        return toJaxRsResponse(coreRenderer.render(request, options));
    }

    Response redirect(HttpRequest request, String location) {
        return toJaxRsResponse(coreRenderer.redirect(request, location));
    }

    Response location(String url) {
        return toJaxRsResponse(coreRenderer.location(url));
    }

    private Response toJaxRsResponse(HttpResponse r) {
        Response.ResponseBuilder builder = Response.status(r.getCode()).entity(r.getBody());
        r.getHeaders().forEach((name, values) -> values.forEach(v -> builder.header(name, v)));
        return builder.build();
    }
}
