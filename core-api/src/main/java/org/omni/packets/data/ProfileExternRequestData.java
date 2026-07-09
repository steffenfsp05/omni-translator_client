package org.omni.packets.data;

import java.util.UUID;

public record ProfileExternRequestData(
        String license,
        byte[] analyticId,
        UUID requestId
) {
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProfileExternRequestData that = (ProfileExternRequestData) o;
        return java.util.Objects.equals(license, that.license) &&
                java.util.Arrays.equals(analyticId, that.analyticId) &&
                java.util.Objects.equals(requestId, that.requestId);
    }

    @Override
    public int hashCode() {
        int result = java.util.Objects.hash(license, requestId);
        result = 31 * result + java.util.Arrays.hashCode(analyticId);
        return result;
    }
}