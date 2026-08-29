package io.github.inertia4j.core.props;

import java.util.function.Supplier;

/**
 * Wraps a prop value that the client should merge into its existing value on a partial reload,
 * instead of replacing it — the Java equivalent of Laravel's {@code Inertia::merge()} / Rails'
 * {@code InertiaRails.merge}. Appends by default. Also once-able (see {@link AbstractProp}): a
 * merged list can be cached client-side too.
 *
 * <pre>{@code
 * // Novos posts sao acrescentados aos que o cliente ja tem, casando por "id" pra
 * // atualizar (nao duplicar) posts que ja existiam.
 * Map.of("posts", new MergeProp(buscarProximaPagina()).matchOn("id"))
 * }</pre>
 *
 * @see <a href="https://inertiajs.com/merging-props">Inertia merging props</a>
 */
public class MergeProp extends AbstractProp<MergeProp> {

    /**
     * Wraps {@code value} to be appended into the client's existing value for this prop.
     *
     * @param value the (already computed) value for this response.
     */
    public MergeProp(Object value) {
        this(() -> value, PropState.DEFAULT.withMerge(Strategy.APPEND));
    }

    private MergeProp(Supplier<Object> callback, PropState state) {
        super(callback, state);
    }

    @Override
    MergeProp withState(PropState newState) {
        return new MergeProp(callback, newState);
    }
}
