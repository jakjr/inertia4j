package io.github.inertia4j.core.props;

import java.util.List;

/**
 * A prop that the client should merge into its existing value instead of replacing outright on a
 * partial reload — e.g. paginated results that grow as the user scrolls.
 *
 * @see <a href="https://inertiajs.com/merging-props">Inertia merging props</a>
 */
public interface Mergeable {
    /** How the new value combines with what the client already has. */
    enum Strategy {
        /** New items are appended to the end of the client's existing array. */
        APPEND,
        /** New items are prepended to the start of the client's existing array. */
        PREPEND,
        /** The whole value is deep-merged into the client's existing value. */
        DEEP
    }

    /**
     * @return whether this prop should actually be merged. A type can implement {@code Mergeable}
     *         without every instance opting in (e.g. a {@code DeferProp} that never called
     *         {@code .merge()}) — {@link io.github.inertia4j.core.InertiaRenderer} only treats it
     *         as mergeable when this is {@code true}.
     */
    boolean shouldMerge();

    /**
     * @return the merge strategy for this prop, applied at the root of its value — only
     *         consulted when {@link #getAppendsAtPaths()} and {@link #getPrependsAtPaths()} are
     *         both empty (see {@link #mergesAtRoot()}).
     */
    Strategy getMergeStrategy();

    /**
     * Sub-paths, relative to this prop's own value, whose arrays should be appended to instead of
     * the prop's root. This is what lets a paginator-shaped prop merge only its item list
     * ({@code "data"}) while its sibling pagination fields are replaced wholesale — the shape
     * {@code ScrollProp} relies on. Mirrors {@code MergesProps::appendsAtPaths()}.
     *
     * @return relative append paths; empty when this prop merges at its own root.
     */
    default List<String> getAppendsAtPaths() {
        return List.of();
    }

    /**
     * Sub-paths, relative to this prop's own value, whose arrays should be prepended to instead
     * of the prop's root. Mirrors {@code MergesProps::prependsAtPaths()}.
     *
     * @return relative prepend paths; empty when this prop merges at its own root.
     */
    default List<String> getPrependsAtPaths() {
        return List.of();
    }

    /**
     * Mirrors {@code MergesProps::mergesAtRoot()}: naming any sub-path at all moves the merge off
     * this prop's root, and {@link #getMergeStrategy()} stops applying to the root.
     *
     * @return whether this prop merges at the root of its own value.
     */
    default boolean mergesAtRoot() {
        return getAppendsAtPaths().isEmpty() && getPrependsAtPaths().isEmpty();
    }

    /**
     * Paths, relative to this prop's own value, identifying the field used to match existing
     * items instead of appending/prepending duplicates (e.g. {@code "id"} for a top-level array
     * of objects, or {@code "data.id"} for an array nested one level deep). Empty when items
     * should just be matched by position.
     *
     * @return relative match-on paths.
     */
    List<String> getMatchOn();
}
