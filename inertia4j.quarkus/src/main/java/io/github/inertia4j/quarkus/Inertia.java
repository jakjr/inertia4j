package io.github.inertia4j.quarkus;

import io.github.inertia4j.core.InertiaRenderingOptions;
import io.github.inertia4j.core.props.AlwaysProp;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;

import java.util.HashMap;
import java.util.Map;

/**
 * CDI bean injectable into JAX-RS resources to render Inertia responses,
 * mirroring the {@code Inertia} bean of the official Spring adapter.
 */
@RequestScoped
public class Inertia {

    @Context
    ContainerRequestContext requestContext;

    @Inject
    InertiaQuarkusRenderer renderer;

    @Inject
    InertiaSharedProps sharedProps;

    @Inject
    InertiaShared shared;

    /**
     * Renders a page with {@code props}, plus whatever {@link InertiaSharedProps}/
     * {@link InertiaShared} have for this request: validation {@code errors} (defaulting to
     * {@code {}}, folded into {@code props} — see {@link InertiaValidationExceptionMapper}),
     * one-shot {@code flash} data (queued via {@link InertiaFlash}, attached outside {@code props}
     * — matching how inertia-laravel resolves {@code flash} outside {@code PropsResolver}), and
     * app-wide shared data (registered via {@link InertiaShared}, typically defaulted every request
     * by a {@code ContainerRequestFilter} your app provides — matching
     * {@code Inertia::share()}/{@code inertia_share}). Resources never pass any of these themselves;
     * all three are attached here the same way Laravel's
     * {@code HandleInertiaRequests::share()}/{@code Inertia::flash()} attach them to every
     * response.
     * <p>
     * {@code errors} is wrapped in {@link AlwaysProp} — mirroring
     * {@code Inertia::always($this->resolveValidationErrors($request))} in the real
     * {@code HandleInertiaRequests::share()} — so it survives a partial reload's
     * {@code only}/{@code except} filter (e.g. a page's "load more" button) instead of silently
     * disappearing just because the caller didn't think to name "errors" in its {@code only} list.
     * {@code shared} data, by contrast, is passed as {@link InertiaRenderingOptions}'
     * {@code sharedProps} argument — a plain value merged into {@code props} ahead of resolution,
     * so it <em>is</em> filtered by only/except like any page prop (matching real
     * {@code PropsResolver::resolve()} semantics — see {@code InertiaRendererTest}), it just isn't
     * ever missing from every page the way an unmentioned page prop would be.
     */
    public Response render(String component, Map<String, Object> props) {
        var req = new InertiaJaxRsRequest(requestContext);
        String url = requestContext.getUriInfo().getPath();
        var allProps = new HashMap<>(props);
        allProps.put("errors", new AlwaysProp(sharedProps::getErrors));
        var options = new InertiaRenderingOptions(
            false, false, url, component, allProps, sharedProps.getFlash(), shared.all()
        );
        return renderer.render(req, options);
    }

    public Response redirect(String location) {
        return renderer.redirect(new InertiaJaxRsRequest(requestContext), location);
    }
}
