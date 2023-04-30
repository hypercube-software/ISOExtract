package com.hypercube.tools.ISOExtractor.pathtable;

import com.hypercube.tools.ISOExtractor.cli.ExecutionContext;
import com.hypercube.tools.ISOExtractor.cli.IsoExtractorCLI;
import com.hypercube.tools.ISOExtractor.directory.DirectoryRecord;
import lombok.Builder;
import lombok.Getter;

import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

@Builder
@Getter
public class PathTable {
    private final int szDirectoryIdentifier;
    private final int extendedAttribute;
    private final int lbaExtend;
    private final int parentIndex;
    private final String directoryIdentifier;

    private PathTable parent;
    private final List<PathTable> children;


    private List<DirectoryRecord> files;

    public void setParent(PathTable parent) {
        this.parent = parent;
        parent.getChildren()
                .add(this);
    }

    public String getPath() {
        StringBuilder result = new StringBuilder();
        PathTable p = this;
        while (p != null) {
            if (result.length() > 0)
                result.insert(0, "/");
            result.insert(0, p.getDirectoryIdentifier());
            p = p.getParent();
        }
        return result.toString();
    }

    public static PathTable parse(ExecutionContext ctx, boolean unicode) {
        var b = ctx.getReader();
        int pos = b.position();
        b.order(ByteOrder.LITTLE_ENDIAN);
        int filenameSizeInBytes = b.readByte();
        int extendedAttributeRecordLength = b.readByte();
        int lbaExt = b.readInt32() * IsoExtractorCLI.SECTOR_SIZE; // convert the lba to bytes offsets
        int dirIndex = b.readInt16() - 1; // convert the index in the range [0,n-1]
        String name = b.readString(filenameSizeInBytes, unicode);
        PathTable r = PathTable.builder()
                .szDirectoryIdentifier(filenameSizeInBytes)
                .extendedAttribute(extendedAttributeRecordLength)
                .lbaExtend(lbaExt)
                .parentIndex(dirIndex)
                .directoryIdentifier(name)
                .children(new ArrayList<>())
                .parent(null)
                .parent(null)
                .build();
        int pad = (b.position() - pos) % 2;
        b.skip(pad);
        b.order(ByteOrder.BIG_ENDIAN);
        return r;
    }

    public List<DirectoryRecord> getFiles() {
        return files;
    }

    public void setFiles(List<DirectoryRecord> files) {
        this.files = files;
    }
}
