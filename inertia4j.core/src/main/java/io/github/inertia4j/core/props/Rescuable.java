package io.github.inertia4j.core.props;

/**
 * A prop whose resolution failure should not fail the whole response — the exception is caught,
 * the prop's key is reported under {@code rescuedProps} instead of a value, and every other prop
 * still resolves normally.
 * <p>
 * Meant for props resolved on a follow-up partial reload (deferred props today): by the time that
 * request runs, the client already has a working page on screen, so one failing widget shouldn't
 * take the whole reload down with it.
 *
 * @see <a href="https://inertiajs.com/deferred-props">Inertia deferred props</a>
 */
public interface Rescuable {
    /**
     * @return whether a {@link RuntimeException} thrown while resolving this prop should be
     *         swallowed (reported via {@code rescuedProps}) instead of propagated.
     */
    boolean shouldRescue();
}
