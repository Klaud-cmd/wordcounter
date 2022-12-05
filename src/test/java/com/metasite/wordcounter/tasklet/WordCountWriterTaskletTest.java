package com.metasite.wordcounter.tasklet;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.util.Map;

import static java.math.BigInteger.ONE;
import static org.junit.jupiter.api.Assertions.assertEquals;

class WordCountWriterTaskletTest {

    private WordCountWriterTasklet tasklet;

    @BeforeEach
    void setup(){
        tasklet = new WordCountWriterTasklet();
    }

    @Test
    void entryToCSV() {
        //Given
        Map<String, BigInteger> input = Map.of("test", ONE);
        String expected = "test,1\n";

        //When
        String actual = tasklet.entryToCSV(input.entrySet().iterator().next());

        //Then
        assertEquals(expected, actual);
    }
}