package io.github.inertia4j.core.props;

/**
 * A prop whose value is computed lazily, only when
 * {@link io.github.inertia4j.core.InertiaRenderer} decides it actually needs to be included in a
 * response — as opposed to a plain value, which is always included as-is.
 * <p>
 * This is the shared building block behind every prop wrapper in this package ({@link DeferProp}
 * today; merge/once/always/optional props follow the same shape) — mirrors how the Laravel and
 * Rails adapters compose small traits/modules around a lazily-resolved value instead of one
 * monolithic class per combination of behaviors.
 *
 * @see <a href="https://inertiajs.com/the-protocol#the-page-object">Inertia Page Object spec</a>
 */
public interface ResolvableProp {
    /**
     * Computes the value to send to the client.
     *
     * @return the resolved prop value.
     */
    Object resolve();
}
