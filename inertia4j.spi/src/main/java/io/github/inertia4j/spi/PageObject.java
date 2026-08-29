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
     */
    public PageObject(
        String component,
        Map<String, Object> props,
        String url,
        boolean encryptHistory,
        boolean clearHistory,
        Object version,
        Map<String, List<String>> deferredProps
    ) {
        this.component = component;
        this.props = props;
        this.url = url;
        this.encryptHistory = encryptHistory;
        this.clearHistory = clearHistory;
        this.version = version;
        this.deferredProps = deferredProps;
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
}
