package io.github.inertia4j.core.props;

/**
 * A prop resolved once and cached by the client — a follow-up visit that already has a fresh
 * copy (announced via {@code X-Inertia-Except-Once-Props}) skips resolving and sending it again.
 *
 * @see <a href="https://inertiajs.com/the-protocol#once-props">Inertia once props</a>
 */
public interface Onceable {
    /**
     * @return whether this prop should be resolved only once and cached client-side.
     */
    boolean shouldResolveOnce();

    /**
     * @return the cache key the client tracks this prop under, or {@code null} to default to its
     *         own path.
     */
    String getKey();

    /**
     * @return whether this prop should always resolve and send, ignoring any cached copy the
     *         client claims to already have.
     */
    boolean shouldBeRefreshed();

    /**
     * @return how long (in epoch millis, computed relative to now) the client's cached copy stays
     *         valid, or {@code null} if it never expires on its own.
     */
    Long getExpiresAtMillis();
}
