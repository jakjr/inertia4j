package io.github.inertia4j.spi;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * One entry of a {@link PageObject}'s {@code scrollProps}: the pagination cursor the client's
 * infinite-scroll component needs to know which page to ask for next (or previously), and whether
 * it should throw away what it has accumulated so far.
 * <p>
 * The three page identifiers are deliberately typed {@code Object}: the protocol allows either a
 * page number ({@code Integer}) or an opaque cursor ({@code String}), and {@code null} for "there
 * is no page in that direction" — which is exactly how the client decides whether more content
 * exists in each direction.
 *
 * @see <a href="https://inertiajs.com/infinite-scrolling">Inertia infinite scrolling</a>
 */
@NullMarked
public final class ScrollMetadata {
    private final String pageName;
    private final @Nullable Object previousPage;
    private final @Nullable Object nextPage;
    private final @Nullable Object currentPage;
    private final boolean reset;

    /**
     * @param pageName the request parameter name the client puts the page identifier in.
     * @param previousPage identifier of the page before the current one, or {@code null} if there
     *                     is none.
     * @param nextPage identifier of the page after the current one, or {@code null} if there is
     *                 none (i.e. the end of the list has been reached).
     * @param currentPage identifier of the page carried by this response.
     * @param reset whether the client should discard its accumulated pages and start over with
     *              this response's items.
     */
    public ScrollMetadata(
        String pageName,
        @Nullable Object previousPage,
        @Nullable Object nextPage,
        @Nullable Object currentPage,
        boolean reset
    ) {
        this.pageName = pageName;
        this.previousPage = previousPage;
        this.nextPage = nextPage;
        this.currentPage = currentPage;
        this.reset = reset;
    }

    /**
     * @return the request parameter name the client puts the page identifier in.
     */
    public String getPageName() {
        return pageName;
    }

    /**
     * @return identifier of the page before the current one, or {@code null} if there is none.
     */
    public @Nullable Object getPreviousPage() {
        return previousPage;
    }

    /**
     * @return identifier of the page after the current one, or {@code null} if there is none.
     */
    public @Nullable Object getNextPage() {
        return nextPage;
    }

    /**
     * @return identifier of the page carried by this response.
     */
    public @Nullable Object getCurrentPage() {
        return currentPage;
    }

    /**
     * @return whether the client should discard its accumulated pages and start over with this
     *         response's items.
     */
    public boolean isReset() {
        return reset;
    }
}
