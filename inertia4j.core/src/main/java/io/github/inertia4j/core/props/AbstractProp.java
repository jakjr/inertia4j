package io.github.inertia4j.core.props;

import java.util.List;
import java.util.function.Supplier;

/**
 * Adds the merge facet on top of {@link AbstractOnceProp}'s once facet — together, the Java
 * equivalent of the {@code MergesProps}/{@code ResolvesOnce} traits inertia-laravel mixes into
 * {@code DeferProp}/{@code MergeProp}/{@code ScrollProp} (all three compose both upstream). Java
 * has no traits/multiple inheritance, so this two-level base class chain plays that role instead:
 * every fluent facet method lives here or in {@link AbstractOnceProp} once, and each leaf just
 * turns on its own "primary" facet in its constructor and returns its own type from every fluent
 * call (see {@link #withState}) — instead of a wrapper class per combination (e.g. a would-be
 * {@code MergeableDeferProp}) that a fixed single-inheritance chain would otherwise force.
 * <p>
 * {@link OnceProp} and {@link OptionalProp} extend {@link AbstractOnceProp} directly instead of
 * this class, since neither composes with merge upstream ({@code OnceProp.php} is
 * {@code implements Onceable} alone; {@code OptionalProp.php} is
 * {@code implements IgnoreFirstLoad, Onceable}) — extending this one would hand them
 * {@code .merge()}/{@code .prepend()}/etc. they have no business exposing.
 * <p>
 * {@code Rescuable} is deliberately NOT here either: in inertia-laravel only {@code DeferProp}
 * implements it (merge/once genuinely apply to any prop type composed with them, resolution
 * failure is specifically a deferred-loading concern), so it stays a facet of {@link DeferProp}
 * alone.
 *
 * @param <T> the concrete leaf type, so fluent methods keep returning that type instead of
 *            {@code AbstractProp}.
 */
abstract class AbstractProp<T extends AbstractProp<T>> extends AbstractOnceProp<T> implements Mergeable {

    AbstractProp(Supplier<Object> callback, PropState state) {
        super(callback, state);
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
    public List<String> getAppendsAtPaths() {
        return state.appendsAtPaths;
    }

    @Override
    public List<String> getPrependsAtPaths() {
        return state.prependsAtPaths;
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
     * Merges by appending into arrays found at {@code paths} <em>inside</em> this prop's value,
     * instead of at its root — for a paginator-shaped value like
     * {@code {"data": [...], "nextPage": 3}}, {@code append("data")} grows the item list while
     * the pagination fields around it are replaced wholesale.
     *
     * <p>
     * Named {@code appendAt} rather than overloading {@link #merge()}/{@link #prepend()} the way
     * PHP's {@code append(bool|string $path)} does — Java can't dispatch a no-arg call and a
     * string call onto one varargs method without the two reading ambiguously at the call site.
     *
     * @param paths one or more paths relative to this prop's own value.
     * @return this prop, also merged by appending at {@code paths}.
     */
    public T appendAt(String... paths) {
        return withState(state.withMerge(Strategy.APPEND).withAppendsAtPaths(List.of(paths)));
    }

    /**
     * The prepend counterpart of {@link #appendAt(String...)}.
     *
     * @param paths one or more paths relative to this prop's own value.
     * @return this prop, also merged by prepending at {@code paths}.
     */
    public T prependAt(String... paths) {
        return withState(state.withMerge(Strategy.PREPEND).withPrependsAtPaths(List.of(paths)));
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
}
