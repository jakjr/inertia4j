package io.github.inertia4j.core.props;

import java.util.function.Supplier;

/**
 * Wraps a prop value that is always resolved and included, even during a partial reload whose
 * {@code only}/{@code except} filter would otherwise exclude it — the Java equivalent of
 * Laravel's {@code Inertia::always()} / Rails' {@code InertiaRails.always}.
 * <p>
 * Deliberately a bare leaf, not an {@link AbstractProp}: neither {@code AlwaysProp.php} nor
 * Rails' {@code always_prop.rb} composes it with merge/once/defer (both reference adapters
 * special-case it with a direct class check — {@code $prop instanceof AlwaysProp} /
 * {@code prop.is_a?(AlwaysProp)} — not an interface), so there is no facet to inherit here.
 * {@link io.github.inertia4j.core.InertiaRenderer} mirrors that same direct check.
 *
 * <pre>{@code
 * Map.of("flash", new AlwaysProp(() -> sessao.pegarFlash()))
 * }</pre>
 *
 * @see <a href="https://inertiajs.com/partial-reloads">Inertia partial reloads</a>
 */
public final class AlwaysProp implements ResolvableProp {

    private final Supplier<Object> callback;

    /**
     * @param callback supplies the value, resolved on every response regardless of any partial
     *                 reload {@code only}/{@code except} filter.
     */
    public AlwaysProp(Supplier<Object> callback) {
        this.callback = callback;
    }

    @Override
    public Object resolve() {
        return callback.get();
    }
}
