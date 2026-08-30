package io.github.inertia4j.quarkus;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.Session;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;

import java.util.Map;

/**
 * Holds the two <em>session-backed, pull-once</em> buckets attached to every Inertia response for
 * the current request: validation {@code errors} (flashed by
 * {@link InertiaValidationExceptionMapper}) and one-shot {@code flash} data (flashed by
 * {@link InertiaFlash}). Both live in the HTTP session (see {@link InertiaSessionSetup}), read and
 * removed here exactly once per request — matching "shown once" semantics without needing a
 * response-side cleanup step.
 * <p>
 * <b>This is not the port of Laravel's generic {@code Inertia::share()}/Rails'
 * {@code inertia_share}</b> — that mechanism (recomputed fresh every request, never session-backed)
 * is {@link InertiaShared}, typically registered by default from a {@code ContainerRequestFilter}
 * your app provides. Adding app-wide shared data here would inherit the wrong lifecycle: it would
 * render on the first page load, then silently vanish on every subsequent navigation, because
 * {@code session.remove(...)} below empties this bucket after a single read. {@code errors} and
 * {@code flash} are kept as two separate session keys/fields (not merged into one) because they
 * also resolve differently downstream: {@code errors} is folded into {@code props} by
 * {@link Inertia#render}, while {@code flash} is attached outside {@code props} — mirroring how
 * inertia-laravel resolves them.
 */
@RequestScoped
class InertiaSharedProps {

    @Inject
    RoutingContext routingContext;

    private Map<String, Object> errors = Map.of();
    private Map<String, Object> flash = Map.of();

    @PostConstruct
    void loadFlash() {
        Session session = routingContext.session();
        if (session == null) {
            return;
        }
        // Stored as a JsonObject (see InertiaValidationExceptionMapper/InertiaFlash) — RedisSessionStore
        // only knows how to serialize that type, not a raw Map.
        JsonObject flashedErrors = session.remove("errors");
        if (flashedErrors != null) {
            errors = flashedErrors.getMap();
        }
        JsonObject flashedData = session.remove(InertiaFlash.SESSION_KEY);
        if (flashedData != null) {
            flash = flashedData.getMap();
        }
    }

    Map<String, Object> getErrors() {
        return errors;
    }

    Map<String, Object> getFlash() {
        return flash;
    }
}
