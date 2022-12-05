package com.metasite.wordcounter.processor;

import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.AfterStep;
import org.springframework.batch.item.ItemProcessor;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class WordCountingProcessor implements ItemProcessor<String[], Map<String, BigInteger>> {

    private final Map<String, BigInteger> wordMap = new ConcurrentHashMap<>();

    @Override
    public Map<String, BigInteger> process(String[] words) {
        Arrays.stream(words).forEach(word -> {
            wordMap.merge(word, BigInteger.ONE, BigInteger::add);
        });
        return null; //No need to pass anything to the writer
    }

    @AfterStep
    public void afterStep(StepExecution stepExecution) {
        //Put into stepExecutionContext to be promoted to the JobExecutionContext
        stepExecution.getExecutionContext().put("wordMap", wordMap);
    }
}
