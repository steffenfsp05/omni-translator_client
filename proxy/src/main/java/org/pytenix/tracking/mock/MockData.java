package org.pytenix.tracking.mock;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class MockData {

    public static final List<UUID> HARDCODED_UUIDS;

    static {
        List<UUID> list = new ArrayList<>(50000);

        for (int i = 0; i < 50000; i++) {
            list.add(UUID.nameUUIDFromBytes(("User-" + i).getBytes()));
        }

        HARDCODED_UUIDS = Collections.unmodifiableList(list);
    }
}