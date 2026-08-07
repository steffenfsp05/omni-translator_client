package org.omni.packets.impl;

import com.google.protobuf.ByteString;
import org.omni.packets.AbstractPacketMapper;
import org.omni.packets.data.DataExportRequestData;
import org.omni.proto.generated.Protobuf;

import java.util.UUID;

public class DataExportRequestMapper extends AbstractPacketMapper<Protobuf.DataExportRequest, DataExportRequestData> {
    public DataExportRequestMapper() {
        super(Protobuf.DataExportRequest.class, DataExportRequestData.class);
    }

    @Override
    public Protobuf.DataExportRequest to(DataExportRequestData packet) {
        return Protobuf.DataExportRequest.newBuilder()
                .setRequestIdMostSig(packet.requestId().getMostSignificantBits())
                .setRequestIdLeastSig(packet.requestId().getLeastSignificantBits())
                .setAnalyticsId(ByteString.copyFrom(packet.analyticId()))
                .build();
    }

    @Override
    public DataExportRequestData from(Protobuf.DataExportRequest packet) {
        return new DataExportRequestData(
                new UUID(packet.getRequestIdMostSig(), packet.getRequestIdLeastSig()),
                packet.getAnalyticsId().toByteArray()
        );
    }
}
