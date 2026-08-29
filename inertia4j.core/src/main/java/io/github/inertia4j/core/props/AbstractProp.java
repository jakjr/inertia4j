package io.github.inertia4j.core.props;

import java.util.List;
import java.util.function.Supplier;

/**
 * Shared, composable facets (merge and once) behind {@link DeferProp}, {@link MergeProp}, and
 * {@link OnceProp} — the Java equivalent of the {@code MergesProps}/{@code ResolvesOnce} traits
 * that inertia-laravel mixes into its own prop classes. Java has no traits/multiple inheritance,
 * so this base class plays that role instead: every fluent facet method
 * ({@link #merge()}/{@link #once()}/...) lives here once, and each leaf just turns on its own
 * "primary" facet in its constructor and returns its own type from every fluent call (see
 * {@link #withState}) — instead of a wrapper class per combination (e.g. a would-be
 * {@code MergeableDeferProp}) that a fixed single-inheritance chain would otherwise force.
 * <p>
 * {@code Rescuable} is deliberately NOT here: in inertia-laravel only {@code DeferProp}
 * implements it (merge/once genuinely apply to any prop type, resolution failure is specifically
 * a deferred-loading concern), so it stays a facet of {@link DeferProp} alone.
 *
 * @param <T> the concrete leaf type, so fluent methods keep returning that type instead of
 *            {@code AbstractProp}.
 */
abstract class AbstractProp<T extends AbstractProp<T>> implements ResolvableProp, Mergeable, Onceable {

    protected final Supplier<Object> callback;
    protected final PropState state;

    AbstractProp(Supplier<Object> callback, PropState state) {
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
    public boolean shouldMerge() {
        return state.merge;
    }

    @Override
    public Strategy getMergeStrategy() {
        return state.mergeStrategy;
    }

    @Override
    public List<String> getMatchOn() {
        return state.matchOn;
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
     * @return this prop, also merged by appending once resolved.
     * @see <a href="https://inertiajs.com/merging-props">Inertia merging props</a>
     */
    public T merge() {
        return withState(state.withMerge(Strategy.APPEND));
    }

    /**
     * @return this prop, also merged by prepending once resolved.
     */
    public T prepend() {
        return withState(state.withMerge(Strategy.PREPEND));
    }

    /**
     * @return this prop, also deep-merged once resolved.
     */
    public T deepMerge() {
        return withState(state.withMerge(Strategy.DEEP));
    }

    /**
     * Matches existing items on {@code paths} instead of appending/prepending duplicates.
     *
     * @param paths one or more paths relative to this prop's own value.
     * @return an equivalent prop that matches items on {@code paths}.
     */
    public T matchOn(String... paths) {
        return withState(state.withMatchOn(List.of(paths)));
    }

    /**
     * Marks this prop to be resolved only once and cached client-side.
     *
     * @return this prop, resolved once.
     * @see <a href="https://inertiajs.com/once-props">Inertia once props</a>
     */
    public T once() {
        return withState(state.withOnce());
    }

    /**
     * Sets a custom cache key for this once prop, instead of defaulting to its own path.
     *
     * @param key the cache key.
     * @return this prop, cached under {@code key}.
     */
    public T as(String key) {
        return withState(state.withOnceKey(key));
    }

    /**
     * Sets how long the client should keep its cached copy before it must be re-resolved.
     *
     * @param ttlMillis time-to-live in milliseconds, counted from when this response renders.
     * @return this prop, expiring after {@code ttlMillis}.
     */
    public T until(long ttlMillis) {
        return withState(state.withOnceTtlMillis(ttlMillis));
    }

    /**
     * Forces this once prop to always resolve and send, ignoring any cached copy the client
     * claims to already have.
     *
     * @return this prop, always refreshed.
     */
    public T fresh() {
        return withState(state.withRefresh());
    }
}
