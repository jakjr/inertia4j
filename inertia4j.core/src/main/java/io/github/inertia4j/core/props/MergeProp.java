package io.github.inertia4j.core.props;

import java.util.Arrays;
import java.util.List;

/**
 * Wraps a prop value that the client should merge into its existing value on a partial reload,
 * instead of replacing it — the Java equivalent of Laravel's {@code Inertia::merge()} / Rails'
 * {@code InertiaRails.merge}. Appends by default.
 *
 * <pre>{@code
 * // Novos posts sao acrescentados aos que o cliente ja tem, casando por "id" pra
 * // atualizar (nao duplicar) posts que ja existiam.
 * Map.of("posts", new MergeProp(buscarProximaPagina()).matchOn("id"))
 * }</pre>
 *
 * @see <a href="https://inertiajs.com/merging-props">Inertia merging props</a>
 */
public class MergeProp implements ResolvableProp, Mergeable {

    private final Object value;
    private final Strategy strategy;
    private final List<String> matchOn;

    /**
     * Wraps {@code value} to be appended into the client's existing value for this prop.
     *
     * @param value the (already computed) value for this response.
     */
    public MergeProp(Object value) {
        this(value, Strategy.APPEND, List.of());
    }

    private MergeProp(Object value, Strategy strategy, List<String> matchOn) {
        this.value = value;
        this.strategy = strategy;
        this.matchOn = matchOn;
    }

    @Override
    public Object resolve() {
        return value;
    }

    @Override
    public Strategy getMergeStrategy() {
        return strategy;
    }

    @Override
    public List<String> getMatchOn() {
        return matchOn;
    }

    /**
     * @return an equivalent prop merged by prepending instead of appending.
     */
    public MergeProp prepend() {
        return new MergeProp(value, Strategy.PREPEND, matchOn);
    }

    /**
     * @return an equivalent prop merged by deep-merging its whole structure instead of
     *         appending/prepending array items.
     */
    public MergeProp deepMerge() {
        return new MergeProp(value, Strategy.DEEP, matchOn);
    }

    /**
     * Matches existing items on {@code paths} instead of appending/prepending duplicates.
     *
     * @param paths one or more paths relative to this prop's own value (e.g. {@code "id"}, or
     *              {@code "data.id"} for an array nested one level deep).
     * @return an equivalent prop that matches items on {@code paths}.
     */
    public MergeProp matchOn(String... paths) {
        return new MergeProp(value, strategy, Arrays.asList(paths));
    }
}
