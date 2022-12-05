package com.metasite.wordcounter.tasklet;

import com.amazonaws.services.s3.AmazonS3;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigInteger;
import java.util.Map;

@Slf4j
public class WordCountWriterTasklet implements Tasklet {


    @Value("${bucket.outputPrefix}")
    public String OUTPUT_DIRECTORY;

    @Value("${bucket.name}")
    public String BUCKET_NAME;

    @Autowired
    private AmazonS3 s3;

    private String requestId;

    private Map<String, BigInteger> wordMap;
    private Map<String, File> files;
    private Map<String, FileWriter> writers;

    @BeforeStep
    public void beforeStep(StepExecution stepExecution) {
        ExecutionContext executionContext = stepExecution
                .getJobExecution()
                .getExecutionContext();
        requestId = stepExecution.getJobParameters().getParameters().get("requestID").toString();
        this.wordMap = (Map<String, BigInteger>) executionContext.get("wordMap");
        try {
            files = Map.of(
                    "A-G", new File("A-G_output_" + requestId + ".csv"),
                    "H-N", new File("H-N_output_" + requestId + ".csv"),
                    "O-U", new File("O-U_output_" + requestId + ".csv"),
                    "V-Z", new File("V-Z_output_" + requestId + ".csv")
            );
            writers = Map.of(
                    "A-G", new FileWriter(files.get("A-G")),
                    "H-N", new FileWriter(files.get("H-N")),
                    "O-U", new FileWriter(files.get("O-U")),
                    "V-Z", new FileWriter(files.get("V-Z"))
            );
        } catch (IOException e) {
            throw new RuntimeException("Failed to open output file or writer", e);
        }


    }

    @SneakyThrows
    @Override
    public RepeatStatus execute(StepContribution stepContribution, ChunkContext chunkContext) {
        if (this.wordMap == null || this.wordMap.isEmpty()) {
            throw new RuntimeException("No data available");
        }

        this.wordMap.entrySet().stream()
                .sorted(Map.Entry.<String, BigInteger>comparingByValue().reversed())
                .forEachOrdered(this::writeToApproriateFile);
        writers.forEach((key, writer) -> {
            try {
                writer.close();
            } catch (IOException e) {
                throw new RuntimeException("Result failed to be written to local storage", e);
            }
        });
        files.forEach((key, file) -> {
            s3.putObject(BUCKET_NAME, OUTPUT_DIRECTORY + requestId + "/" + file.getName(), file);
            if (!file.delete()) {
                log.error("failed to delete local file {}", file.getName());
            }
        });

        return RepeatStatus.FINISHED;
    }

    void writeToApproriateFile(Map.Entry<String, BigInteger> entry) {
        char initial = entry.getKey().charAt(0);
        try {

            if (initial >= 'A' && initial <= 'G') {
                writers.get("A-G").write(entryToCSV(entry));
            } else if (initial >= 'H' && initial <= 'N') {
                writers.get("H-N").write(entryToCSV(entry));
            } else if (initial >= 'O' && initial <= 'U') {
                writers.get("O-U").write(entryToCSV(entry));
            } else {
                writers.get("V-Z").write(entryToCSV(entry));
            }
        } catch (IOException e) {
            log.error("failed to write {} to output file", entry.getKey());
        }
    }

    String entryToCSV(Map.Entry<String, BigInteger> entry) {
        return entry.getKey() + "," + entry.getValue() + "\n";
    }
}
