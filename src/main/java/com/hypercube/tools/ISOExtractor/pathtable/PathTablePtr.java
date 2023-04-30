package com.hypercube.tools.ISOExtractor.pathtable;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class PathTablePtr {
    private final int leMainOffset;
    private final int leOptOffset;
    private final int beMainOffset;
    private final int beOptOffset;

    public int getMainOffset() {
        return leMainOffset != 0 ? leMainOffset : beMainOffset;
    }

    public int getOptOffset() {
        return leOptOffset != 0 ? leOptOffset : beOptOffset;
    }
}
