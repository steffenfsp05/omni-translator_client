package org.pytenix.socket.consumer;

import com.google.inject.Inject;
import org.omni.packets.MappedPacketReceiveConsumer;
import org.omni.packets.data.DataExportResultData;
import org.omni.proto.generated.Protobuf;
import org.omni.transport.endpoint.DataExportEndpoint;
import org.transport.service.PacketContext;

import java.net.http.WebSocket;

public class BackendDataExportResultConsumer extends MappedPacketReceiveConsumer<WebSocket, Protobuf.DataExportResult, DataExportResultData> {

    private final DataExportEndpoint dataExportEndpoint;

    @Inject
    public BackendDataExportResultConsumer(DataExportEndpoint dataExportEndpoint) {
        this.dataExportEndpoint = dataExportEndpoint;
    }

    @Override
    public void handle(PacketContext<WebSocket> context, DataExportResultData javaPacket) {
        dataExportEndpoint.handleIncoming(javaPacket);
    }}
