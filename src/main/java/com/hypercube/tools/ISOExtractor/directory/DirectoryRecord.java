package com.hypercube.tools.ISOExtractor.directory;

import com.hypercube.tools.ISOExtractor.cli.ExecutionContext;
import com.hypercube.tools.ISOExtractor.date.ISODateFormat;
import com.hypercube.tools.ISOExtractor.utils.DoubleInt16;
import com.hypercube.tools.ISOExtractor.utils.DoubleInt32;
import lombok.Builder;
import lombok.Getter;

/**
 * ECMA 119, chapter: 9.1 Format of a Directory Record
 */
@Builder
@Getter
public class DirectoryRecord {
    private final int recordSize;
    private final int extendedRecordSize;

    private final DoubleInt32 extentOffset;
    private final DoubleInt32 dataSize;

    private final ISODateFormat recordDate;
    private final int fileFlags;
    private final int fileUnitSize;
    private final int interleaveGapSize;
    private final DoubleInt16 volumeSequenceNumber;
    private int fileNameLen;
    private String fileName;

    public boolean isDirectory() {
        return (fileFlags & (1 << 1)) != 0;
    }

    public boolean isHidden() {
        return (fileFlags & (1 << 0)) != 0;
    }

    public boolean isAssociatedFile() {
        return (fileFlags & (1 << 2)) != 0;
    }

    public boolean isExtendedAttribute() {
        return (fileFlags & (1 << 3)) != 0;
    }

    public boolean isOwnerAndGroupInExtendedAttribute() {
        return (fileFlags & (1 << 4)) != 0;
    }

    public boolean isFileRecordNotFinal() {
        return (fileFlags & (1 << 7)) != 0;
    }

    public static DirectoryRecord parse(ExecutionContext ctx, boolean unicode) {
        var b = ctx.getReader();
        int pos = b.position();
        int recordSize = b.readByte(); // Typically 0x22
        if (recordSize < 0)
            throw new RuntimeException("Unexpected record size: " + recordSize);
        int extendedRecordSize = b.readByte();
        DoubleInt32 extendedOffset = b.readDoubleInt32();
        DoubleInt32 dataSize = b.readDoubleInt32();
        ISODateFormat recordDate = b.readISODateFormat();
        int fileFlags = b.readByte();
        int fileUnitSize = b.readByte();
        int interleaveGapSize = b.readByte();
        DoubleInt16 volumeSequenceNumber = b.readDoubleInt16();
        int fileNameLen = b.readByte();
        String fileName = b.readString(fileNameLen, unicode);
        //Log.info(String.format("%8X DirectoryRecord: %s", pos, fileName));
        DirectoryRecord r = DirectoryRecord.builder()
                .recordSize(recordSize)
                .extendedRecordSize(extendedRecordSize)
                .extentOffset(extendedOffset)
                .dataSize(dataSize)
                .recordDate(recordDate)
                .fileFlags(fileFlags)
                .fileUnitSize(fileUnitSize)
                .interleaveGapSize(interleaveGapSize)
                .volumeSequenceNumber(volumeSequenceNumber)
                .fileNameLen(fileNameLen)
                .fileName(fileName)
                .build();
        b.seek(pos + recordSize);
        return r;
    }
}
