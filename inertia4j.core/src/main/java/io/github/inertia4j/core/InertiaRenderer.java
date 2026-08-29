package io.github.inertia4j.core;

import io.github.inertia4j.core.props.Deferrable;
import io.github.inertia4j.core.props.ResolvableProp;
import io.github.inertia4j.spi.PageObject;
import io.github.inertia4j.spi.PageObjectSerializer;
import io.github.inertia4j.spi.SerializationException;
import io.github.inertia4j.spi.TemplateRenderer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * The core class responsible for transforming regular web responses into Inertia-compatible responses.
 * It handles full page loads, partial updates, asset versioning, and redirects according to the Inertia protocol.
 */
public class InertiaRenderer {
    private final PageObjectSerializer pageObjectSerializer;
    private final TemplateRenderer templateRenderer;
    private final Supplier<String> versionProvider;

    /**
     * Constructs an InertiaRenderer with explicit dependencies.
     *
     * @param pageObjectSerializer PageObjectSerializer implementation used to serialize the {@link PageObject}.
     * @param versionProvider provider for the current Inertia asset version.
     * @param templateRenderer renderer for the base HTML template used in full page loads.
     */
    public InertiaRenderer(
        PageObjectSerializer pageObjectSerializer,
        Supplier<String> versionProvider,
        TemplateRenderer templateRenderer
    ) {
        this.pageObjectSerializer = pageObjectSerializer;
        this.templateRenderer = templateRenderer;
        this.versionProvider = versionProvider;
    }

    /**
     * Constructs an InertiaRenderer using the default {@link SimpleTemplateRenderer}.
     *
     * @param pageObjectSerializer PageObjectSerializer implementation used to serialize the {@link PageObject}.
     * @param versionProvider provider for the current Inertia asset version
     * @param templatePath path to the HTML template to be served
     * @throws TemplateRenderingException if the template file cannot be read.
     */
    public InertiaRenderer(
        PageObjectSerializer pageObjectSerializer,
        Supplier<String> versionProvider,
        String templatePath
    ) throws TemplateRenderingException {
        this(pageObjectSerializer, versionProvider, new SimpleTemplateRenderer(templatePath));
    }

    /**
     * Renders the response according to the Inertia protocol based on the incoming request and rendering options.
     * Handles full page loads, partial updates, and asset version conflicts.
     *
     * @param request The incoming HTTP request wrapper.
     * @param options rendering options containing component name, props, etc.
     * @return An {@link HttpResponse} object configured according to the Inertia protocol.
     * @throws SerializationException if the {@link PageObject} serialization fails.
     */
    public HttpResponse render(
        HttpRequest request,
        InertiaRenderingOptions options
    ) throws SerializationException {
        if (isVersionConflict(request)) {
            return handleVersionConflictResponse(request, options);
        }
        return handleSuccessResponse(request, options);
    }

    /**
     * Creates an appropriate redirect response based on the Inertia protocol.
     * Uses a 303 See Other redirect for PUT/PATCH/DELETE requests and a 302 Found for others.
     *
     * @param request The incoming HTTP request wrapper.
     * @param location URL to redirect to
     * @return An {@link HttpResponse} object configured for an Inertia redirect.
     */
    public HttpResponse redirect(
        HttpRequest request,
        String location
    ) {
        return new HttpResponse()
            .setCode(isPutPatchDelete(request) ? 303 : 302)
            .setHeader("Location", location);
    }

    /**
     * Instructs the client-side Inertia adapter to perform a hard visit to an external URL
     * by returning a 409 Conflict response with the `X-Inertia-Location` header.
     *
     * @param url The external URL to navigate to.
     * @return An {@link HttpResponse} object configured for an external redirect.
     */
    public HttpResponse location(String url) {
        return new HttpResponse()
            .setCode(409)
            .setHeader("X-Inertia-Location", url);
    }

    /**
     * Checks if the request indicates an asset version conflict.
     * This happens on GET requests where the `X-Inertia-Version` header doesn't match the current asset version.
     *
     * @param request The incoming HTTP request.
     * @return {@code true} if there's a version conflict, {@code false} otherwise.
     */
    private boolean isVersionConflict(HttpRequest request) {
        if (!request.getMethod().equalsIgnoreCase("GET")) return false;

        String versionHeader = request.getHeader("X-Inertia-Version");

        return versionHeader != null && !versionHeader.equals(versionProvider.get());
    }

    /**
     * Handles the response when an asset version conflict is detected.
     * Returns a 409 Conflict response with the `X-Inertia-Location` header set to the request URL.
     *
     * @param request The incoming HTTP request.
     * @param options The rendering options.
     * @return An {@link HttpResponse} for a version conflict.
     */
    private HttpResponse handleVersionConflictResponse(
        HttpRequest request,
        InertiaRenderingOptions options
    ) {
        return new HttpResponse()
            .setCode(409)
            .setHeader("X-Inertia-Location", options.url);
    }

    /**
     * Handles a standard successful Inertia request (not a version conflict or redirect).
     * Determines whether to return a full HTML response or a JSON response based on the `X-Inertia` header.
     *
     * @param request The incoming HTTP request.
     * @param options The rendering options.
     * @return An {@link HttpResponse} containing either the full HTML page or the JSON PageObject.
     * @throws SerializationException if PageObject serialization fails.
     */
    private HttpResponse handleSuccessResponse(
        HttpRequest request,
        InertiaRenderingOptions options
    ) throws SerializationException {
        var response = new HttpResponse();

        PageObject pageObject = pageObjectFromOptions(request, options);
        String serializedPageObject = serializePageObject(request, pageObject);

        String inertiaHeader = request.getHeader("X-Inertia");
        if (inertiaHeader != null && inertiaHeader.equalsIgnoreCase("true")) {
            response
                .setHeader("Content-Type", "application/json")
                .setHeader("X-Inertia", "true")
                .setBody(serializedPageObject);
        } else {
            response
                .setHeader("Content-Type", "text/html")
                .setBody(templateRenderer.render(serializedPageObject));
        }

        return response.setCode(200);
    }

    /**
     * Creates a {@link PageObject} instance from the provided rendering options.
     * Checks for the `X-Inertia-Partial-Component` header to potentially modify props based on
     * partial rendering requests, and resolves each prop according to the partial-reload filter
     * and {@link Deferrable}/{@link ResolvableProp} semantics (see {@link #resolveProps}).
     *
     * @param request The incoming HTTP request.
     * @param options The rendering options.
     * @return A configured {@link PageObject}.
     */
    private PageObject pageObjectFromOptions(HttpRequest request, InertiaRenderingOptions options) {
        String partialComponentHeader = request.getHeader("X-Inertia-Partial-Component");
        if (partialComponentHeader != null) {
            options = options.withPartialComponent(partialComponentHeader);
        }

        Map<String, Object> rawProps = options.props != null ? options.props : Map.of();
        Map<String, List<String>> deferredProps = new LinkedHashMap<>();
        Map<String, Object> resolvedProps = resolveProps(request, rawProps, deferredProps);

        return new PageObject(
            options.componentName,
            resolvedProps,
            options.url,
            options.encryptHistory,
            options.clearHistory,
            versionProvider.get(),
            deferredProps
        );
    }

    /**
     * Decides, for every raw prop, whether it is included (and resolved) in this response —
     * driven by the `X-Inertia-Partial-Data` only-list (when present) and each value's
     * {@link Deferrable}/{@link ResolvableProp} markers.
     * <p>
     * A {@link Deferrable} prop is never resolved on a full visit (no `X-Inertia-Partial-Data`
     * header at all): it is left out of the returned map and its key recorded into
     * {@code deferredPropsOut}, grouped by {@link Deferrable#getGroup()}, so the client knows to
     * issue a follow-up partial reload for it. On a partial reload it behaves like any other prop
     * — included only if its key is in the only-list.
     *
     * @param request the incoming HTTP request (read for the partial-reload only-list).
     * @param rawProps the props as passed to {@code Inertia.render()}, still possibly wrapping
     *                 {@link ResolvableProp}/{@link Deferrable} values.
     * @param deferredPropsOut mutated in place with the deferred prop keys skipped this response,
     *                         grouped by request group.
     * @return the props to actually put on the {@link PageObject}, with every
     *         {@link ResolvableProp} already resolved to its real value.
     */
    private Map<String, Object> resolveProps(
        HttpRequest request,
        Map<String, Object> rawProps,
        Map<String, List<String>> deferredPropsOut
    ) {
        List<String> onlyKeys = parseCsvHeader(request, "X-Inertia-Partial-Data");
        boolean isPartialReload = onlyKeys != null;

        Map<String, Object> resolvedProps = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : rawProps.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            if (value instanceof Deferrable) {
                if (!isPartialReload) {
                    String group = ((Deferrable) value).getGroup();
                    deferredPropsOut.computeIfAbsent(group, g -> new ArrayList<>()).add(key);
                    continue;
                }
                if (!onlyKeys.contains(key)) {
                    // Partial reload, but not (yet) asking for this deferred prop's group.
                    continue;
                }
            } else if (isPartialReload && !onlyKeys.contains(key)) {
                continue;
            }

            resolvedProps.put(key, resolveIfNeeded(value));
        }
        return resolvedProps;
    }

    private static Object resolveIfNeeded(Object value) {
        return value instanceof ResolvableProp ? ((ResolvableProp) value).resolve() : value;
    }

    /**
     * Serializes the {@link PageObject} into a JSON string.
     * Checks for the `X-Inertia-Partial-Data` header to determine if only a subset of props should be included in the JSON.
     *
     * @param request The incoming HTTP request.
     * @param pageObject The PageObject to serialize.
     * @return The JSON string representation of the PageObject.
     * @throws SerializationException if serialization fails.
     */
    private String serializePageObject(HttpRequest request, PageObject pageObject) throws SerializationException {
        return pageObjectSerializer.serialize(pageObject, parseCsvHeader(request, "X-Inertia-Partial-Data"));
    }

    /**
     * Parses a comma-separated header value (e.g. `X-Inertia-Partial-Data`) into a trimmed,
     * blank-filtered list.
     *
     * @param request the incoming HTTP request.
     * @param headerName the header to read.
     * @return the parsed list, or {@code null} if the header is absent — callers rely on this
     *         {@code null} to distinguish "no partial reload at all" from "partial reload naming
     *         zero props".
     */
    private static List<String> parseCsvHeader(HttpRequest request, String headerName) {
        String header = request.getHeader(headerName);
        if (header == null) {
            return null;
        }
        return Arrays.stream(header.split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .collect(Collectors.toList());
    }

    /**
     * Checks if the HTTP request method is PUT, PATCH, or DELETE.
     *
     * @param request The incoming HTTP request.
     * @return {@code true} if the method is PUT, PATCH, or DELETE, {@code false} otherwise.
     */
    private boolean isPutPatchDelete(HttpRequest request) {
        String requestMethod = request.getMethod();
        return (requestMethod.equalsIgnoreCase("PUT")
            || requestMethod.equalsIgnoreCase("PATCH")
            || requestMethod.equalsIgnoreCase("DELETE"));
    }
}
