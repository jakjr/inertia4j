package io.github.inertia4j.core.props;

import java.util.function.Supplier;

/**
 * Wraps a prop value that is skipped on every full page visit and — unlike {@link DeferProp} —
 * never announced anywhere (no entry in {@code deferredProps}): the client only learns it exists,
 * and gets it resolved, by explicitly naming it in a partial reload's {@code only} list. The Java
 * equivalent of Laravel's {@code Inertia::optional()} / Rails' {@code InertiaRails.optional}.
 * <p>
 * Extends {@link AbstractOnceProp} rather than {@link AbstractProp}, matching
 * {@code OptionalProp.php} ({@code implements IgnoreFirstLoad, Onceable}, no {@code MergesProps})
 * and Rails' {@code optional_prop.rb} ({@code < IgnoreOnFirstLoadProp; prepend PropOnceable}, no
 * {@code PropMergeable}) exactly: neither reference adapter mixes a merge trait/module into its
 * optional-prop class. Giving this one a {@code .merge()} it doesn't have upstream would let a
 * partial reload that explicitly requests it also announce a {@code mergeProps} instruction for a
 * key the client has otherwise never been told exists on any other visit.
 *
 * <pre>{@code
 * Map.of("relatorioCompleto", new OptionalProp(() -> gerarRelatorioCaro()))
 * }</pre>
 *
 * @see <a href="https://inertiajs.com/partial-reloads">Inertia partial reloads</a>
 */
public final class OptionalProp extends AbstractOnceProp<OptionalProp> {

    /**
     * Wraps {@code callback}, resolved only when explicitly requested via partial reload.
     *
     * @param callback supplies the value once the client requests it.
     */
    public OptionalProp(Supplier<Object> callback) {
        this(callback, PropState.DEFAULT);
    }

    private OptionalProp(Supplier<Object> callback, PropState state) {
        super(callback, state);
    }

    @Override
    OptionalProp withState(PropState newState) {
        return new OptionalProp(callback, newState);
    }
}
