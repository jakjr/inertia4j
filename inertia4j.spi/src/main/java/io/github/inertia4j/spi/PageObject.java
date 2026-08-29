package io.github.inertia4j.spi;

import org.jspecify.annotations.NullMarked;

import java.util.List;
import java.util.Map;

/**
 * Internal representation of an Inertia Page Object.
 * This object is serialized and included in the server responses.
 *
 * @see <a href="https://inertiajs.com/the-protocol#the-page-object">Inertia Page Object spec</a>
 */
@NullMarked
public class PageObject {
    private final String component;
    private final Map<String, Object> props;
    private final String url;
    private final Object version;
    private final boolean encryptHistory;
    private final boolean clearHistory;
    private final Map<String, List<String>> deferredProps;
    private final MergeInstructions mergeInstructions;
    private final List<String> rescuedProps;
    private final Map<String, ScrollMetadata> scrollProps;
    private final Map<String, OnceMetadata> onceProps;

    /**
     * Constructs a new PageObject.
     *
     * @param component component to be rendered by the client.
     * @param props data to be served to client.
     * @param url value of the URL field in response.
     * @param encryptHistory flag set to encrypt previous browsing activity.
     * @param clearHistory flag set to clear previous browsing activity.
     * @param version asset version to be compared with current client asset version.
     * @param deferredProps deferred prop keys, grouped by their request group name. Empty when
     *                      there are no deferred props on this page — a {@link PageObjectSerializer}
     *                      is expected to omit the field entirely in that case, per the protocol's
     *                      "only relevant labels appear per response" rule.
     * @param mergeInstructions which props on this page should be merged (rather than replaced)
     *                          client-side, and how. {@link MergeInstructions#none()} when there
     *                          are none — a {@link PageObjectSerializer} is expected to omit each
     *                          of its four fields individually when empty, per the protocol's
     *                          "only relevant labels appear per response" rule.
     * @param rescuedProps keys of props whose resolution threw and was swallowed. Empty when none
     *                     — a {@link PageObjectSerializer} is expected to omit the field entirely
     *                     in that case.
     * @param scrollProps infinite-scroll pagination metadata, keyed by the dotted path of the
     *                    scroll prop it describes. Empty when none — a
     *                    {@link PageObjectSerializer} is expected to omit the field entirely in
     *                    that case.
     * @param onceProps once-cached props on this page, keyed by their (custom or default) cache
     *                  key. Empty when none — a {@link PageObjectSerializer} is expected to omit
     *                  the field entirely in that case.
     */
    public PageObject(
        String component,
        Map<String, Object> props,
        String url,
        boolean encryptHistory,
        boolean clearHistory,
        Object version,
        Map<String, List<String>> deferredProps,
        MergeInstructions mergeInstructions,
        List<String> rescuedProps,
        Map<String, ScrollMetadata> scrollProps,
        Map<String, OnceMetadata> onceProps
    ) {
        this.component = component;
        this.props = props;
        this.url = url;
        this.encryptHistory = encryptHistory;
        this.clearHistory = clearHistory;
        this.version = version;
        this.deferredProps = deferredProps;
        this.mergeInstructions = mergeInstructions;
        this.rescuedProps = rescuedProps;
        this.scrollProps = scrollProps;
        this.onceProps = onceProps;
    }

    /**
     * Gets the name of the component to be rendered by the client.
     *
     * @return component name.
     */
    public String getComponent() {
        return component;
    }

    /**
     * Gets the data to be served to client.
     *
     * @return props data.
     */
    public Map<String, Object> getProps() {
        return props;
    }

    /**
     * Gets the value of the URL field.
     *
     * @return URL.
     */
    public String getUrl() {
        return url;
    }

    /**
     * Gets the current version of the project assets.
     *
     * @return version.
     */
    public Object getVersion() {
        return version;
    }

    /**
     * Gets the current value of the encryptHistory flag.
     *
     * @return value of the encryptHistory flag.
     * @see <a href="https://inertiajs.com/history-encryption">Inertia encryptHistory flag</a>
     */
    public boolean isEncryptHistory() {
        return encryptHistory;
    }

    /**
     * Gets the current value of the clearHistory flag.
     *
     * @return value of the clearHistory flag.
     * @see <a href="https://inertiajs.com/history-encryption#clearing-history">Inertia clearHistory flag</a>
     */
    public boolean isClearHistory() {
        return clearHistory;
    }

    /**
     * Gets the deferred prop keys announced for this page, grouped by their request group name
     * (the value passed as the second argument when the prop was deferred server-side).
     *
     * @return deferred prop keys by group; empty when nothing was deferred.
     * @see <a href="https://inertiajs.com/deferred-props">Inertia deferred props</a>
     */
    public Map<String, List<String>> getDeferredProps() {
        return deferredProps;
    }

    /**
     * Gets the keys of props to be appended to their existing client-side value, instead of
     * replacing it — flattened here (rather than nesting the whole {@link MergeInstructions}
     * object) so a reflection-based {@link PageObjectSerializer} like the Jackson one produces the
     * flat top-level {@code mergeProps} field the protocol expects.
     *
     * @return merge-by-append prop keys; empty when none.
     * @see <a href="https://inertiajs.com/merging-props">Inertia merging props</a>
     */
    public List<String> getMergeProps() {
        return mergeInstructions.getMergeProps();
    }

    /**
     * Gets the keys of props to be prepended to their existing client-side value.
     *
     * @return merge-by-prepend prop keys; empty when none.
     * @see <a href="https://inertiajs.com/merging-props">Inertia merging props</a>
     */
    public List<String> getPrependProps() {
        return mergeInstructions.getPrependProps();
    }

    /**
     * Gets the keys of props whose entire structure should be deep-merged into the existing
     * client-side value.
     *
     * @return deep-merge prop keys; empty when none.
     * @see <a href="https://inertiajs.com/merging-props">Inertia merging props</a>
     */
    public List<String> getDeepMergeProps() {
        return mergeInstructions.getDeepMergeProps();
    }

    /**
     * Gets the {@code "<prop>.<path>"} entries identifying the field used to match existing items
     * during a merge, instead of appending/prepending duplicates.
     *
     * @return match-on entries; empty when none.
     * @see <a href="https://inertiajs.com/merging-props#matching-on-a-different-key">Inertia merging props — matching items</a>
     */
    public List<String> getMatchPropsOn() {
        return mergeInstructions.getMatchPropsOn();
    }

    /**
     * Gets the keys of props whose resolution threw and was swallowed (see
     * {@code Rescuable} in {@code inertia4j.core}), so the client knows that prop simply failed
     * to load this time instead of silently missing.
     *
     * @return rescued prop keys; empty when none.
     * @see <a href="https://inertiajs.com/deferred-props">Inertia deferred props</a>
     */
    public List<String> getRescuedProps() {
        return rescuedProps;
    }

    /**
     * Gets the infinite-scroll pagination metadata announced for this page, keyed by the dotted
     * path of the scroll prop each entry describes — so the client's infinite-scroll component
     * knows which page identifier to request next (or previously), and whether to reset what it
     * has accumulated.
     *
     * @return scroll metadata by prop path; empty when none.
     * @see <a href="https://inertiajs.com/infinite-scrolling">Inertia infinite scrolling</a>
     */
    public Map<String, ScrollMetadata> getScrollProps() {
        return scrollProps;
    }

    /**
     * Gets the once-cached props announced for this page, keyed by their (custom or default)
     * cache key — so the client knows which key to send back via
     * {@code X-Inertia-Except-Once-Props} once it has a fresh copy.
     *
     * @return once-prop metadata by cache key; empty when none.
     * @see <a href="https://inertiajs.com/once-props">Inertia once props</a>
     */
    public Map<String, OnceMetadata> getOnceProps() {
        return onceProps;
    }
}
