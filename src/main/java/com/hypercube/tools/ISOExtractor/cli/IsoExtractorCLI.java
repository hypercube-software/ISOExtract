package com.hypercube.tools.ISOExtractor.cli;

import com.hypercube.tools.ISOExtractor.directory.DirectoryRecord;
import com.hypercube.tools.ISOExtractor.pathtable.PathTable;
import com.hypercube.tools.ISOExtractor.utils.BinaryDataReader;
import com.hypercube.tools.ISOExtractor.vd.VdPrimary;
import com.hypercube.tools.ISOExtractor.vd.VdSupplementary;
import com.hypercube.tools.ISOExtractor.vd.VolumeDescriptor;
import com.hypercube.tools.ISOExtractor.vd.VolumeDescriptorTypes;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;


@ShellComponent
@Slf4j
@AllArgsConstructor
public class IsoExtractorCLI {
    public static final int SECTOR_SIZE = 2048;
    static final byte[] SYNC_HEADER = HexFormat.ofDelimiter(":")
            .parseHex("00:FF:FF:FF:FF:FF:FF:FF:FF:FF:FF:00");

    @ShellMethod(value = "Extract files from an ISO file")
    public void extract(@ShellOption(value = "-i") String isoFilename, @ShellOption(value = "-d") String targetDir) {
        try {
            File isoFile = new File(isoFilename);
            BinaryDataReader reader = new BinaryDataReader(isoFile);
            ExecutionContext ctx = new ExecutionContext(targetDir, reader, null);
            reader.order(ByteOrder.BIG_ENDIAN);
            // skip the System Area
            reader.seek(SECTOR_SIZE * 16);
            // parse the Data Area
            for (int s = 16; ; s++) {
                reader.seek(s * SECTOR_SIZE);
                VolumeDescriptor vd = VolumeDescriptor.parse(ctx);
                log.info(String.format("Sector %d %s %s", s, vd.getId(), vd.getType()
                        .name()));
                if (vd.getType() == VolumeDescriptorTypes.Terminator)
                    break;
                if (vd instanceof VdPrimary) {
                    var vdp = (VdPrimary) vd;
                    // The Primary volume uses short filenames, so it is irrelevant
                    /*
                    readPathTable(ctx, vdp.getPathTablePtr()
                            .getMainOffset() * SECTOR_SIZE, vdp.getPathTableSize()
                            .getValue(), false);
                    readPathTable(ctx, vdp.getPathTablePtr()
                            .getOptOffset() * SECTOR_SIZE, vdp.getPathTableSize()
                            .getValue(), false);*/
                } else if (vd instanceof VdSupplementary) {
                    var vdp = (VdSupplementary) vd;
                    // this is where directories are
                    readPathTable(ctx, vdp.getPathTablePtr()
                            .getMainOffset() * SECTOR_SIZE, vdp.getPathTableSize()
                            .getValue(), true).ifPresent(root -> ctx.setRoot(root));
                    // The Optional table should be empty
                    readPathTable(ctx, vdp.getPathTablePtr()
                            .getOptOffset() * SECTOR_SIZE, vdp.getPathTableSize()
                            .getValue(), true);
                }
            }
            // given the root entry found in the VdSuplementary, extract everything
            if (ctx.getRoot() != null) {
                recurseExtract(ctx, ctx.getRoot(), "");
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void recurseExtract(ExecutionContext ctx, PathTable p, String prefix) {
        var b = ctx.getReader();
        p.getFiles()
                .forEach(r ->
                {
                    int lba = r.getExtentOffset()
                            .getValue() * SECTOR_SIZE;

                    int dataSize = r.getDataSize()
                            .getValue();

                    File f = new File(ctx.getTargeDir() + "/" + p.getPath() + "/" + r.getFileName());
                    log.info(String.format("%s LBA: %8X SIZE: %10d %s", prefix, lba, dataSize, f.getPath()));
                    f.getParentFile()
                            .mkdirs();
                    byte[] data = new byte[dataSize];
                    try {
                        b.seek(lba);
                        b.readByte(data);
                        Files.write(f.toPath(), data, StandardOpenOption.CREATE);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
        p.getChildren()
                .forEach(c -> recurseExtract(ctx, c, prefix + "    "));
    }


    /**
     * The Path table contains only directories
     * Once parsed, we need to follow the LBA to found the files
     *
     * @param ctx           execution context
     * @param start         offset in bytes from the begining of the file
     * @param pathTableSize number of entries
     * @param unicode       do we have to read unicode strings or not
     */
    private Optional<PathTable> readPathTable(ExecutionContext ctx, int start, int pathTableSize, boolean unicode) {
        if (start == 0)
            return Optional.empty();
        var b = ctx.getReader();
        //log.info(String.format("%8X Path table --------------------", start));
        b.seek(start);
        List<PathTable> table = new ArrayList<>();
        for (; ; ) {
            PathTable pt = PathTable.parse(ctx, unicode);
            //log.info(String.format("index: %d parentIndex: %d LBA:%8X %s", table.size(), pt.getParentIndex(), pt.getLbaExtend(), pt.getDirectoryIdentifier()));
            table.add(pt);
            if (b.position() - start == pathTableSize)
                break;
        }

        // set the parent directory on each entry
        // the first one is the root
        table.stream()
                .filter(e -> e != table.get(0) && e.getParentIndex() <= table.size())
                .forEach(e -> {
                    e.setParent(table.get(e.getParentIndex()));
                });

        var root = table.get(0);

        collectDirectoryFiles(ctx, root, unicode);

        dumpTable(ctx, root, "");

        return Optional.of(table.get(0));
    }

    private void collectDirectoryFiles(ExecutionContext ctx, PathTable directory, boolean unicode) {
        ctx.getReader()
                .seek(directory.getLbaExtend());

        List<DirectoryRecord> records = new ArrayList<>();
        for (; ; ) {
            DirectoryRecord r = DirectoryRecord.parse(ctx, unicode);
            if (r.getRecordSize() == 0)
                break;
            if (!r.isDirectory()) {
                records.add(r);
            }
        }
        cleanupDirectoryEntries(records);
        directory.setFiles(records);

        // recurse
        directory.getChildren()
                .forEach(c -> collectDirectoryFiles(ctx, c, unicode));
    }

    /**
     * This is why I wrote this parser, some ISO contains duplicate entries with empty size
     * This method get rid of them
     *
     * @param records
     */
    private void cleanupDirectoryEntries(List<DirectoryRecord> records) {
        var nonZeroSize = records.stream()
                .filter(r -> r.getDataSize()
                        .getValue() != 0)
                .map(r -> r.getFileName())
                .collect(Collectors.toSet());
        var zeroSize = records.stream()
                .filter(r -> r.getDataSize()
                        .getValue() == 0)
                .filter(r -> nonZeroSize.contains(r.getFileName()))
                .toList();
        if (zeroSize.size() > 0) {
            zeroSize.forEach(f -> {
                //log.warn(String.format("Duplicated garbage files to ignore: %s", f.getFileName()));
                records.remove(f);
            });
        }
    }

    private static void dumpTable(ExecutionContext ctx, PathTable pathTable, String depth) {
        log.info(String.format("%s%s %8X", depth, pathTable.getDirectoryIdentifier(), pathTable.getLbaExtend()));
        pathTable.getChildren()
                .forEach(c -> dumpTable(ctx, c, depth + "   "));
        pathTable.getFiles()
                .forEach(f -> log.info(String.format("%s%8X %s ", depth, f.getExtentOffset()
                        .getValue() * SECTOR_SIZE, f.getFileName())));
    }


    /**
     * Parse a CUE/BIN image and extract the ISO from it
     *
     * @throws IOException
     * @see <a href="https://github.com/libyal/libodraw/blob/main/documentation/CUE%20sheet%20format.asciidoc"></a>
     * @see <a href="https://wiki.osdev.org/User:Combuster/CDRom_BS"></a>
     */
    private void parseCueBin() throws IOException {
        byte[] sync = new byte[12];

        ByteBuffer b = ByteBuffer.wrap(Files.readAllBytes(Path.of("./input/dlm-ytc.bin")));
        int padSize, eccSize, sectorSize;
        int dataSize = 2048;
        try (OutputStream out = new FileOutputStream("output.iso")) {

            boolean MAC = false;
            for (; ; ) {
                if (b.position() == b.capacity())
                    break;
                int sector_start = b.position();
                b.get(sync);
                int addr = b.get() << 16 | b.get() << 8 | b.get();
                int mode = b.get();
                log(sync);
                log.info("Mode: " + mode);
                if (mode == 1) {
                    eccSize = 288;
                    sectorSize = 2352; /* Mode 1 / 2352 */
                    padSize = 30;
                } else if (mode == 2) {
                    eccSize = 280;
                    sectorSize = 2352; /* Mode 2 / 2352 */
                    padSize = 30;
                } else {
                    throw new RuntimeException("Unexpected mode: " + mode);
                }

                byte[] pad = new byte[padSize];
                byte[] data = new byte[2048];
                b.get(data);
                out.write(data);
                byte[] ecc = new byte[eccSize];
                b.get(ecc);
            }
        }
    }

    private static void log(byte[] sync) {
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < 12; i++) {
            buf.append(String.format("%2X", sync[i]));
        }
        log.info(buf.toString());
    }
}
