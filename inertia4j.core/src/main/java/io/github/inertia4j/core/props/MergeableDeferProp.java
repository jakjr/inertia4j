package io.github.inertia4j.core.props;

import java.util.Arrays;
import java.util.List;

/**
 * A {@link DeferProp} that also merges into the client's existing value once resolved, instead of
 * replacing it — e.g. a paginated list that both loads lazily <em>and</em> grows as the user
 * scrolls. Created via {@link DeferProp#merge()}/{@link DeferProp#prepend()}/
 * {@link DeferProp#deepMerge()}, never directly.
 *
 * @see <a href="https://inertiajs.com/merging-props#combining-with-deferred-props">Inertia — combining merge with deferred props</a>
 */
public class MergeableDeferProp implements ResolvableProp, Deferrable, Mergeable, Rescuable {

    private final DeferProp inner;
    private final Strategy strategy;
    private final List<String> matchOn;

    MergeableDeferProp(DeferProp inner, Strategy strategy, List<String> matchOn) {
        this.inner = inner;
        this.strategy = strategy;
        this.matchOn = matchOn;
    }

    @Override
    public Object resolve() {
        return inner.resolve();
    }

    @Override
    public String getGroup() {
        return inner.getGroup();
    }

    @Override
    public Strategy getMergeStrategy() {
        return strategy;
    }

    @Override
    public List<String> getMatchOn() {
        return matchOn;
    }

    @Override
    public boolean shouldRescue() {
        return inner.shouldRescue();
    }

    /**
     * Matches existing items on {@code paths} instead of appending/prepending duplicates.
     *
     * @param paths one or more paths relative to this prop's own value.
     * @return an equivalent prop that matches items on {@code paths}.
     */
    public MergeableDeferProp matchOn(String... paths) {
        return new MergeableDeferProp(inner, strategy, Arrays.asList(paths));
    }

    /**
     * @return an equivalent prop, rescued on resolution failure.
     * @see DeferProp#rescue()
     */
    public MergeableDeferProp rescue() {
        return new MergeableDeferProp(inner.rescue(), strategy, matchOn);
    }
}
