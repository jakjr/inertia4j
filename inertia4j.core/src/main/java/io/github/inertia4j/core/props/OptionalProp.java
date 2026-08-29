package io.github.inertia4j.core.props;

import java.util.function.Supplier;

/**
 * Wraps a prop value that is skipped on every full page visit and — unlike {@link DeferProp} —
 * never announced anywhere (no entry in {@code deferredProps}): the client only learns it exists,
 * and gets it resolved, by explicitly naming it in a partial reload's {@code only} list. The Java
 * equivalent of Laravel's {@code Inertia::optional()} / Rails' {@code InertiaRails.optional}.
 * <p>
 * Also once-able (see {@link Onceable}), matching {@code OptionalProp.php}
 * (implements {@code IgnoreFirstLoad, Onceable}) and Rails' {@code optional_prop.rb}
 * ({@code < IgnoreOnFirstLoadProp; prepend PropOnceable}) exactly. Deliberately does
 * <strong>not</strong> extend {@link AbstractProp} to pick this up, though: that shared base
 * bundles merge in with once, and neither reference adapter mixes a merge trait/module into its
 * optional-prop class. Giving this one a {@code .merge()} it doesn't have upstream would let it
 * announce a {@code mergeProps} instruction for a prop the client was never told exists — an
 * optional prop nobody asked for is invisible, full stop, so there is nothing to merge into. This
 * duplicates a small slice of {@link PropState}'s once-facet rather than widening
 * {@link AbstractProp} to cover it.
 *
 * <pre>{@code
 * Map.of("relatorioCompleto", new OptionalProp(() -> gerarRelatorioCaro()))
 * }</pre>
 *
 * @see <a href="https://inertiajs.com/partial-reloads">Inertia partial reloads</a>
 */
public final class OptionalProp implements ResolvableProp, Onceable {

    private final Supplier<Object> callback;
    private final PropState state;

    /**
     * Wraps {@code callback}, resolved only when explicitly requested via partial reload.
     *
     * @param callback supplies the value once the client requests it.
     */
    public OptionalProp(Supplier<Object> callback) {
        this(callback, PropState.DEFAULT);
    }

    private OptionalProp(Supplier<Object> callback, PropState state) {
        this.callback = callback;
        this.state = state;
    }

    @Override
    public Object resolve() {
        return callback.get();
    }

    @Override
    public boolean shouldResolveOnce() {
        return state.once;
    }

    @Override
    public String getKey() {
        return state.onceKey;
    }

    @Override
    public boolean shouldBeRefreshed() {
        return state.refresh;
    }

    @Override
    public Long getExpiresAtMillis() {
        return state.onceTtlMillis == null ? null : System.currentTimeMillis() + state.onceTtlMillis;
    }

    /**
     * @return this prop, resolved once and cached client-side under its own path.
     * @see <a href="https://inertiajs.com/once-props">Inertia once props</a>
     */
    public OptionalProp once() {
        return new OptionalProp(callback, state.withOnce());
    }

    /**
     * @param key custom cache key, instead of defaulting to this prop's own path.
     * @return this prop, cached under {@code key}.
     */
    public OptionalProp as(String key) {
        return new OptionalProp(callback, state.withOnceKey(key));
    }

    /**
     * @param ttlMillis time-to-live in milliseconds, counted from when this response renders.
     * @return this prop, expiring client-side after {@code ttlMillis}.
     */
    public OptionalProp until(long ttlMillis) {
        return new OptionalProp(callback, state.withOnceTtlMillis(ttlMillis));
    }

    /**
     * @return this prop, forced to always resolve and send, ignoring any cached copy the client
     *         claims to already have.
     */
    public OptionalProp fresh() {
        return new OptionalProp(callback, state.withRefresh());
    }
}
