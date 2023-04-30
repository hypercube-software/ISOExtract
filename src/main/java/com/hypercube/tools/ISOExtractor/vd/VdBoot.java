package com.hypercube.tools.ISOExtractor.vd;

import lombok.Builder;
import lombok.Getter;

@Getter
public class VdBoot extends VolumeDescriptor {
    private final String bootSystemIdentifier;
    private final String bootIdentifier;

    @Builder(builderMethodName = "childBuilder")
    VdBoot(VolumeDescriptorTypes type, String id, int version, String bootSystemIdentifier, String bootIdentifier) {
        super(type, id, version);
        this.bootSystemIdentifier = bootSystemIdentifier;
        this.bootIdentifier = bootIdentifier;
    }
}
