package io.github.inertia4j.quarkus;

import io.quarkus.hibernate.validator.runtime.jaxrs.ResteasyReactiveViolationException;
import io.quarkus.hibernate.validator.runtime.jaxrs.ResteasyReactiveViolationExceptionMapper;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.Session;
import jakarta.inject.Inject;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.ElementKind;
import jakarta.validation.Path;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.Logger;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Global counterpart of what Laravel does automatically whenever a controller runs
 * {@code $request->validate(...)}: catch the validation exception before it reaches application
 * code, and turn it into a redirect back to the page the form was submitted from, with the
 * errors flashed to the session for that next page load to pick up (see
 * {@link InertiaSharedProps} and {@link InertiaSessionSetup}).
 * <p>
 * Because this is a JAX-RS {@code @Provider}, it applies to every Resource that validates its
 * input with {@code @Valid} — none of them need to know this mapper exists, matching how
 * {@code $request->validate()} works the same way in every Laravel controller without each one
 * writing its own error-handling.
 * <p>
 * This mapper deliberately targets {@link ConstraintViolationException} and not its supertype
 * {@code ValidationException}: Quarkus throws {@code ResteasyReactiveViolationException}, which
 * extends {@code ConstraintViolationException}, and RESTEasy Reactive resolves mappers by walking
 * up the thrown exception's class hierarchy. So this one is found before the built-in
 * {@code ResteasyReactiveViolationExceptionMapper} (registered for {@code ValidationException},
 * one level further up) — the override is deterministic, not a matter of registration order.
 */
@Provider
class InertiaValidationExceptionMapper implements ExceptionMapper<ConstraintViolationException> {

    private static final Logger LOG = Logger.getLogger(InertiaValidationExceptionMapper.class);

    /** Used when the {@code Referer} is missing or unusable — there is nowhere sane to go back to. */
    private static final String FALLBACK_LOCATION = "/";

    /**
     * Constraints answering "is there a value at all?" win over constraints about the shape of the
     * value, so an empty field reports a "required" message instead of a size/format complaint.
     * Bean Validation hands violations back in an unspecified (hash) order, so without an explicit
     * ordering the SAME input could produce different messages on different requests.
     */
    private static final List<String> PRESENCE_CONSTRAINTS = List.of("NotNull", "NotBlank", "NotEmpty");

    /** Stateless; kept as a field only to avoid re-instantiating it on every failed validation. */
    private final ResteasyReactiveViolationExceptionMapper quarkusMapper =
        new ResteasyReactiveViolationExceptionMapper();

    @Context
    HttpHeaders headers;

    @Inject
    RoutingContext routingContext;

    @Inject
    Inertia inertia;

    @Override
    public Response toResponse(ConstraintViolationException exception) {
        // A violation on the RETURN VALUE is not a user error, it's a server bug (the method
        // returned something that violates its own contract). Turning it into a redirect would hide
        // the failure and still expose an internal message as if it were a form error — the same
        // distinction Quarkus's own native mapper makes before answering 500.
        if (hasReturnValueViolation(exception)) {
            LOG.error("Constraint violation on the return value (server bug)", exception);
            return Response.serverError().build();
        }

        // Redirect-with-flash only makes sense to a client that speaks the Inertia protocol. An API
        // client (curl, a mobile app, another service) has no way to follow the 302 to a page and
        // read its "errors" prop: the correct response for it is whatever Quarkus would have
        // answered if this adapter didn't exist. The Inertia client marks every request with
        // "X-Inertia: true", so the header's absence is the signal that whoever called in isn't this
        // app's front end.
        if (!isInertiaRequest()) {
            return apiValidationResponse(exception);
        }

        Session session = routingContext.session();
        if (session != null) {
            // RedisSessionStore needs to serialize the value to bytes (the LocalSessionStore used in
            // dev doesn't, it keeps the Java object directly in memory) — it only accepts primitive
            // types, byte[]/Buffer, and JsonObject/JsonArray. A raw Map<String,String> breaks with an
            // IllegalStateException at runtime, silently from the client's point of view (the session
            // simply doesn't persist, with no error at all in the response).
            session.put("errors", new JsonObject(new LinkedHashMap<String, Object>(collectErrors(exception))));
        }

        return inertia.redirect(safeLocation(headers.getHeaderString("Referer")));
    }

    private boolean isInertiaRequest() {
        return "true".equalsIgnoreCase(headers.getHeaderString("X-Inertia"));
    }

    /**
     * Hands the exception to the very mapper Quarkus would have used if this adapter were not
     * installed, so API clients keep getting the standard {@code 400} + {@code ViolationReport}
     * payload (and the {@code validation-exception} header) instead of a bespoke error shape that
     * only this app knows how to produce.
     */
    private Response apiValidationResponse(ConstraintViolationException exception) {
        if (exception instanceof ResteasyReactiveViolationException violation) {
            return quarkusMapper.toResponse(violation);
        }
        // Didn't come from request validation (e.g. a ConstraintViolationException thrown by
        // application code, or by a Hibernate flush). Not a client error, it's a server failure —
        // and the native mapper doesn't handle it either, it just rethrows.
        LOG.error("ConstraintViolationException outside of request validation", exception);
        return Response.serverError().build();
    }

    private Map<String, String> collectErrors(ConstraintViolationException exception) {
        Map<String, String> errors = new LinkedHashMap<>();
        exception.getConstraintViolations().stream()
            .sorted(Comparator
                .comparing(InertiaValidationExceptionMapper::fieldName, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparingInt(InertiaValidationExceptionMapper::presenceRank)
                .thenComparing(ConstraintViolation::getMessage))
            .forEach(violation -> errors.putIfAbsent(fieldName(violation), violation.getMessage()));
        return errors;
    }

    /**
     * Turns the {@code Referer} into a location that is guaranteed to stay on this application.
     * <p>
     * The header is attacker-controllable (it is just an HTTP header; a cross-site form post sets
     * it too), so using it verbatim as {@code Location} would be an open redirect. Only its
     * path and query survive — the scheme/host are dropped rather than compared, which keeps the
     * check correct behind a TLS-terminating reverse proxy, where the request URI the app sees
     * ({@code http://internal:8080}) legitimately differs from the browser's origin.
     */
    static String safeLocation(String referer) {
        if (referer == null || referer.isBlank()) {
            return FALLBACK_LOCATION;
        }
        URI uri;
        try {
            uri = new URI(referer).normalize();
        } catch (URISyntaxException e) {
            return FALLBACK_LOCATION;
        }
        String path = uri.getRawPath();
        // Opaque URIs ("javascript:alert(1)") and relative referers don't have an absolute path:
        // there's no page in this app to go back to, so they fall through to the fallback.
        if (path == null || !path.startsWith("/")) {
            return FALLBACK_LOCATION;
        }
        // "//evil.example.com/x" has no scheme, but the browser resolves it as protocol-relative,
        // i.e. a third-party host. Collapsing the leading slashes here (rather than trusting
        // URI.normalize(), whose handling of duplicate slashes is unspecified) guarantees Location
        // always points at a path on this origin.
        path = path.replaceFirst("^/+", "/");
        if (path.startsWith("/..")) {
            return FALLBACK_LOCATION;
        }
        return uri.getRawQuery() == null ? path : path + "?" + uri.getRawQuery();
    }

    private static boolean hasReturnValueViolation(ConstraintViolationException exception) {
        return exception.getConstraintViolations().stream()
            .flatMap(violation -> streamNodes(violation.getPropertyPath()))
            .anyMatch(node -> node.getKind() == ElementKind.RETURN_VALUE);
    }

    private static Stream<Path.Node> streamNodes(Path path) {
        return StreamSupport.stream(path.spliterator(), false);
    }

    private static int presenceRank(ConstraintViolation<?> violation) {
        String annotation = violation.getConstraintDescriptor().getAnnotation().annotationType().getSimpleName();
        int index = PRESENCE_CONSTRAINTS.indexOf(annotation);
        return index < 0 ? PRESENCE_CONSTRAINTS.size() : index;
    }

    private static String fieldName(ConstraintViolation<?> violation) {
        String name = null;
        for (Path.Node node : violation.getPropertyPath()) {
            name = node.getName();
        }
        return name;
    }
}
