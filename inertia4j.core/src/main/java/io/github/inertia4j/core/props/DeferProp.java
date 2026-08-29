package io.github.inertia4j.core.props;

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
public class DeferProp implements ResolvableProp, Deferrable {

    private static final String DEFAULT_GROUP = "default";

    private final Supplier<Object> callback;
    private final String group;

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
        this.callback = callback;
        this.group = group;
    }

    @Override
    public Object resolve() {
        return callback.get();
    }

    @Override
    public String getGroup() {
        return group;
    }
}
