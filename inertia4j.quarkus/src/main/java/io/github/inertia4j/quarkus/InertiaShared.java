package io.github.inertia4j.quarkus;

import jakarta.enterprise.context.RequestScoped;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Accumulates app-wide "shared data" for the current request — the equivalent of Laravel's
 * {@code Inertia::share()} / Rails' {@code inertia_share}. Your app registers its defaults here,
 * typically from a {@code ContainerRequestFilter} that runs on every request — mirroring how
 * {@code HandleInertiaRequests::share($request)}/a class-level {@code inertia_share} attaches
 * defaults without each controller action mentioning them (see the example filter in the
 * consuming app's source, e.g. {@code InertiaSharedDataFilter} in
 * <a href="https://github.com/jakjr/quarkus-inertia-lab">quarkus-inertia-lab</a>). The actual merge
 * into every page's props happens in {@link Inertia#render}, which reads {@link #all()} and passes
 * it as {@code InertiaRenderingOptions}' {@code sharedProps} argument — so a Resource can also call
 * {@link #share} itself (injecting this bean directly) to add request-specific shared data on top
 * of the filter's defaults, without a Resource ever having to pass either through {@code render}'s
 * {@code props} argument.
 * <p>
 * Unlike {@link InertiaFlash}/the {@code errors} handled by {@link InertiaSharedProps}, this is
 * <em>not</em> session-backed and has no pull-once lifecycle: shared data is computed fresh every
 * request (real {@code HandleInertiaRequests::share($request)}/{@code inertia_share} before_action
 * run per-request too, reading whatever is true right now — e.g. the authenticated user — not a
 * value queued by a previous request). It simply accumulates in memory for the lifetime of this
 * request-scoped bean.
 */
@RequestScoped
public class InertiaShared {

    private final Map<String, Object> data = new LinkedHashMap<>();

    /**
     * Shares a single value under {@code key}, delivered merged into every page's props.
     *
     * @throws NullPointerException if {@code key} is {@code null} — caught here rather than left
     *                               to surface as a confusing {@code List.copyOf()} failure two
     *                               layers away, inside {@link io.github.inertia4j.core.InertiaRenderer}.
     */
    public void share(String key, Object value) {
        data.put(Objects.requireNonNull(key, "shared prop key must not be null"), value);
    }

    /** Shares several values at once, merging with anything already shared this request. */
    public void share(Map<String, Object> values) {
        Objects.requireNonNull(values, "shared prop map must not be null").forEach(this::share);
    }

    Map<String, Object> all() {
        return data;
    }
}
