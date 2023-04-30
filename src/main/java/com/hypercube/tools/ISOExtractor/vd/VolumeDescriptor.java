package com.hypercube.tools.ISOExtractor.vd;

import com.hypercube.tools.ISOExtractor.cli.ExecutionContext;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class VolumeDescriptor {
    private final VolumeDescriptorTypes type;
    private final String id;
    private final int version;

    public static VolumeDescriptor parse(ExecutionContext ctx) {
        var b = ctx.getReader();
        var vdType = VolumeDescriptorTypes.from(b.readByte());
        String vdId = b.readASCIIString(5);
        int vdVersion = b.readByte();
        if (vdType == VolumeDescriptorTypes.PrimaryVolume) {
            return VdPrimary.parse(ctx, vdType, vdId, vdVersion);
        } else if (vdType == VolumeDescriptorTypes.SupplementaryVolume) {
            return VdSupplementary.parse(ctx, vdType, vdId, vdVersion);
        } else if (vdType == VolumeDescriptorTypes.BootRecord) {
            return VdBoot.childBuilder()
                    .id(vdId)
                    .type(vdType)
                    .version(vdVersion)
                    .bootSystemIdentifier(b.readASCIIString(0x20))
                    .bootIdentifier(b.readASCIIString(0x20))
                    .build();
        } else {
            return VolumeDescriptor.builder()
                    .id(vdId)
                    .type(vdType)
                    .version(vdVersion)
                    .build();
        }
    }
}