package com.metasite.wordcounter.processor;

import org.springframework.batch.item.ItemProcessor;


public class LineFilterProcessor implements ItemProcessor<String, String> {

    @Override
    public String process(String line) {
        if (line.isBlank()) {
            return null;
        }
        line = line.replaceAll("[^\\p{Alpha}\\s]", "").trim().toUpperCase();
        return line;
    }
}
