package io.github.inertia4j.core.props;

import java.util.List;
import java.util.function.Supplier;

/**
 * Wraps a prop value that should load in a follow-up request instead of the initial page visit —
 * the Java equivalent of Laravel's {@code Inertia::defer()} / Rails' {@code InertiaRails.defer}.
 *
 * <pre>{@code
 * Map.of(
 *     "tarefas", Tarefa.listAll(),
 *     "estatisticas", new DeferProp(() -> Tarefa.calcularEstatisticas())
 * )
 * }</pre>
 *
 * @see <a href="https://inertiajs.com/deferred-props">Inertia deferred props</a>
 */
public class DeferProp implements ResolvableProp, Deferrable, Rescuable {

    private static final String DEFAULT_GROUP = "default";

    private final Supplier<Object> callback;
    private final String group;
    private final boolean rescue;

    /**
     * Defers {@code callback} under the default request group.
     *
     * @param callback supplies the value once the client requests it.
     */
    public DeferProp(Supplier<Object> callback) {
        this(callback, DEFAULT_GROUP);
    }

    /**
     * Defers {@code callback} under a named request group, so it is fetched together with any
     * other prop deferred under the same group.
     *
     * @param callback supplies the value once the client requests it.
     * @param group    the request group name.
     */
    public DeferProp(Supplier<Object> callback, String group) {
        this(callback, group, false);
    }

    private DeferProp(Supplier<Object> callback, String group, boolean rescue) {
        this.callback = callback;
        this.group = group;
        this.rescue = rescue;
    }

    @Override
    public Object resolve() {
        return callback.get();
    }

    @Override
    public String getGroup() {
        return group;
    }

    @Override
    public boolean shouldRescue() {
        return rescue;
    }

    /**
     * Marks this deferred prop so that, if resolving it throws, the exception is swallowed and
     * the prop's key is reported under {@code rescuedProps} instead of failing the whole partial
     * reload — useful when this prop's data source is known to be flaky and the rest of the page
     * shouldn't go down with it.
     *
     * @return this deferred prop, rescued on failure.
     */
    public DeferProp rescue() {
        return new DeferProp(callback, group, true);
    }

    /**
     * @return this deferred prop, also merged by appending once resolved.
     * @see <a href="https://inertiajs.com/merging-props#combining-with-deferred-props">Inertia — combining merge with deferred props</a>
     */
    public MergeableDeferProp merge() {
        return new MergeableDeferProp(this, Mergeable.Strategy.APPEND, List.of());
    }

    /**
     * @return this deferred prop, also merged by prepending once resolved.
     */
    public MergeableDeferProp prepend() {
        return new MergeableDeferProp(this, Mergeable.Strategy.PREPEND, List.of());
    }

    /**
     * @return this deferred prop, also deep-merged once resolved.
     */
    public MergeableDeferProp deepMerge() {
        return new MergeableDeferProp(this, Mergeable.Strategy.DEEP, List.of());
    }
}
