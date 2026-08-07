package org.omni.packets.data;

import java.util.UUID;

public record DataExportResultData(UUID requestId, String dataLink) {
}
