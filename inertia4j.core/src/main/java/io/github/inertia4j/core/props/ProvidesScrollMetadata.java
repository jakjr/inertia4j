package io.github.inertia4j.core.props;

/**
 * Supplies the pagination cursor a {@link ScrollProp} announces to the client — the Java
 * equivalent of Laravel's {@code ProvidesScrollMetadata}.
 * <p>
 * It exists as an interface (rather than {@link ScrollPage} being the only option) for the same
 * reason it does in inertia-laravel, whose {@code ScrollMetadata::fromPaginator()} knows how to
 * read Eloquent's paginators: there is no single paginator type in Java either, so an application
 * with its own page/slice abstraction (Panache's {@code PanacheQuery}, Spring Data's
 * {@code Page}, a cursor DTO) can implement this directly instead of restating its pagination
 * state at every render.
 *
 * @see <a href="https://inertiajs.com/infinite-scrolling">Inertia infinite scrolling</a>
 */
public interface ProvidesScrollMetadata {
    /**
     * @return the request parameter name the client puts the page identifier in (e.g.
     *         {@code "page"} or a cursor parameter's name).
     */
    String getPageName();

    /**
     * @return identifier of the page before the current one ({@code Integer} or {@code String}),
     *         or {@code null} when the current page is the first.
     */
    Object getPreviousPage();

    /**
     * @return identifier of the page after the current one ({@code Integer} or {@code String}),
     *         or {@code null} when there is nothing more to load — this is what stops the
     *         client's infinite scroll.
     */
    Object getNextPage();

    /**
     * @return identifier of the page carried by this response.
     */
    Object getCurrentPage();
}
