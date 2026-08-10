package org.omni.packets.data;

import java.util.UUID;

public record ProfileRequestData(
        UUID requestId,
        byte[] analyticId
) {
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProfileRequestData that = (ProfileRequestData) o;
        return
                java.util.Arrays.equals(analyticId, that.analyticId) &&
                        java.util.Objects.equals(requestId, that.requestId);
    }

    @Override
    public int hashCode() {
        int result = java.util.Objects.hash(requestId);
        result = 31 * result + java.util.Arrays.hashCode(analyticId);
        return result;
    }
}