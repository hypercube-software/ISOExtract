package com.hypercube.tools.ISOExtractor.utils;

import com.hypercube.tools.ISOExtractor.date.ISODateFormat;
import com.hypercube.tools.ISOExtractor.pathtable.PathTablePtr;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * This reader load the entire ISO in memory
 * If you don't like that, fell free to use java.io.RandomAccessFile
 */
public class BinaryDataReader {
    private final ByteBuffer b;

    private final File isoFile;


    public BinaryDataReader(File isoFile) throws IOException {
        this.isoFile = isoFile;
        b = ByteBuffer.wrap(Files.readAllBytes(isoFile.toPath()));
    }

    public void seek(int offset) {
        b.position(offset);
    }

    public void skip(int offset) {
        b.position(b.position() + offset);
    }

    public void order(ByteOrder order) {
        b.order(order);
    }

    public int position() {
        return b.position();
    }

    public int readByte() {
        return b.get();
    }

    public DoubleInt32 readDoubleInt32() {
        b.order(ByteOrder.LITTLE_ENDIAN);
        int leValue = b.getInt();
        b.order(ByteOrder.BIG_ENDIAN);
        int beValue = b.getInt();
        return new DoubleInt32(leValue, beValue);
    }

    public ISODateFormat readISODateFormat() {
        return ISODateFormat.builder()
                .year(b.get() + 1900)
                .month(b.get())
                .day(b.get())
                .hour(b.get())
                .minute(b.get())
                .second(b.get())
                .secondFrac(0)
                .gmtOffset(b.get())
                .build();
    }

    public ISODateFormat readStringISODateFormat() {
        return ISODateFormat.builder()
                .year(readIntASCII(4))
                .month(readIntASCII(2))
                .day(readIntASCII(2))
                .hour(readIntASCII(2))
                .minute(readIntASCII(2))
                .second(readIntASCII(2))
                .secondFrac(readIntASCII(2))
                .gmtOffset(b.get())
                .build();
    }

    public PathTablePtr readPathTablePtr() {
        b.order(ByteOrder.LITTLE_ENDIAN);
        int leMainOffset = b.getInt();
        int leOptOffset = b.getInt();
        b.order(ByteOrder.BIG_ENDIAN);
        int beMainOffset = b.getInt();
        int beOptOffset = b.getInt();
        return PathTablePtr.builder()
                .leMainOffset(leMainOffset)
                .leOptOffset(leOptOffset)
                .beMainOffset(beMainOffset)
                .beOptOffset(beOptOffset)
                .build();
    }

    public DoubleInt16 readDoubleInt16() {
        b.order(ByteOrder.LITTLE_ENDIAN);
        short leValue = b.getShort();
        b.order(ByteOrder.BIG_ENDIAN);
        short beValue = b.getShort();
        return new DoubleInt16(leValue, beValue);
    }

    public void readByte(byte[] data) {
        b.get(data);
    }

    public String readString(int sizeInBytes, boolean unicode) {
        if (unicode)
            return readUnicodeBE(sizeInBytes);
        else
            return readASCIIString(sizeInBytes);
    }

    public String readASCIIString(int sizeInBytes) {
        if (sizeInBytes == 0)
            return "";
        byte[] data = new byte[sizeInBytes];
        b.get(data);
        if (data[0] == 0)
            return "";
        return new String(data, StandardCharsets.US_ASCII);
    }

    public String readUnicodeBE(int sizeInBytes) {
        if (sizeInBytes == 0) {
            return "";
        }
        if (sizeInBytes == 1) {
            b.get();
            return "";
        }
        byte[] data = new byte[sizeInBytes];
        b.get(data);
        if (data[0] == 0 && data[1] == 0)
            return "";
        return new String(data, StandardCharsets.UTF_16BE);
    }

    public int readIntASCII(int sizeInBytes) {
        String v = readASCIIString(sizeInBytes);
        if (v.length() == 0)
            return 0;
        else
            return Integer.parseInt(v);
    }

    public int readInt32() {
        return b.getInt();
    }

    public short readInt16() {
        return b.getShort();
    }
}
