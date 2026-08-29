package io.github.inertia4j.core;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.inertia4j.spi.PageObject;
import io.github.inertia4j.spi.PageObjectSerializer;
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
     * The Jackson ObjectMapper instance used for serialization.
     * Configured to order map entries by keys for consistent output.
     */
    private final ObjectMapper objectMapper = new ObjectMapper()
        .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);

    /**
     * {@inheritDoc}
     * <p>
     * If {@code partialDataProps} is provided, only the properties specified
     * in the list will be included under the "props" key in the resulting JSON.
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
            // Metadata Emission (https://inertiajs.com/the-protocol#metadata-emission): the field
            // must be absent, not an empty object, when there is nothing deferred on this page.
            if (tree.get("deferredProps").isEmpty()) {
                tree.remove("deferredProps");
            }
            return objectMapper.writeValueAsString(tree);
        } catch (JsonProcessingException e) {
            throw new SerializationException(e);
        }
    }
}
