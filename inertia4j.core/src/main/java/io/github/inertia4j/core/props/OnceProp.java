package io.github.inertia4j.core.props;

import java.util.function.Supplier;

/**
 * Wraps a prop value that should be resolved once and cached client-side — the Java equivalent of
 * Laravel's {@code Inertia::once()}. A follow-up visit that already has a fresh copy (announced
 * via {@code X-Inertia-Except-Once-Props}) skips resolving and sending it again.
 * <p>
 * Deliberately extends {@link AbstractOnceProp} rather than {@link AbstractProp}: real
 * {@code OnceProp.php} is {@code implements Onceable} alone (no {@code MergesProps} trait), and
 * Rails' {@code once_prop.rb} matches — neither gives a plain once prop merge capability, so this
 * doesn't either. (A prop that is both merge- and once-able is real and supported — see
 * {@link DeferProp}/{@link MergeProp}, which upstream does compose with both.)
 *
 * <pre>{@code
 * Map.of("planos", new OnceProp(() -> Plano.listAll()).until(Duration.ofDays(1).toMillis()))
 * }</pre>
 *
 * @see <a href="https://inertiajs.com/once-props">Inertia once props</a>
 */
public class OnceProp extends AbstractOnceProp<OnceProp> {

    /**
     * Wraps {@code callback}, resolved once and cached client-side under its own path.
     *
     * @param callback supplies the value the first time it is resolved.
     */
    public OnceProp(Supplier<Object> callback) {
        this(callback, PropState.DEFAULT.withOnce());
    }

    private OnceProp(Supplier<Object> callback, PropState state) {
        super(callback, state);
    }

    @Override
    OnceProp withState(PropState newState) {
        return new OnceProp(callback, newState);
    }
}
