package io.github.inertia4j.spi;

import org.jspecify.annotations.NullMarked;

import java.util.List;

/**
 * The four merge-related metadata fields of a {@link PageObject}, bundled together because they
 * are always computed in the same pass (over the same props) and always travel together — kept
 * as one constructor parameter on {@link PageObject} instead of four, to stop that constructor
 * from growing indefinitely as more protocol metadata is added.
 *
 * @see <a href="https://inertiajs.com/merging-props">Inertia merging props</a>
 */
@NullMarked
public final class MergeInstructions {
    private final List<String> mergeProps;
    private final List<String> prependProps;
    private final List<String> deepMergeProps;
    private final List<String> matchPropsOn;

    /**
     * Constructs a new set of merge instructions.
     *
     * @param mergeProps keys of props to be appended to their existing client-side value.
     * @param prependProps keys of props to be prepended to their existing client-side value.
     * @param deepMergeProps keys of props whose entire structure should be deep-merged into the
     *                       existing client-side value.
     * @param matchPropsOn {@code "<prop>.<path>"} entries identifying the field used to match
     *                     existing items instead of appending/prepending duplicates.
     */
    public MergeInstructions(
        List<String> mergeProps,
        List<String> prependProps,
        List<String> deepMergeProps,
        List<String> matchPropsOn
    ) {
        this.mergeProps = mergeProps;
        this.prependProps = prependProps;
        this.deepMergeProps = deepMergeProps;
        this.matchPropsOn = matchPropsOn;
    }

    /**
     * @return an empty instance — the common case of a page with no mergeable props at all.
     */
    public static MergeInstructions none() {
        return new MergeInstructions(List.of(), List.of(), List.of(), List.of());
    }

    /**
     * @return keys of props to be appended to their existing client-side value.
     */
    public List<String> getMergeProps() {
        return mergeProps;
    }

    /**
     * @return keys of props to be prepended to their existing client-side value.
     */
    public List<String> getPrependProps() {
        return prependProps;
    }

    /**
     * @return keys of props whose entire structure should be deep-merged into the existing
     *         client-side value.
     */
    public List<String> getDeepMergeProps() {
        return deepMergeProps;
    }

    /**
     * @return {@code "<prop>.<path>"} entries identifying the field used to match existing items
     *         instead of appending/prepending duplicates.
     */
    public List<String> getMatchPropsOn() {
        return matchPropsOn;
    }
}
