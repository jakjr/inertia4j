package io.github.inertia4j.core.props;

import java.util.function.Supplier;

/**
 * Wraps a prop value that should load in a follow-up request instead of the initial page visit —
 * the Java equivalent of Laravel's {@code Inertia::defer()} / Rails' {@code InertiaRails.defer}.
 * Also merge-, once-, and rescue-able (see {@link AbstractProp}, {@link #rescue()}): a paginated
 * list can both load lazily <em>and</em> grow as the user scrolls, for instance.
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
public class DeferProp extends AbstractProp<DeferProp> implements Deferrable, Rescuable {

    private static final String DEFAULT_GROUP = "default";

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
        this(callback, group, PropState.DEFAULT, false);
    }

    private DeferProp(Supplier<Object> callback, String group, PropState state, boolean rescue) {
        super(callback, state);
        this.group = group;
        this.rescue = rescue;
    }

    @Override
    DeferProp withState(PropState newState) {
        return new DeferProp(callback, group, newState, rescue);
    }

    @Override
    public boolean shouldDefer() {
        return true;
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
        return new DeferProp(callback, group, state, true);
    }
}
