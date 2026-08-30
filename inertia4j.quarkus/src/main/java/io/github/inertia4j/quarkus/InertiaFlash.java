package io.github.inertia4j.quarkus;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.Session;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Injectable bean for queuing one-shot flash data (e.g. a toast message) to be delivered with the
 * <em>next</em> Inertia response — the equivalent of Laravel's {@code Inertia::flash()}. Written
 * to the HTTP session under its own key, read and removed exactly once by
 * {@link InertiaSharedProps} — same pull-once lifecycle {@code errors} already has (see
 * {@link InertiaValidationExceptionMapper}), same session/{@code RedisSessionStore} set up by
 * {@link InertiaSessionSetup}, no new infrastructure.
 * <p>
 * Unlike {@code errors} (which lands inside {@code props}), a flashed value is attached outside
 * {@code props} by {@link Inertia#render} — mirroring how {@code resolveFlashData()} in
 * inertia-laravel merges {@code flash} straight into the page object, never through
 * {@code PropsResolver}. That is also why this is a separate key from {@code "errors"}: the two
 * are unrelated buckets with different resolution paths on the client.
 */
@RequestScoped
public class InertiaFlash {

    /** Package-visible so {@link InertiaSharedProps} reads back the same session key. */
    static final String SESSION_KEY = "inertia_flash";

    @Inject
    RoutingContext routingContext;

    /**
     * Queues a single flash value under {@code key}, delivered to the client as
     * {@code page.flash[key]} on the next response.
     */
    public void flash(String key, Object value) {
        flash(Map.of(key, value));
    }

    /**
     * Queues several flash values at once, merging with anything already flashed this request
     * (e.g. errors flashed earlier in the same handler are not clobbered).
     */
    public void flash(Map<String, Object> data) {
        Session session = routingContext.session();
        if (session == null) {
            return;
        }
        JsonObject existing = session.get(SESSION_KEY);
        Map<String, Object> merged = existing != null
            ? new LinkedHashMap<>(existing.getMap())
            : new LinkedHashMap<>();
        merged.putAll(data);
        // JsonObject, not a raw Map: RedisSessionStore only knows how to serialize that type (see
        // InertiaValidationExceptionMapper.toResponse() for the same care taken with "errors").
        session.put(SESSION_KEY, new JsonObject(merged));
    }
}
