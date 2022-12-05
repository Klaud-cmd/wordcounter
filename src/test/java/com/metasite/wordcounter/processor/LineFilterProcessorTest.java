package com.metasite.wordcounter.processor;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LineFilterProcessorTest {

    private LineFilterProcessor lineFilterProcessor = new LineFilterProcessor();

    @Test
    void removesNumbersAndSpecialCharacters() {
        //Given
        String line = "1) \"LOREM IPSUM DOLOR SIT AMET\"";
        String expected = "LOREM IPSUM DOLOR SIT AMET";

        //When
        String actual = lineFilterProcessor.process(line);

        //Then
        assertEquals(expected, actual);
    }

    @Test
    void uppercasesAllWords() {
        //Given
        String line = "lorem ipsum dolor sit amet";
        String expected = "LOREM IPSUM DOLOR SIT AMET";

        //When
        String actual = lineFilterProcessor.process(line);

        //Then
        assertEquals(expected, actual);
    }
}