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

import java.util.List;

/**
 * ECMA 119 chapter: 8.5 Supplementary Volume Descriptor and Enhanced Volume Descriptor
 */
@Getter
public class VdSupplementary extends VolumeDescriptor {
    private final String systemId;
    private final String volumeId;
    private final DoubleInt32 pathTableSize;
    private final PathTablePtr pathTablePtr;
    private final DirectoryRecord record;
    private final String applicationId;

    @Builder(builderMethodName = "childBuilder")
    VdSupplementary(VolumeDescriptorTypes type, String id, int version, String systemId, String volumeId, DoubleInt32 pathTableSize, PathTablePtr pathTablePtr, DirectoryRecord record, String applicationId) {
        super(type, id, version);
        this.systemId = systemId;
        this.volumeId = volumeId;
        this.pathTableSize = pathTableSize;
        this.pathTablePtr = pathTablePtr;

        this.record = record;
        this.applicationId = applicationId;
    }

    public static VdSupplementary parse(ExecutionContext ctx, VolumeDescriptorTypes type, String id, int version) {
        var b = ctx.getReader();
        int vdStart = b.position() - 7;
        Log.info(String.format("Supplementary volume descriptor found at 0x%8X", vdStart));
        List escapeSequences = List.of("%/@", "%/C", "%/E");
        int volFlags = b.readByte();
        String systemId = b.readUnicodeBE(0x20);
        String volumeId = b.readUnicodeBE(0x20);
        b.skip(8);
        DoubleInt32 volSpaceSize = b.readDoubleInt32();
        String escapeSequence = b.readASCIIString(3);
        if (!escapeSequences.contains(escapeSequence))
            throw new RuntimeException("Unexpected escape sequence: " + escapeSequence);
        b.skip(0x20 - 3);
        DoubleInt16 volSetSize = b.readDoubleInt16();
        DoubleInt16 volSeqNumber = b.readDoubleInt16();
        DoubleInt16 logicalBlockSize = b.readDoubleInt16();
        DoubleInt32 pathTableSize = b.readDoubleInt32();
        PathTablePtr pathTablePtr = b.readPathTablePtr();
        b.seek(vdStart + 157 - 1);
        DirectoryRecord record = DirectoryRecord.parse(ctx, false);
        String volSetId = b.readUnicodeBE(0x80);
        String publisherId = b.readUnicodeBE(0x80);
        String dataPreparerId = b.readUnicodeBE(0x80);
        String applicationId = b.readUnicodeBE(0x80);
        String copyrightFileId = b.readUnicodeBE(0x25);
        String AbstractFileId = b.readUnicodeBE(0x25);
        b.seek(vdStart + 814 - 1);
        ISODateFormat creationTime = b.readStringISODateFormat();
        ISODateFormat modificationTime = b.readStringISODateFormat();
        int fileStructVersion = b.readByte();
        return VdSupplementary.childBuilder()
                .type(type)
                .id(id)
                .version(version)
                .systemId(systemId)
                .volumeId("")
                .pathTablePtr(pathTablePtr)
                .pathTableSize(pathTableSize)
                .record(record)
                .applicationId(applicationId)
                .build();
    }
}
