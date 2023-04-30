package com.hypercube.tools.ISOExtractor.cli;

import com.hypercube.tools.ISOExtractor.pathtable.PathTable;
import com.hypercube.tools.ISOExtractor.utils.BinaryDataReader;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;


@Getter
@AllArgsConstructor
public class ExecutionContext {
    private final String targeDir;
    private final BinaryDataReader reader;

    @Setter
    private PathTable root;
}
