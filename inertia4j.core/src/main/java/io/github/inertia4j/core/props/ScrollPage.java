package io.github.inertia4j.core.props;

/**
 * A plain, immutable {@link ProvidesScrollMetadata} — the ready-made option for when the
 * pagination state is right there at render time and doesn't warrant its own type.
 *
 * <pre>{@code
 * // Pagina 2 de uma paginacao numerada, e ainda ha mais:
 * ScrollPage.numbered("pagina", 2, true);
 *
 * // Paginacao por cursor:
 * ScrollPage.cursor("cursor", cursorAnterior, proximoCursor, cursorAtual);
 * }</pre>
 *
 * @see <a href="https://inertiajs.com/infinite-scrolling">Inertia infinite scrolling</a>
 */
public final class ScrollPage implements ProvidesScrollMetadata {

    private final String pageName;
    private final Object previousPage;
    private final Object nextPage;
    private final Object currentPage;

    /**
     * @param pageName the request parameter name the client puts the page identifier in.
     * @param previousPage identifier of the previous page, or {@code null} if there is none.
     * @param nextPage identifier of the next page, or {@code null} if there is none.
     * @param currentPage identifier of the page carried by this response.
     */
    public ScrollPage(String pageName, Object previousPage, Object nextPage, Object currentPage) {
        this.pageName = pageName;
        this.previousPage = previousPage;
        this.nextPage = nextPage;
        this.currentPage = currentPage;
    }

    /**
     * Derives the previous/next identifiers of a numbered (offset) pagination the same way
     * {@code ScrollMetadata::fromPaginator()} does for Laravel's {@code LengthAwarePaginator}:
     * the previous page is {@code currentPage - 1} unless already on the first page, and the next
     * page is {@code currentPage + 1} only while more pages exist.
     * <p>
     * Note this counts pages from 1, like every Inertia adapter's default — an API that pages
     * from 0 (Panache's {@code Page.of}, Spring Data's {@code PageRequest}) must add one before
     * calling this, or use the {@link ScrollPage constructor} directly.
     *
     * @param pageName the request parameter name carrying the page number.
     * @param currentPage the 1-based number of the page carried by this response.
     * @param hasMorePages whether at least one more page exists after this one.
     * @return the corresponding scroll metadata.
     */
    public static ScrollPage numbered(String pageName, int currentPage, boolean hasMorePages) {
        return new ScrollPage(
            pageName,
            currentPage > 1 ? currentPage - 1 : null,
            hasMorePages ? currentPage + 1 : null,
            currentPage
        );
    }

    /**
     * The cursor-pagination counterpart of {@link #numbered}: the identifiers are opaque cursor
     * strings the application already computed, so there is nothing to derive.
     *
     * @param cursorName the request parameter name carrying the cursor.
     * @param previousCursor cursor of the previous page, or {@code null} if there is none.
     * @param nextCursor cursor of the next page, or {@code null} if there is none.
     * @param currentCursor cursor of the page carried by this response.
     * @return the corresponding scroll metadata.
     */
    public static ScrollPage cursor(
        String cursorName,
        String previousCursor,
        String nextCursor,
        String currentCursor
    ) {
        return new ScrollPage(cursorName, previousCursor, nextCursor, currentCursor);
    }

    @Override
    public String getPageName() {
        return pageName;
    }

    @Override
    public Object getPreviousPage() {
        return previousPage;
    }

    @Override
    public Object getNextPage() {
        return nextPage;
    }

    @Override
    public Object getCurrentPage() {
        return currentPage;
    }
}
