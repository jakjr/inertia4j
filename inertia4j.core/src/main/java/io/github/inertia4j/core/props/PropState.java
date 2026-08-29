package io.github.inertia4j.core.props;

import java.util.ArrayList;
import java.util.List;

/**
 * The once facet shared by every {@link AbstractOnceProp} leaf, plus the merge facet on top for
 * the {@link AbstractProp} leaves that also compose it — split out of the leaf classes themselves
 * so their fluent methods can rebuild an immutable copy-on-write instance without each leaf having
 * to know about every other leaf's fields. A leaf that only extends {@link AbstractOnceProp}
 * (e.g. {@link OnceProp}, {@link OptionalProp}) simply never touches this class's merge-related
 * fields, which stay at their {@link #DEFAULT} values.
 */
final class PropState {
    static final PropState DEFAULT = new PropState(
        false, Mergeable.Strategy.APPEND, List.of(), List.of(), List.of(), false, null, false, null
    );

    final boolean merge;
    final Mergeable.Strategy mergeStrategy;
    final List<String> matchOn;
    final List<String> appendsAtPaths;
    final List<String> prependsAtPaths;
    final boolean once;
    final String onceKey;
    final boolean refresh;
    final Long onceTtlMillis;

    private PropState(
        boolean merge,
        Mergeable.Strategy mergeStrategy,
        List<String> matchOn,
        List<String> appendsAtPaths,
        List<String> prependsAtPaths,
        boolean once,
        String onceKey,
        boolean refresh,
        Long onceTtlMillis
    ) {
        this.merge = merge;
        this.mergeStrategy = mergeStrategy;
        this.matchOn = matchOn;
        this.appendsAtPaths = appendsAtPaths;
        this.prependsAtPaths = prependsAtPaths;
        this.once = once;
        this.onceKey = onceKey;
        this.refresh = refresh;
        this.onceTtlMillis = onceTtlMillis;
    }

    PropState withMerge(Mergeable.Strategy strategy) {
        return new PropState(
            true, strategy, matchOn, appendsAtPaths, prependsAtPaths, once, onceKey, refresh, onceTtlMillis
        );
    }

    PropState withMatchOn(List<String> paths) {
        return new PropState(
            merge, mergeStrategy, paths, appendsAtPaths, prependsAtPaths, once, onceKey, refresh, onceTtlMillis
        );
    }

    /**
     * Moves the merge off the prop's root and onto {@code paths} inside it, appending. Adds to
     * whatever paths were already named (mirroring {@code MergesProps::append()}, which pushes
     * onto its list) so several sub-paths of the same prop can merge independently.
     */
    PropState withAppendsAtPaths(List<String> paths) {
        return new PropState(
            merge, mergeStrategy, matchOn, concat(appendsAtPaths, paths), prependsAtPaths, once, onceKey, refresh, onceTtlMillis
        );
    }

    /** The prepend counterpart of {@link #withAppendsAtPaths}. */
    PropState withPrependsAtPaths(List<String> paths) {
        return new PropState(
            merge, mergeStrategy, matchOn, appendsAtPaths, concat(prependsAtPaths, paths), once, onceKey, refresh, onceTtlMillis
        );
    }

    /**
     * Points the merge at exactly one sub-path, in exactly one direction, discarding any
     * previously named sub-paths. {@code ScrollProp} needs replace-not-add semantics because its
     * direction comes from a per-request header: re-deriving it must never leave the previous
     * request's direction (or a duplicate of the same wrapper path) behind.
     */
    PropState withSoleMergePath(String path, boolean prepend) {
        return new PropState(
            merge,
            mergeStrategy,
            matchOn,
            prepend ? List.of() : List.of(path),
            prepend ? List.of(path) : List.of(),
            once,
            onceKey,
            refresh,
            onceTtlMillis
        );
    }

    PropState withOnce() {
        return new PropState(
            merge, mergeStrategy, matchOn, appendsAtPaths, prependsAtPaths, true, onceKey, refresh, onceTtlMillis
        );
    }

    PropState withOnceKey(String key) {
        return new PropState(
            merge, mergeStrategy, matchOn, appendsAtPaths, prependsAtPaths, once, key, refresh, onceTtlMillis
        );
    }

    PropState withRefresh() {
        return new PropState(
            merge, mergeStrategy, matchOn, appendsAtPaths, prependsAtPaths, once, onceKey, true, onceTtlMillis
        );
    }

    PropState withOnceTtlMillis(long ttlMillis) {
        return new PropState(
            merge, mergeStrategy, matchOn, appendsAtPaths, prependsAtPaths, once, onceKey, refresh, ttlMillis
        );
    }

    private static List<String> concat(List<String> existing, List<String> added) {
        List<String> combined = new ArrayList<>(existing.size() + added.size());
        combined.addAll(existing);
        combined.addAll(added);
        return List.copyOf(combined);
    }
}
