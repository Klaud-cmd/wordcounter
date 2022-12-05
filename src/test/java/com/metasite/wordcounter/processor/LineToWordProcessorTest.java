package com.metasite.wordcounter.processor;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LineToWordProcessorTest {

    private LineToWordProcessor lineToWordProcessor = new LineToWordProcessor();

    @Test
    void shouldIgnoreWhitespaceAsWords() {
        //Given
        String line = "Lorem ipsum dolor sit amet";
        String[] expected = new String[]{"Lorem","ipsum","dolor","sit","amet"};

        //When
        String[] actual = lineToWordProcessor.process(line);

        //Then
        assertArrayEquals(expected, actual);
    }
}