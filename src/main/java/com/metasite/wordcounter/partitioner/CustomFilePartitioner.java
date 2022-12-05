package com.metasite.wordcounter.partitioner;

import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import lombok.Setter;
import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.item.ExecutionContext;

import java.util.HashMap;
import java.util.Map;

public class CustomFilePartitioner implements Partitioner {

    @Setter
    private ObjectListing listOfFiles;

    @Override
    public Map<String, ExecutionContext> partition(int gridSize) {
        Map<String, ExecutionContext> map = new HashMap<>(gridSize);
        for (S3ObjectSummary objectSummary : listOfFiles.getObjectSummaries()) {
            ExecutionContext context = new ExecutionContext();
            context.putString("inputFilename", objectSummary.getKey());
            map.put(objectSummary.getKey(), context);
        }
        return map;
    }
}