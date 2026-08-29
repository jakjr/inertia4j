package io.github.inertia4j.core;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.inertia4j.spi.PageObject;
import io.github.inertia4j.spi.PageObjectSerializer;
import io.github.inertia4j.spi.ScrollMetadata;
import io.github.inertia4j.spi.SerializationException;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * {@link PageObjectSerializer} implementation using Jackson for JSON serialization.
 */
@NullMarked
public class JacksonPageObjectSerializer implements PageObjectSerializer {
    /**
     * Fields that the Inertia protocol's "Metadata Emission" rule
     * (https://inertiajs.com/the-protocol#metadata-emission) requires to be entirely absent —
     * not present-but-empty — when there is nothing to report for this page.
     */
    private static final List<String> OMIT_WHEN_EMPTY = List.of(
        "deferredProps", "mergeProps", "prependProps", "deepMergeProps", "matchPropsOn",
        "rescuedProps", "scrollProps", "onceProps", "flash", "sharedProps"
    );

    /**
     * Pins {@link PageObject}'s JSON field order deterministically. Without this, Jackson's
     * reflection-based POJO introspection has no ordering guarantee beyond "usually, but not
     * reliably, declaration order" — a mixin (rather than an annotation on {@link PageObject}
     * itself) keeps {@code inertia4j.spi} free of a Jackson dependency.
     */
    @JsonPropertyOrder({
        "component", "props", "url", "version", "encryptHistory", "clearHistory", "flash",
        "sharedProps", "deferredProps", "mergeProps", "prependProps", "deepMergeProps",
        "matchPropsOn", "rescuedProps", "scrollProps", "onceProps"
    })
    private interface PageObjectPropertyOrder {
    }

    /**
     * Pins the field order of each {@code scrollProps} entry, for the same reason (and by the
     * same means) as {@link PageObjectPropertyOrder}.
     */
    @JsonPropertyOrder({"pageName", "previousPage", "nextPage", "currentPage", "reset"})
    private interface ScrollMetadataPropertyOrder {
    }

    /**
     * The Jackson ObjectMapper instance used for serialization.
     * Configured to order map entries by keys for consistent output.
     */
    private final ObjectMapper objectMapper = new ObjectMapper()
        .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true)
        .addMixIn(PageObject.class, PageObjectPropertyOrder.class)
        .addMixIn(ScrollMetadata.class, ScrollMetadataPropertyOrder.class);

    /**
     * {@inheritDoc}
     * <p>
     * If {@code partialDataProps} is provided, only the top-level properties specified in the
     * list are retained under the "props" key. {@link io.github.inertia4j.core.InertiaRenderer}
     * itself never passes anything here (its {@code pageObject.getProps()} is already exactly the
     * filtered set — including nested-path filtering that a flat top-level retain cannot express),
     * so this only matters for a caller that builds/serializes a {@link PageObject} directly.
     */
    @Override
    public String serialize(
        PageObject pageObject,
        @Nullable List<String> partialDataProps
    ) throws SerializationException {
        try {
            ObjectNode tree = objectMapper.valueToTree(pageObject);
            if (partialDataProps != null) {
                ObjectNode propsNode = (ObjectNode) tree.get("props");
                propsNode.retain(partialDataProps);
            }
            for (String field : OMIT_WHEN_EMPTY) {
                if (tree.get(field).isEmpty()) {
                    tree.remove(field);
                }
            }
            return objectMapper.writeValueAsString(tree);
        } catch (JsonProcessingException e) {
            throw new SerializationException(e);
        }
    }
}
