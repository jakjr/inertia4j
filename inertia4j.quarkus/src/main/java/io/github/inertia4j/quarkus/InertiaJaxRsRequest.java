package io.github.inertia4j.quarkus;

import io.github.inertia4j.core.HttpRequest;
import jakarta.ws.rs.container.ContainerRequestContext;

class InertiaJaxRsRequest implements HttpRequest {
    private final ContainerRequestContext ctx;

    InertiaJaxRsRequest(ContainerRequestContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public String getHeader(String name) {
        return ctx.getHeaderString(name);
    }

    @Override
    public String getMethod() {
        return ctx.getMethod();
    }
}
