package org.omni.packets.impl;

import org.omni.packets.AbstractPacketMapper;
import org.omni.packets.data.DataExportResultData;
import org.omni.proto.generated.Protobuf;

import java.util.UUID;

public class DataExportResultMapper extends AbstractPacketMapper<Protobuf.DataExportResult, DataExportResultData> {
    public DataExportResultMapper() {
        super(Protobuf.DataExportResult.class, DataExportResultData.class);
    }

    @Override
    public Protobuf.DataExportResult to(DataExportResultData packet) {
        return Protobuf.DataExportResult.newBuilder()
                .setRequestIdMostSig(packet.requestId().getMostSignificantBits())
                .setRequestIdLeastSig(packet.requestId().getLeastSignificantBits())
                .setDataLink(packet.dataLink())
                .build();
    }

    @Override
    public DataExportResultData from(Protobuf.DataExportResult packet) {
        return new DataExportResultData(
                new UUID(packet.getRequestIdMostSig(), packet.getRequestIdLeastSig()),
                packet.getDataLink()
        );
    }
}
