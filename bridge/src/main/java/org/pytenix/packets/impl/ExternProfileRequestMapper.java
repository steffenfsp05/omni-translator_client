package org.pytenix.packets.impl;

import com.google.protobuf.ByteString;
import org.pytenix.packets.AbstractPacketMapper;
import org.pytenix.proto.generated.NetworkPackets;

import java.util.UUID;

public class ExternProfileRequestMapper extends AbstractPacketMapper<NetworkPackets.ProfileExternRequest, ExternProfileRequestMapper.ExternProfileRequestData> {


public ExternProfileRequestMapper() {
    super(NetworkPackets.ProfileExternRequest.class, ExternProfileRequestData.class);
}

@Override
public NetworkPackets.ProfileExternRequest to(ExternProfileRequestData packet) {
    return NetworkPackets.ProfileExternRequest.newBuilder()
            .setLicense(packet.license())
            .setRequestIdMostSig(packet.requestId().getMostSignificantBits())
            .setRequestIdLeastSig(packet.requestId().getLeastSignificantBits())
            .setAnalyticsId(ByteString.copyFrom(packet.analyticId()))
            .build();
}

@Override
public ExternProfileRequestData from(NetworkPackets.ProfileExternRequest packet) {
    return new ExternProfileRequestData(
            packet.getLicense(),
            packet.getAnalyticsId().toByteArray(),
            new UUID(packet.getRequestIdMostSig(), packet.getRequestIdLeastSig())
    );
}

public record ExternProfileRequestData(
        String license,
        byte[] analyticId,
        UUID requestId
) {
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ExternProfileRequestData that = (ExternProfileRequestData) o;
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
}
