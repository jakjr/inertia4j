package io.github.inertia4j.core.props;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Wraps one page of a paginated value that the client's infinite-scroll component grows as the
 * user scrolls — the Java equivalent of Laravel's {@code Inertia::scroll()}.
 * <p>
 * It is a merge prop with two extras. First, it merges at a <em>sub-path</em> of its value (the
 * {@code wrapper}, {@code "data"} by default) instead of at the root, so the item list grows
 * while the pagination fields sitting beside it are replaced by this response's fresh ones.
 * Second, it announces a {@code scrollProps} entry carrying the pagination cursor
 * ({@link ProvidesScrollMetadata}), which is how the client knows which page to ask for next —
 * and how it knows to stop, when {@code nextPage} comes back {@code null}.
 * <p>
 * The merge direction is not a server-side decision: the client sends it per request via
 * {@code X-Inertia-Infinite-Scroll-Merge-Intent} (scrolling down appends, scrolling back up
 * prepends), and {@link #configureMergeIntent} applies it. That mirrors
 * {@code ScrollProp::configureMergeIntent()}, except this returns a reconfigured copy rather than
 * mutating in place, since props here are immutable.
 *
 * <pre>{@code
 * Map.of("posts", new ScrollProp(
 *     () -> Map.of("data", buscarPagina(pagina)),
 *     ScrollPage.numbered("pagina", pagina, temMais)
 * ).matchOn("id"))
 * }</pre>
 *
 * @see <a href="https://inertiajs.com/infinite-scrolling">Inertia infinite scrolling</a>
 */
public class ScrollProp extends AbstractProp<ScrollProp> implements Deferrable {

    /** The sub-path merged into by default, matching Laravel's {@code $wrapper} default. */
    public static final String DEFAULT_WRAPPER = "data";

    private static final String DEFAULT_GROUP = "default";

    private final String wrapper;
    private final Function<Object, ProvidesScrollMetadata> metadataProvider;
    private final boolean deferred;
    private final String group;

    /**
     * Paginates {@code callback}'s value, merging its {@value #DEFAULT_WRAPPER} sub-path.
     *
     * @param callback supplies this page of the paginated value.
     * @param metadata the pagination cursor to announce to the client.
     */
    public ScrollProp(Supplier<Object> callback, ProvidesScrollMetadata metadata) {
        this(callback, DEFAULT_WRAPPER, metadata);
    }

    /**
     * Paginates {@code callback}'s value, merging the given sub-path of it.
     *
     * @param callback supplies this page of the paginated value.
     * @param wrapper the sub-path, relative to the value, holding the array that grows.
     * @param metadata the pagination cursor to announce to the client.
     */
    public ScrollProp(Supplier<Object> callback, String wrapper, ProvidesScrollMetadata metadata) {
        this(callback, wrapper, resolved -> metadata, PropState.DEFAULT.withMerge(Strategy.APPEND), false, DEFAULT_GROUP);
    }

    /**
     * Paginates {@code callback}'s value, deriving the pagination cursor from the resolved value
     * itself — for when the value already carries its own pagination state (a page/slice object
     * from the persistence layer, say) and restating it separately would be duplication.
     *
     * @param callback supplies this page of the paginated value.
     * @param wrapper the sub-path, relative to the value, holding the array that grows.
     * @param metadataProvider derives the pagination cursor from the resolved value.
     */
    public ScrollProp(
        Supplier<Object> callback,
        String wrapper,
        Function<Object, ProvidesScrollMetadata> metadataProvider
    ) {
        this(callback, wrapper, metadataProvider, PropState.DEFAULT.withMerge(Strategy.APPEND), false, DEFAULT_GROUP);
    }

    private ScrollProp(
        Supplier<Object> callback,
        String wrapper,
        Function<Object, ProvidesScrollMetadata> metadataProvider,
        PropState state,
        boolean deferred,
        String group
    ) {
        super(callback, state);
        this.wrapper = wrapper;
        this.metadataProvider = metadataProvider;
        this.deferred = deferred;
        this.group = group;
    }

    @Override
    ScrollProp withState(PropState newState) {
        return new ScrollProp(callback, wrapper, metadataProvider, newState, deferred, group);
    }

    @Override
    public boolean shouldDefer() {
        return deferred;
    }

    @Override
    public String getGroup() {
        return group;
    }

    /**
     * Skips this prop on the initial page load, letting the client fetch the first page in a
     * follow-up request — the same opt-in {@code ScrollProp} gets from Laravel's
     * {@code DefersProps} trait (unlike {@link DeferProp}, a scroll prop is <em>not</em> deferred
     * by default).
     *
     * @return this scroll prop, deferred under the default request group.
     */
    public ScrollProp defer() {
        return defer(DEFAULT_GROUP);
    }

    /**
     * @param group the request group name this prop is fetched together with.
     * @return this scroll prop, deferred under {@code group}.
     */
    public ScrollProp defer(String group) {
        return new ScrollProp(callback, wrapper, metadataProvider, state, true, group);
    }

    /**
     * @return the sub-path of this prop's value holding the array that grows.
     */
    public String getWrapper() {
        return wrapper;
    }

    /**
     * Points this prop's merge at its wrapper sub-path, in the direction the client asked for.
     * Called by the renderer just before resolving, mirroring where
     * {@code PropsResolver::resolveValue()} calls its Laravel counterpart.
     *
     * @param prepend whether the client sent {@code prepend} as its merge intent (it is scrolling
     *                back up); {@code false} appends, which is both the scroll-down case and the
     *                default when the header is absent.
     * @return an equivalent scroll prop merging at {@code wrapper} in that direction.
     */
    public ScrollProp configureMergeIntent(boolean prepend) {
        return withState(state.withSoleMergePath(wrapper, prepend));
    }

    /**
     * The pagination cursor to announce for this response.
     *
     * @param resolvedValue this prop's already-resolved value, handed in so a value-derived
     *                      provider doesn't have to resolve it a second time (Laravel memoizes
     *                      inside {@code ScrollProp} for the same reason).
     * @return the pagination cursor.
     */
    public ProvidesScrollMetadata getScrollMetadata(Object resolvedValue) {
        return metadataProvider.apply(resolvedValue);
    }
}
