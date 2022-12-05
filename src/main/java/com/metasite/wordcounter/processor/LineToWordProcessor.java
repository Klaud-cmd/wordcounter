package com.metasite.wordcounter.processor;

import org.springframework.batch.item.ItemProcessor;

public class LineToWordProcessor implements ItemProcessor<String, String[]> {

    @Override
    public String[] process(String line) {
        return line.split("\\s+");
    }
}
