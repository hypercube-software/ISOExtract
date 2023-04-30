package com.hypercube.tools.ISOExtractor.vd;

import com.hypercube.tools.ISOExtractor.cli.ExecutionContext;
import com.hypercube.tools.ISOExtractor.date.ISODateFormat;
import com.hypercube.tools.ISOExtractor.directory.DirectoryRecord;
import com.hypercube.tools.ISOExtractor.pathtable.PathTablePtr;
import com.hypercube.tools.ISOExtractor.utils.DoubleInt16;
import com.hypercube.tools.ISOExtractor.utils.DoubleInt32;
import lombok.Builder;
import lombok.Getter;
import org.jline.utils.Log;

/**
 * ECMA 119 chapter: 8.4 Primary Volume Descriptor
 */
@Getter
public class VdPrimary extends VolumeDescriptor {
    private final String systemId;
    private final String volumeId;
    private final DoubleInt32 pathTableSize;
    private final PathTablePtr pathTablePtr;
    private final DirectoryRecord record;
    private final String applicationId;

    @Builder(builderMethodName = "childBuilder")
    VdPrimary(VolumeDescriptorTypes type, String id, int version, String systemId, String volumeId, DoubleInt32 pathTableSize, PathTablePtr pathTablePtr, DirectoryRecord record, String applicationId) {
        super(type, id, version);
        this.systemId = systemId;
        this.volumeId = volumeId;
        this.pathTableSize = pathTableSize;
        this.pathTablePtr = pathTablePtr;

        this.record = record;
        this.applicationId = applicationId;
    }

    public static VdPrimary parse(ExecutionContext ctx, VolumeDescriptorTypes type, String id, int version) {
        var b = ctx.getReader();
        int vdStart = b.position() - 7;
        Log.info(String.format("Primary volume descriptor found at 0x%8X", vdStart));
        b.skip(1); // padding
        String systemId = b.readASCIIString(0x20);
        String volumeId = b.readASCIIString(0x20);
        b.skip(8);
        DoubleInt32 spaceSize = b.readDoubleInt32();
        b.skip(0x20);
        DoubleInt16 setSize = b.readDoubleInt16();
        DoubleInt16 sequenceNumber = b.readDoubleInt16();
        DoubleInt16 logicalBlockSize = b.readDoubleInt16();
        DoubleInt32 pathTableSize = b.readDoubleInt32();
        PathTablePtr pathTablePtr = b.readPathTablePtr();
        DirectoryRecord record = DirectoryRecord.parse(ctx, false);
        String setId = b.readASCIIString(0x80);
        String publisherId = b.readASCIIString(0x80);
        String preparerId = b.readASCIIString(0x80);
        String applicationId = b.readASCIIString(0x80);
        String copyrightFileId = b.readASCIIString(0x25);
        String abstractFileId = b.readASCIIString(0x25);
        String bibliographicFileId = b.readASCIIString(0x25);
        ISODateFormat creationTime = b.readStringISODateFormat();
        ISODateFormat modificationTime = b.readStringISODateFormat();
        ISODateFormat expirationTime = b.readStringISODateFormat();
        ISODateFormat effectiveTime = b.readStringISODateFormat();
        int fileStructVersion = b.readByte();
        b.skip(0x200 + 0x28E);
        return VdPrimary.childBuilder()
                .type(type)
                .id(id)
                .version(version)
                .systemId(systemId)
                .volumeId(volumeId)
                .pathTablePtr(pathTablePtr)
                .pathTableSize(pathTableSize)
                .record(record)
                .applicationId(applicationId)
                .build();
    }
}
