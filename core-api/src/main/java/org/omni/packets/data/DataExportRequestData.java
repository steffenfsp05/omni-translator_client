package org.omni.packets.data;

import java.util.UUID;

public record DataExportRequestData(UUID requestId,
                                    byte[] analyticId) {

}
