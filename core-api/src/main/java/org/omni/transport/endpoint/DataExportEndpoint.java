package org.omni.transport.endpoint;

import org.omni.packets.data.DataExportResultData;
import org.omni.transport.EndpointHandler;

import java.util.UUID;

public interface DataExportEndpoint extends EndpointHandler<DataExportResultData, UUID, String> {
}
