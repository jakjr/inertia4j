package io.github.inertia4j.quarkus;

import io.vertx.core.Vertx;
import io.vertx.core.http.CookieSameSite;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.SessionHandler;
import io.vertx.ext.web.sstore.redis.RedisSessionStore;
import io.vertx.redis.client.Redis;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Registers an HTTP session (Redis-backed) on the Vert.x router, the equivalent of what Laravel's
 * session middleware sets up for every request. This is what lets
 * {@link InertiaValidationExceptionMapper} and {@link InertiaSharedProps} flash validation errors
 * across a redirect using an opaque session-id cookie, instead of putting the errors themselves in
 * the cookie (see those two classes' javadoc for why that distinction matters).
 * <p>
 * Redis, not the in-memory {@code LocalSessionStore}: this app is meant to run as more than one
 * replica in Kubernetes/OpenShift, and an in-memory store doesn't survive that — a session written
 * on the pod that handled the failed POST is invisible to whichever pod the Service happens to
 * route the follow-up GET to, so the flash would silently vanish depending on which pod answers.
 * Backing the store with Redis (shared by every replica) is the direct equivalent of Laravel's own
 * fix for the same problem, {@code SESSION_DRIVER=redis}.
 * <p>
 * {@code quarkus.redis.hosts} is supplied automatically by Quarkus Dev Services in
 * {@code quarkus:dev}/tests (spins up a throwaway Redis container, no local setup needed). In a
 * real deployment it must be set to a Redis reachable from the cluster — e.g. via
 * {@code %prod.quarkus.redis.hosts} or an environment variable, pointing at a Service/ElastiCache/
 * whatever Redis the cluster provides.
 */
@ApplicationScoped
class InertiaSessionSetup {

    void start(
        @Observes Router router,
        Vertx vertx,
        @ConfigProperty(name = "quarkus.redis.hosts", defaultValue = "redis://localhost:6379") String redisHosts,
        @ConfigProperty(name = "inertia.session.cookie-secure", defaultValue = "false") boolean cookieSecure
    ) {
        Redis redisClient = Redis.createClient(vertx, redisHosts);
        var sessionStore = RedisSessionStore.create(vertx, redisClient);
        // The cookie carries the session ID (the only thing that grants access to the flash data
        // stored in Redis), so the three flags actually matter here — without them an XSS on the app
        // turns into session theft, not just reading a disposable error string. cookieSecure is only
        // safe to turn on behind real HTTPS (the "false" default exists so it doesn't break
        // quarkus:dev over plain HTTP) — turn it on via inertia.session.cookie-secure=true in
        // production (typically under a %prod.inertia.session.cookie-secure=true).
        router.route().handler(SessionHandler.create(sessionStore)
            .setCookieHttpOnlyFlag(true)
            .setCookieSameSite(CookieSameSite.LAX)
            .setCookieSecureFlag(cookieSecure));
    }
}
