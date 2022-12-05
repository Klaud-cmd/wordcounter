package com.metasite.wordcounter.util;

import org.springframework.batch.item.ItemWriter;

import java.util.List;

//Spring Batch requires to specify a writer, however we don't need an ItemWriter for the chunk-based processing we do
public class NoOpItemWriter implements ItemWriter {

  @Override
  public void write(List items){
    //no-op
  }
}