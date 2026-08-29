package io.github.inertia4j.spi;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * One entry of a {@link PageObject}'s {@code onceProps}: which prop path a once-cache key refers
 * to, and when the client's cached copy expires.
 *
 * @see <a href="https://inertiajs.com/once-props">Inertia once props</a>
 */
@NullMarked
public final class OnceMetadata {
    private final String prop;
    private final @Nullable Long expiresAt;

    /**
     * @param prop the dotted path of the prop this cache entry refers to.
     * @param expiresAt epoch millis the client's cached copy expires at, or {@code null} if it
     *                  never expires on its own.
     */
    public OnceMetadata(String prop, @Nullable Long expiresAt) {
        this.prop = prop;
        this.expiresAt = expiresAt;
    }

    /**
     * @return the dotted path of the prop this cache entry refers to.
     */
    public String getProp() {
        return prop;
    }

    /**
     * @return epoch millis the client's cached copy expires at, or {@code null} if it never
     *         expires on its own.
     */
    public @Nullable Long getExpiresAt() {
        return expiresAt;
    }
}
