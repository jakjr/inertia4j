package io.github.inertia4j.core.props;

import java.util.List;

/**
 * The merge/once facets shared by every {@link AbstractProp} leaf — split out of the leaf classes
 * themselves so {@link AbstractProp}'s fluent methods can rebuild an immutable copy-on-write
 * instance without each leaf having to know about every other leaf's fields.
 */
final class PropState {
    static final PropState DEFAULT =
        new PropState(false, Mergeable.Strategy.APPEND, List.of(), false, null, false, null);

    final boolean merge;
    final Mergeable.Strategy mergeStrategy;
    final List<String> matchOn;
    final boolean once;
    final String onceKey;
    final boolean refresh;
    final Long onceTtlMillis;

    private PropState(
        boolean merge,
        Mergeable.Strategy mergeStrategy,
        List<String> matchOn,
        boolean once,
        String onceKey,
        boolean refresh,
        Long onceTtlMillis
    ) {
        this.merge = merge;
        this.mergeStrategy = mergeStrategy;
        this.matchOn = matchOn;
        this.once = once;
        this.onceKey = onceKey;
        this.refresh = refresh;
        this.onceTtlMillis = onceTtlMillis;
    }

    PropState withMerge(Mergeable.Strategy strategy) {
        return new PropState(true, strategy, matchOn, once, onceKey, refresh, onceTtlMillis);
    }

    PropState withMatchOn(List<String> paths) {
        return new PropState(merge, mergeStrategy, paths, once, onceKey, refresh, onceTtlMillis);
    }

    PropState withOnce() {
        return new PropState(merge, mergeStrategy, matchOn, true, onceKey, refresh, onceTtlMillis);
    }

    PropState withOnceKey(String key) {
        return new PropState(merge, mergeStrategy, matchOn, once, key, refresh, onceTtlMillis);
    }

    PropState withRefresh() {
        return new PropState(merge, mergeStrategy, matchOn, once, onceKey, true, onceTtlMillis);
    }

    PropState withOnceTtlMillis(long ttlMillis) {
        return new PropState(merge, mergeStrategy, matchOn, once, onceKey, refresh, ttlMillis);
    }
}
