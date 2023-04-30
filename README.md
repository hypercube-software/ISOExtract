# ISO Extractor

A simple native CLI to extract files from an ISO file, powered by [Spring native](https://docs.spring.io/spring-boot/docs/current/reference/html/native-image.html) and [Spring Boot 3](https://spring.io/blog/2022/05/24/preparing-for-spring-boot-3-0).

**Why this tool ?**

I recently found a damaged Hybrid ISO file containing **duplicated entries**: one file A with a 0 size and
another A with the right size. I found no tools able to handle this kind of damaged ISO, so I wrote one.

- ISO Extractor will extract the right file
- ISO Extract rely on the Joliet format to extract long filenames
- It loads the entire ISO in memory and parse it with [ByteBuffer](https://www.baeldung.com/java-bytebuffer)

# Requirements

- [GraalVM 17](https://www.graalvm.org/downloads/)
- [Visual Studio Community Edition](https://visualstudio.microsoft.com/downloads/)

# Build

Edit `build-native.cmd` to point to the right JDK, then run:

```bash
build-native.cmd
```

# Run

```bash
target\ISOExtractor.exe extract -i "input.iso" -d output
```

# About ISO 9660

## The spec

You can get the original specification for 187€ [here](https://www.iso.org/standard/81979.html).

- There is a free alternate version at ECMA
  called [ECMA-119](https://www.ecma-international.org/publications-and-standards/standards/ecma-119/), the Annex C
  contains the Joliet extension.
- Wiki provides good explanations [here](https://wiki.osdev.org/ISO_9660).
- The Joliet extension (for long file names) can be found [here](http://www.buildorbuy.org/pdf/joliet.pdf).

## How it works ?

Basically... it is complicated. I hardly recommend [ImHex](https://github.com/WerWolv/ImHex) to inspect your ISO file.

- You have a bunch of sectors of size 2048 bytes and various [LBA](https://en.wikipedia.org/wiki/Logical_block_addressing) pointers. So given a LBA of 12, you must go to the offset 12*2048 in the file.
- The format contains here and there both little endian and big endian pointers
- The file contains a section called `Primary Volume Descriptor` where short filenames are used
- The file also contains a section called `Supplementary Volume Descriptor` where long filenames are used (**UTF_16BE**). It is the Joliet extension.

## How to read the spec

**💀 The spec is awful to read 💀**. It's like they don't want you to implement it.

- The PDF does not contain any bookmarks. Thank you.
- The table 6 in chapter 8.5 contains misaligned columns.
- Byte offsets are in the range [1,n] instead of [0,n-1]
- Byte offsets are in decimal instead of hexadecimal
- Most of the time there is no **size** for each field only **start** and **end** offsets. Seriously ?
- Bytes offsets are relative to the current descriptor, but the spec use an absolute terminology: byte position (BP)
- RBP replaces BP when it is about offsets inside a field.
- Escape sequences talk about character `0x2F` as `\` which is in fact `/`.
- Instead of saying "it is in little endian" the spec says "This field shall be recorded according to 7.3.1"
- Instead of saying "it is in big endian" the spec says "This field shall be recorded according to 7.3.2"

Navigate in the spec:

- Open the **ECMA-119** PDF and look for `BP 1` you will found the beginning of each sections.
- The section `8.4 Primary Volume Descriptor`  contains what we want. It describes the structure of the data.
- The section `8.5 Supplementary Volume Descriptor` contains the equivalent for the Joliet entries.
- When you read `BP 41-72` it means byte offset `40`, size `32` bytes.

## Strings

- When you deal with the Joliet section, **some** strings are in **UTF_16BE** but their size remains in bytes. This is
  crazy.
- Other strings are in ASCII, for instance the volume id which is always `CD001`
- In the **ECMA 119** spec, when you read `a/d-characters` it means ASCII, whereas `a1/d1-characters`means UTF-16BE
- They don't say `ASCII`, but `ECMA-6`. The characters table is in Annex A .
- Because the ISO spec was written before `Joliet`, they don't say `UTF-16BE`,
  but `subject to agreement between the originator and the recipient of the volume.`

So if the spec says there is a string of 32 characters, in the Joliet section it becomes a string of 32 bytes, which is
in fact 16 characters !

`APPLE COMPUTER, ` will
be `00 41 00 50 00 50 00 4C 00 45 00 20 00 43 00 4F 00 4D 00 50 00 55 00 54 00 45 00 52 00 2C 00 20`

## Extracting files

Basically, we must:

- Ignore the `Primary Colume Descriptor` where ASCII filenames are used.
- Parse the `Supplementary Volume Descriptor` where Unicode filenames are used.
- Parse the `PathTable` where all directories are.
- Reconstruct the tree of directories and collect the file entries for each of them.
- Finally extract the files given their LBA offset.



