package io.github.inertia4j.core.props;

/**
 * A prop that should be skipped on a full page visit (and merely announced under
 * {@code deferredProps}, grouped by {@link #getGroup()}), then resolved on the follow-up partial
 * reload the client automatically issues for that group.
 *
 * @see <a href="https://inertiajs.com/deferred-props">Inertia deferred props</a>
 */
public interface Deferrable {
    /**
     * @return whether this prop should actually be deferred.
     */
    boolean shouldDefer();

    /**
     * The request group this prop is fetched together with. Props deferred with the same group
     * name are resolved in a single follow-up partial reload; different groups get separate
     * (parallel) reloads.
     *
     * @return the group name.
     */
    String getGroup();
}
