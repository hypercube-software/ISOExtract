package com.hypercube.tools.ISOExtractor.utils;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class DoubleInt32 {
    private final int leValue;
    private final int beValue;

    public int getValue() {
        return leValue != 0 ? leValue : beValue;
    }
}
