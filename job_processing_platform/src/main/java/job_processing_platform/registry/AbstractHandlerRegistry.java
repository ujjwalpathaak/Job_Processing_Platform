package job_processing_platform.registry;

import jakarta.annotation.PostConstruct;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class AbstractHandlerRegistry<K, H> {

    protected final Map<K, H> registry = new HashMap<>();
    private final List<H> handlers;

    protected AbstractHandlerRegistry(List<H> handlers) {
        this.handlers = handlers;
    }

    /**
     * Each concrete registry defines how to extract the key
     */
    protected abstract K identify(H handler);

    /**
     * Registry name for better exception messages
     */
    protected abstract String registryName();

    @PostConstruct
    public void init() {
        for (H handler : handlers) {
            K key = identify(handler);

            if (key == null || (key instanceof String && ((String) key).isBlank())) {
                throw new IllegalStateException(
                        registryName() + " identify() cannot be null/blank: "
                                + handler.getClass().getName()
                );
            }

            if (registry.containsKey(key)) {
                throw new IllegalStateException(
                        "Duplicate " + registryName() + " for key: " + key
                                + " (" + handler.getClass().getName() + ")"
                );
            }

            registry.put(key, handler);
        }

        if (registry.isEmpty()) {
            throw new IllegalStateException("No " + registryName() + " registered!");
        }
    }

    public H get(K key) {
        H handler = registry.get(key);

        if (handler == null) {
            throw new IllegalStateException(
                    "No " + registryName() + " found for key: " + key
            );
        }

        return handler;
    }
}