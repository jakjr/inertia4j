package io.github.inertia4j.core.props;

import java.util.function.Supplier;

/**
 * The once facet shared by every leaf that composes it — split out of {@link AbstractProp} so a
 * leaf that is once-able but deliberately <strong>not</strong> merge-able (today: only
 * {@link OptionalProp}) doesn't have to inherit {@code .merge()}/{@code .prepend()}/etc. it has
 * no business exposing. {@link AbstractProp} itself extends this and adds the merge facet on top
 * for the leaves that upstream actually composes with merge ({@code DeferProp}/{@code MergeProp}/
 * {@code ScrollProp}); {@link OnceProp} extends this directly, matching {@code OnceProp.php}
 * (`implements Onceable` — nothing else) and Rails' {@code once_prop.rb} exactly.
 *
 * @param <T> the concrete leaf type, so {@link #once()}/{@link #as}/{@link #until}/{@link #fresh}
 *            keep returning that type instead of {@code AbstractOnceProp}.
 */
abstract class AbstractOnceProp<T extends AbstractOnceProp<T>> implements ResolvableProp, Onceable {

    protected final Supplier<Object> callback;
    protected final PropState state;

    AbstractOnceProp(Supplier<Object> callback, PropState state) {
        this.callback = callback;
        this.state = state;
    }

    /** Rebuilds this prop, of its own concrete type, with {@code newState}. */
    abstract T withState(PropState newState);

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
    public T once() {
        return withState(state.withOnce());
    }

    /**
     * @param key custom cache key, instead of defaulting to this prop's own path.
     * @return this prop, cached under {@code key}.
     */
    public T as(String key) {
        return withState(state.withOnceKey(key));
    }

    /**
     * @param ttlMillis time-to-live in milliseconds, counted from when this response renders.
     * @return this prop, expiring client-side after {@code ttlMillis}.
     */
    public T until(long ttlMillis) {
        return withState(state.withOnceTtlMillis(ttlMillis));
    }

    /**
     * @return this prop, forced to always resolve and send, ignoring any cached copy the client
     *         claims to already have.
     */
    public T fresh() {
        return withState(state.withRefresh());
    }
}
