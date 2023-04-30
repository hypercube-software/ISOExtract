package com.hypercube.tools.ISOExtractor.vd;

public enum VolumeDescriptorTypes {
    BootRecord(0),
    PrimaryVolume(1),
    SupplementaryVolume(2),
    VolumePartition(3),
    Terminator(0xff);

    private final int ordinal;

    VolumeDescriptorTypes(int ordinal) {
        this.ordinal = ordinal;
    }

    public static VolumeDescriptorTypes from(int b) {
        return switch (b) {
            case 0 -> BootRecord;
            case 1 -> PrimaryVolume;
            case 2 -> SupplementaryVolume;
            case 3 -> VolumePartition;
            case -1 -> Terminator;
            default -> throw new RuntimeException("Unexpected type: " + b);
        };
    }
}
