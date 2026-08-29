package io.github.inertia4j.core;

import io.github.inertia4j.core.props.AlwaysProp;
import io.github.inertia4j.core.props.Deferrable;
import io.github.inertia4j.core.props.Mergeable;
import io.github.inertia4j.core.props.Onceable;
import io.github.inertia4j.core.props.OptionalProp;
import io.github.inertia4j.core.props.ProvidesScrollMetadata;
import io.github.inertia4j.core.props.Rescuable;
import io.github.inertia4j.core.props.ResolvableProp;
import io.github.inertia4j.core.props.ScrollProp;
import io.github.inertia4j.spi.MergeInstructions;
import io.github.inertia4j.spi.OnceMetadata;
import io.github.inertia4j.spi.ScrollMetadata;
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
    /**
     * Returned by {@link #resolveProp} instead of a value, for a prop that must not appear in the
     * response at all — distinct from {@code null}, which is a legitimate resolved prop value.
     */
    private static final Object SKIP = new Object();

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
        String serializedPageObject = serializePageObject(pageObject);

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
     * Creates a {@link PageObject} instance from the provided rendering options, resolving props
     * according to the partial-reload filter and {@link Deferrable}/{@link ResolvableProp}/
     * {@link Mergeable}/{@link Onceable}/{@link Rescuable} semantics (see {@link #resolveProps}).
     *
     * @param request The incoming HTTP request.
     * @param options The rendering options.
     * @return A configured {@link PageObject}.
     */
    private PageObject pageObjectFromOptions(HttpRequest request, InertiaRenderingOptions options) {
        Map<String, Object> rawProps = options.props != null ? options.props : Map.of();
        ResolutionContext ctx = new ResolutionContext(request, options.componentName);
        Map<String, Object> resolvedProps = resolveProps(rawProps, "", false, ctx);

        return new PageObject(
            options.componentName,
            resolvedProps,
            options.url,
            options.encryptHistory,
            options.clearHistory,
            versionProvider.get(),
            ctx.deferredProps,
            new MergeInstructions(ctx.mergeProps, ctx.prependProps, ctx.deepMergeProps, ctx.matchPropsOn),
            ctx.rescuedProps,
            ctx.scrollProps,
            ctx.onceProps
        );
    }

    /**
     * Everything one {@link #pageObjectFromOptions} call needs to resolve a props tree, mirroring
     * {@code Inertia\PropsResolver} (inertia-laravel): the partial-reload signal and its
     * only/except/reset lists (fixed for the whole resolution), plus the metadata every recursive
     * {@link #resolveProps} call accumulates into as it walks the tree.
     * <p>
     * Whether this is a partial reload is decided the same way Laravel does it — by comparing
     * {@code X-Inertia-Partial-Component} against the component actually being rendered, not by
     * merely checking whether {@code X-Inertia-Partial-Data} is present. A client that still has
     * a stale/different page open (and so sends a mismatched component name) gets the full prop
     * tree back, ignoring its only/except lists, instead of a response filtered against the wrong
     * page's expectations.
     */
    private static final class ResolutionContext {
        final boolean isPartial;
        final boolean isInertiaRequest;
        final List<String> onlyPaths;
        final List<String> exceptPaths;
        final List<String> resetPaths;
        final List<String> exceptOncePaths;
        final boolean prependScrollIntent;

        final Map<String, List<String>> deferredProps = new LinkedHashMap<>();
        final List<String> mergeProps = new ArrayList<>();
        final List<String> prependProps = new ArrayList<>();
        final List<String> deepMergeProps = new ArrayList<>();
        final List<String> matchPropsOn = new ArrayList<>();
        final List<String> rescuedProps = new ArrayList<>();
        final Map<String, ScrollMetadata> scrollProps = new LinkedHashMap<>();
        final Map<String, OnceMetadata> onceProps = new LinkedHashMap<>();

        ResolutionContext(HttpRequest request, String componentName) {
            String partialComponentHeader = request.getHeader("X-Inertia-Partial-Component");
            this.isPartial = partialComponentHeader != null && partialComponentHeader.equals(componentName);
            this.isInertiaRequest = "true".equalsIgnoreCase(request.getHeader("X-Inertia"));
            this.onlyPaths = parseCsvHeader(request, "X-Inertia-Partial-Data");
            this.exceptPaths = parseCsvHeader(request, "X-Inertia-Partial-Except");
            List<String> reset = parseCsvHeader(request, "X-Inertia-Reset");
            this.resetPaths = reset != null ? reset : List.of();
            List<String> exceptOnce = parseCsvHeader(request, "X-Inertia-Except-Once-Props");
            this.exceptOncePaths = exceptOnce != null ? exceptOnce : List.of();
            // Anything other than the literal "prepend" means append — including the header being
            // absent, which is the ordinary scroll-down case (mirrors ScrollProp's
            // `=== 'prepend' ? prepend(...) : append(...)`).
            this.prependScrollIntent =
                "prepend".equals(request.getHeader("X-Inertia-Infinite-Scroll-Merge-Intent"));
        }
    }

    /**
     * Recursively resolves a props tree (or sub-tree) into the values actually sent to the
     * client, collecting deferred/merge/rescue metadata along the way — the Java equivalent of
     * {@code PropsResolver::resolveProps()} in inertia-laravel. Recursion (not just a flat
     * top-level pass) is what lets a {@link Deferrable}/{@link Mergeable} prop live at any depth
     * (e.g. {@code feed.posts}), and what lets `X-Inertia-Partial-Data`/`X-Inertia-Partial-Except`
     * target a nested path.
     *
     * @param props the props at this level — top-level props on the outermost call, or a nested
     *              {@code Map} value's own props on a recursive call.
     * @param prefix the dotted path leading to {@code props} (empty at the top level).
     * @param parentWasResolved whether an ancestor of {@code props} was itself produced by
     *                          resolving a {@link ResolvableProp}/callback rather than being a
     *                          plain {@code Map} already — such a value was explicitly requested
     *                          as a whole, so its children bypass further only/except filtering
     *                          (mirrors {@code $parentWasResolved} in the Laravel source).
     * @param ctx the resolution-wide signals ({@link ResolutionContext#isPartial} and its
     *            only/except/reset lists) and the metadata lists mutated as props are visited.
     * @return the resolved props at this level, with every {@link ResolvableProp} already
     *         resolved to its real value and nested {@code Map} values recursively resolved too.
     */
    private Map<String, Object> resolveProps(
        Map<String, Object> props,
        String prefix,
        boolean parentWasResolved,
        ResolutionContext ctx
    ) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : props.entrySet()) {
            String key = entry.getKey();
            String path = prefix.isEmpty() ? key : prefix + "." + key;
            Object value = resolveProp(entry.getValue(), path, parentWasResolved, ctx);
            if (value != SKIP) {
                result.put(key, value);
            }
        }
        return result;
    }

    /**
     * Same as {@link #resolveProps} but for a {@code List} value. PHP makes no distinction
     * between a map and a list, so {@code PropsResolver::resolveProps()}'s single {@code is_array}
     * recursion covers both — meaning a prop wrapper nested inside a list resolves there just
     * like one nested inside a map. Java needs the two cases spelled out separately to keep a
     * list serializing as a JSON array; element paths use the index ({@code "posts.0"}), matching
     * the numeric keys PHP would produce.
     */
    private List<Object> resolveList(
        List<?> items,
        String prefix,
        boolean parentWasResolved,
        ResolutionContext ctx
    ) {
        List<Object> result = new ArrayList<>(items.size());
        int index = 0;
        for (Object item : items) {
            String path = prefix + "." + index++;
            Object value = resolveProp(item, path, parentWasResolved, ctx);
            if (value != SKIP) {
                result.add(value);
            }
        }
        return result;
    }

    /**
     * Resolves a single prop found at {@code path}, collecting its metadata, or returns
     * {@link #SKIP} to signal that it must not appear in the response at all (filtered out by
     * the partial reload, deferred to a follow-up request, already cached client-side, or
     * rescued after throwing).
     */
    private Object resolveProp(Object prop, String path, boolean parentWasResolved, ResolutionContext ctx) {
        if (!shouldInclude(prop, path, parentWasResolved, ctx)) {
            return SKIP;
        }

        // Full visit only (mirrors excludeFromInitialResponse: only ever consulted when
        // !isPartial — a client explicitly asking for a prop again via a partial reload
        // gets it resolved fresh regardless of any cached copy it claims to have).
        if (!ctx.isPartial) {
            // A Deferrable prop is never resolved now, only announced — unless the client
            // already has a fresh once-cached copy, in which case it isn't even announced
            // (nothing to fetch: the client already has what it needs).
            if (prop instanceof Deferrable && ((Deferrable) prop).shouldDefer()) {
                if (!wasAlreadyLoadedByClient(prop, path, ctx)) {
                    String group = ((Deferrable) prop).getGroup();
                    ctx.deferredProps.computeIfAbsent(group, g -> new ArrayList<>()).add(path);
                }
                if (prop instanceof Mergeable) {
                    recordMergeInstructions(path, (Mergeable) prop, ctx);
                }
                if (prop instanceof Onceable) {
                    recordOnceMetadata(path, (Onceable) prop, ctx);
                }
                return SKIP;
            }
            // An OptionalProp is excluded from every full visit too, but — unlike a deferred
            // prop — it is never announced anywhere: no deferredProps entry, so the client has
            // no way to know it exists until it asks for it by name in a partial reload. Once
            // metadata is still recorded unconditionally here (not gated on whether the client
            // already claims a cached copy), mirroring excludeIgnoredProp()/keep_prop? for a
            // prop that is IgnoreFirstLoad without being Deferrable.
            if (prop instanceof OptionalProp) {
                if (prop instanceof Onceable) {
                    recordOnceMetadata(path, (Onceable) prop, ctx);
                }
                return SKIP;
            }
            // Not deferred, but the client already has a fresh once-cached copy: skip
            // resolving it (still announced under onceProps so the client's cache entry
            // survives, matching the expiry/key it was given the first time).
            if (ctx.isInertiaRequest && wasAlreadyLoadedByClient(prop, path, ctx)) {
                if (prop instanceof Onceable) {
                    recordOnceMetadata(path, (Onceable) prop, ctx);
                }
                return SKIP;
            }
        }

        // The client decides the scroll direction per request, so the prop only learns where (and
        // which way) it merges here, right before resolving — mirroring where resolveValue()
        // calls configureMergeIntent() in the Laravel source. Note this is deliberately NOT done
        // in the deferred branch above, exactly as in Laravel: a scroll prop deferred out of the
        // initial response announces a plain root-level merge there, and only gets its real
        // wrapper-scoped instruction on the follow-up request that actually carries an intent.
        if (prop instanceof ScrollProp) {
            prop = ((ScrollProp) prop).configureMergeIntent(ctx.prependScrollIntent);
        }

        boolean shouldRescue = prop instanceof Rescuable && ((Rescuable) prop).shouldRescue();
        Object value;
        try {
            value = resolveIfNeeded(prop);
        } catch (RuntimeException e) {
            if (!shouldRescue) {
                throw e;
            }
            ctx.rescuedProps.add(path);
            return SKIP;
        }

        if (prop instanceof Mergeable) {
            recordMergeInstructions(path, (Mergeable) prop, ctx);
        }
        if (prop instanceof ScrollProp) {
            recordScrollMetadata(path, (ScrollProp) prop, value, ctx);
        }
        if (prop instanceof Onceable) {
            recordOnceMetadata(path, (Onceable) prop, ctx);
        }

        // A value that was produced by resolving a wrapper/callback (rather than having been a
        // plain container all along) was requested as a whole, so its children bypass further
        // only/except filtering — mirrors `$parentWasResolved || ! is_array($prop)`.
        boolean childParentWasResolved = parentWasResolved || !isContainer(prop);
        if (value instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> nested = (Map<String, Object>) value;
            return resolveProps(nested, path, childParentWasResolved, ctx);
        }
        if (value instanceof List) {
            return resolveList((List<?>) value, path, childParentWasResolved, ctx);
        }
        return value;
    }

    /** Whether {@code prop} is one of the container shapes {@link #resolveProp} recurses into. */
    private static boolean isContainer(Object prop) {
        return prop instanceof Map || prop instanceof List;
    }

    /**
     * Mirrors {@code shouldIncludeInPartialResponse()}: on a full visit (or for children of an
     * already-resolved value) everything is included; on a partial reload, a path is included
     * when it matches (or is an ancestor/descendant of) the only-list, and isn't excepted —
     * unless {@code prop} is an {@link AlwaysProp}, which bypasses the only/except filter
     * entirely (both reference adapters special-case it with this same direct class check,
     * rather than an interface — see {@link AlwaysProp}'s javadoc).
     */
    private static boolean shouldInclude(Object prop, String path, boolean parentWasResolved, ResolutionContext ctx) {
        if (!ctx.isPartial || prop instanceof AlwaysProp || parentWasResolved) {
            return true;
        }
        if (ctx.onlyPaths != null && !matchesPath(path, ctx.onlyPaths) && !leadsToPath(path, ctx.onlyPaths)) {
            return false;
        }
        return ctx.exceptPaths == null || !matchesPath(path, ctx.exceptPaths);
    }

    /**
     * Mirrors {@code isIncludedInPartialMetadata()}: stricter than {@link #shouldInclude} — no
     * "leads to" allowance — since a container path only present to let recursion reach a deeper
     * only-target shouldn't itself be flagged as mergeable.
     */
    private static boolean isIncludedInPartialMetadata(String path, ResolutionContext ctx) {
        if (ctx.onlyPaths != null && !matchesPath(path, ctx.onlyPaths)) {
            return false;
        }
        return ctx.exceptPaths == null || !matchesPath(path, ctx.exceptPaths);
    }

    /** Whether {@code path} equals, or is a descendant of, one of {@code candidates}. */
    private static boolean matchesPath(String path, List<String> candidates) {
        for (String candidate : candidates) {
            if (path.equals(candidate) || path.startsWith(candidate + ".")) {
                return true;
            }
        }
        return false;
    }

    /** Whether {@code path} is an ancestor of one of {@code candidates}. */
    private static boolean leadsToPath(String path, List<String> candidates) {
        for (String candidate : candidates) {
            if (candidate.startsWith(path + ".")) {
                return true;
            }
        }
        return false;
    }

    private static void recordMergeInstructions(String path, Mergeable mergeable, ResolutionContext ctx) {
        if (!mergeable.shouldMerge()) {
            return;
        }
        if (ctx.resetPaths.contains(path)) {
            return;
        }
        if (ctx.isPartial && !isIncludedInPartialMetadata(path, ctx)) {
            return;
        }
        // Mirrors collectMergeableMetadata()'s branch order: a deep merge wins outright, then a
        // root-level append/prepend, and only when the prop named sub-paths to merge at (as a
        // ScrollProp does with its wrapper) does each of those become its own instruction.
        if (mergeable.getMergeStrategy() == Mergeable.Strategy.DEEP) {
            ctx.deepMergeProps.add(path);
        } else if (mergeable.mergesAtRoot()) {
            if (mergeable.getMergeStrategy() == Mergeable.Strategy.PREPEND) {
                ctx.prependProps.add(path);
            } else {
                ctx.mergeProps.add(path);
            }
        } else {
            for (String appendPath : mergeable.getAppendsAtPaths()) {
                ctx.mergeProps.add(path + "." + appendPath);
            }
            for (String prependPath : mergeable.getPrependsAtPaths()) {
                ctx.prependProps.add(path + "." + prependPath);
            }
        }
        for (String relativePath : mergeable.getMatchOn()) {
            ctx.matchPropsOn.add(path + "." + relativePath);
        }
    }

    /**
     * Mirrors {@code collectScrollMetadata()}: announces {@code prop}'s pagination cursor under
     * {@code scrollProps}, keyed by its path, plus whether {@code X-Inertia-Reset} named it (in
     * which case the client throws away the pages it had accumulated instead of growing them).
     * <p>
     * Unlike merge and once metadata, this is recorded unconditionally for an included scroll
     * prop — no partial-metadata filter — again matching the Laravel source: a scroll prop that
     * made it this far <em>is</em> in the response, and the client cannot paginate it without
     * knowing where it currently stands.
     */
    private static void recordScrollMetadata(
        String path,
        ScrollProp prop,
        Object resolvedValue,
        ResolutionContext ctx
    ) {
        ProvidesScrollMetadata metadata = prop.getScrollMetadata(resolvedValue);
        ctx.scrollProps.put(path, new ScrollMetadata(
            metadata.getPageName(),
            metadata.getPreviousPage(),
            metadata.getNextPage(),
            metadata.getCurrentPage(),
            ctx.resetPaths.contains(path)
        ));
    }

    /**
     * Mirrors {@code wasAlreadyLoadedByClient()}: {@code prop} is a once prop, not forced fresh,
     * and the client already announced (via {@code X-Inertia-Except-Once-Props}) that it has a
     * valid cached copy — keyed by its custom {@link Onceable#getKey()} if it set one, its path
     * otherwise.
     */
    private static boolean wasAlreadyLoadedByClient(Object prop, String path, ResolutionContext ctx) {
        if (!(prop instanceof Onceable)) {
            return false;
        }
        Onceable onceable = (Onceable) prop;
        if (!onceable.shouldResolveOnce() || onceable.shouldBeRefreshed()) {
            return false;
        }
        String key = onceable.getKey() != null ? onceable.getKey() : path;
        return ctx.exceptOncePaths.contains(key);
    }

    /**
     * Mirrors {@code collectOnceMetadata()}: announces {@code prop} under {@code onceProps},
     * keyed the same way {@link #wasAlreadyLoadedByClient} looks it up, so a later request can
     * reference it back — regardless of whether the value itself was included this response.
     */
    private static void recordOnceMetadata(String path, Onceable onceable, ResolutionContext ctx) {
        if (!onceable.shouldResolveOnce()) {
            return;
        }
        if (ctx.isPartial && !isIncludedInPartialMetadata(path, ctx)) {
            return;
        }
        String key = onceable.getKey() != null ? onceable.getKey() : path;
        ctx.onceProps.put(key, new OnceMetadata(path, onceable.getExpiresAtMillis()));
    }

    private static Object resolveIfNeeded(Object value) {
        return value instanceof ResolvableProp ? ((ResolvableProp) value).resolve() : value;
    }

    /**
     * Serializes the {@link PageObject} into a JSON string.
     * <p>
     * {@code pageObject.getProps()} is already exactly the partial-reload-filtered set — see
     * {@link #resolveProps}, which (unlike a post-hoc filter over the serialized JSON) can follow
     * a nested `only`/`except` path like {@code "feed.posts"} correctly. So there is nothing left
     * for the serializer to filter; the {@code partialDataProps} parameter on
     * {@link PageObjectSerializer} exists for serializer implementations that might still want it,
     * not because this renderer needs it applied again.
     *
     * @param pageObject The PageObject to serialize.
     * @return The JSON string representation of the PageObject.
     * @throws SerializationException if serialization fails.
     */
    private String serializePageObject(PageObject pageObject) throws SerializationException {
        return pageObjectSerializer.serialize(pageObject, null);
    }

    /**
     * Parses a comma-separated header value (e.g. `X-Inertia-Partial-Data`) into a trimmed,
     * blank-filtered list.
     *
     * @param request the incoming HTTP request.
     * @param headerName the header to read.
     * @return the parsed list, or {@code null} if the header is absent <em>or names nothing</em> —
     *         callers rely on this {@code null} to mean "no filter at all". Mirrors
     *         {@code PropsResolver::parseHeader()}, whose {@code array_filter(...) ?: null}
     *         collapses an empty/blank header to {@code null} too: a present-but-empty
     *         {@code X-Inertia-Partial-Data} must not be read as "the client asked for zero
     *         props" (which would strip the whole page), it means the client sent no only-list.
     */
    private static List<String> parseCsvHeader(HttpRequest request, String headerName) {
        String header = request.getHeader(headerName);
        if (header == null) {
            return null;
        }
        List<String> values = Arrays.stream(header.split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .collect(Collectors.toList());
        return values.isEmpty() ? null : values;
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
