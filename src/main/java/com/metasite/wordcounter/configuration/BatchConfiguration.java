package com.metasite.wordcounter.configuration;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.metasite.wordcounter.partitioner.CustomFilePartitioner;
import com.metasite.wordcounter.processor.LineFilterProcessor;
import com.metasite.wordcounter.processor.LineToWordProcessor;
import com.metasite.wordcounter.processor.WordCountingProcessor;
import com.metasite.wordcounter.tasklet.WordCountWriterTasklet;
import com.metasite.wordcounter.util.NoOpItemWriter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.listener.ExecutionContextPromotionListener;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.mapping.PassThroughFieldSetMapper;
import org.springframework.batch.item.file.mapping.PassThroughLineMapper;
import org.springframework.batch.item.support.CompositeItemProcessor;
import org.springframework.batch.item.support.SynchronizedItemStreamReader;
import org.springframework.batch.item.support.builder.CompositeItemProcessorBuilder;
import org.springframework.batch.item.support.builder.SynchronizedItemStreamReaderBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.task.SimpleAsyncTaskExecutor;

import java.math.BigInteger;
import java.util.Map;

@Configuration
@EnableBatchProcessing
@Slf4j
public class BatchConfiguration {

    @Value("${bucket.inputPrefix}")
    public String INPUT_DIRECTORY;

    @Value("${bucket.name}")
    public String BUCKET_NAME;

    private ResourceLoader resourceLoader;

    @Autowired
    public JobBuilderFactory jobBuilderFactory;

    @Autowired
    public AmazonS3 s3;

    @Autowired
    public StepBuilderFactory stepBuilderFactory;

    @Bean
    @StepScope
    public SynchronizedItemStreamReader<String> reader(@Value("#{stepExecutionContext[inputFilename]}") String filename) {
        S3Object s3object = s3.getObject(BUCKET_NAME, filename);
        S3ObjectInputStream inputStream = s3object.getObjectContent();
        FlatFileItemReader<String> reader = new FlatFileItemReaderBuilder()
                .name("reader")
                .resource(new InputStreamResource(inputStream))
                .lineMapper(new PassThroughLineMapper())
                .fieldSetMapper(new PassThroughFieldSetMapper())
                .build();
        return new SynchronizedItemStreamReaderBuilder<String>().delegate(reader).build();
    }

    @Bean
    public Job countWordsJob() {
        return jobBuilderFactory
                .get("Calculate word count of *.txt files")
                .incrementer(new RunIdIncrementer())
                .start(partitionStep())
                .next(writeResultsToFiles())
                .build();
    }

    @Bean
    public Step partitionStep() {
        return stepBuilderFactory
                .get("Partition by file.manager")
                .partitioner("Partition by file", partitioner(null))
                .step(filterNumbersAndSpecialCharactersStep())
                .taskExecutor(new SimpleAsyncTaskExecutor())
                .build();
    }

    @Bean
    @StepScope
    public CustomFilePartitioner partitioner(@Value("#{jobParameters['requestID']}") String requestID) {
        CustomFilePartitioner partitioner = new CustomFilePartitioner();
        log.info("bucket {} directory {} request id {}", BUCKET_NAME, INPUT_DIRECTORY, requestID);
        ObjectListing objectListing = s3.listObjects(BUCKET_NAME, INPUT_DIRECTORY + requestID);
        partitioner.setListOfFiles(objectListing);
        return partitioner;
    }

    @Bean
    public Step filterNumbersAndSpecialCharactersStep() {
        return stepBuilderFactory
                .get("Filter numbers and special characters")
                .listener(promotionListener())
                .listener(wordCountingProcessor())
                .chunk(1000)
                .reader(reader(null))
                .processor(processor())
                .writer(new NoOpItemWriter())
                .build();
    }

    @Bean
    public Step writeResultsToFiles() {
        return stepBuilderFactory
                .get("Write results to files")
                .listener(wordCountWriterTasklet())
                .tasklet(wordCountWriterTasklet())
                .build();
    }

    @Bean
    public WordCountWriterTasklet wordCountWriterTasklet() {
        return new WordCountWriterTasklet();
    }

    @Bean
    public LineFilterProcessor lineFilterProcessor() {
        return new LineFilterProcessor();
    }

    @Bean
    public LineToWordProcessor lineToWordProcessor() {
        return new LineToWordProcessor();
    }

    @Bean
    public WordCountingProcessor wordCountingProcessor() {
        return new WordCountingProcessor();
    }

    @Bean
    public CompositeItemProcessor processor() {
        return new CompositeItemProcessorBuilder<String, Map<String, BigInteger>>()
                .delegates(
                        lineFilterProcessor(),
                        lineToWordProcessor(),
                        wordCountingProcessor()
                )
                .build();
    }

    @Bean
    public ExecutionContextPromotionListener promotionListener() {
        ExecutionContextPromotionListener listener = new ExecutionContextPromotionListener();
        listener.setKeys(new String[]{"wordMap"});
        return listener;
    }
}
