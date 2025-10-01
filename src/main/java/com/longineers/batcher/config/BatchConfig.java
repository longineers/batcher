package com.longineers.batcher.config;

import java.time.LocalDateTime;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.support.CompositeItemProcessor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.batch.item.database.JpaItemWriter;
import org.springframework.batch.item.database.builder.JpaItemWriterBuilder;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.transaction.PlatformTransactionManager;

import com.longineers.batcher.model.Product;

import jakarta.persistence.EntityManagerFactory;
import org.springframework.beans.factory.annotation.Value;

@Configuration
@EnableBatchProcessing
public class BatchConfig {

    @Value("${batch.chunk-size}")
    private final int chunkSize;

    public BatchConfig(@Value("${batch.chunk-size}") int chunkSize) {
        this.chunkSize = chunkSize;
    }

    @Bean
    public FlatFileItemReader<Product> reader() {
        return new FlatFileItemReaderBuilder<Product>()
                .name("productItemReader")
                .resource(new ClassPathResource("db/data/massive_products.csv"))
                .linesToSkip(1)
                .delimited()
                .delimiter(",")
                .names(new String[] {
                        "id", "uuid", "name", "brand", "category", "subcategory", "description",
                        "price", "currency", "discountPercent", "finalPrice", "rating", "reviewCount",
                        "stockQuantity", "inStock", "sku", "barcode", "weightKg", "tags", "imageUrl",
                        "thumbnailUrl", "createdAt", "updatedAt", "status", "featured", "lengthCm",
                        "widthCm", "heightCm", "freeShipping", "shippingCost", "estimatedDays"
                })
                .fieldSetMapper(fieldSet -> Product.builder()
                        // .id(fieldSet.readLong("id"))
                        .uuid(java.util.UUID.fromString(fieldSet.readString("uuid")))
                        .name(fieldSet.readString("name"))
                        .brand(fieldSet.readString("brand"))
                        .category(fieldSet.readString("category"))
                        .subcategory(fieldSet.readString("subcategory"))
                        .description(fieldSet.readString("description"))
                        .price(fieldSet.readDouble("price"))
                        .currency(fieldSet.readString("currency"))
                        .discountPercent(fieldSet.readDouble("discountPercent"))
                        .finalPrice(fieldSet.readDouble("finalPrice"))
                        .rating(fieldSet.readDouble("rating"))
                        .reviewCount(fieldSet.readInt("reviewCount"))
                        .stockQuantity(fieldSet.readInt("stockQuantity"))
                        .inStock(fieldSet.readBoolean("inStock"))
                        .sku(fieldSet.readString("sku"))
                        .barcode(fieldSet.readString("barcode"))
                        .weightKg(fieldSet.readDouble("weightKg"))
                        .tags(fieldSet.readString("tags"))
                        .imageUrl(fieldSet.readString("imageUrl"))
                        .thumbnailUrl(fieldSet.readString("thumbnailUrl"))
                        .createdAt(LocalDateTime.parse(fieldSet.readString("createdAt")))
                        .updatedAt(LocalDateTime.parse(fieldSet.readString("updatedAt")))
                        .status(fieldSet.readString("status"))
                        .featured(fieldSet.readBoolean("featured"))
                        .lengthCm(fieldSet.readDouble("lengthCm"))
                        .widthCm(fieldSet.readDouble("widthCm"))
                        .heightCm(fieldSet.readDouble("heightCm"))
                        .freeShipping(fieldSet.readBoolean("freeShipping"))
                        .shippingCost(fieldSet.readDouble("shippingCost"))
                        .estimatedDays(fieldSet.readInt("estimatedDays"))
                        .build()
                )
                .build();
    }

    @Bean
    public JpaItemWriter<Product> writer(EntityManagerFactory entityManagerFactory) {
        return new JpaItemWriterBuilder<Product>()
                .entityManagerFactory(entityManagerFactory)
                .build();
    }

    @Bean
    @StepScope
    public ItemProcessor<Product, Product> categoryFilterProcessor(
            @Value("#{jobParameters['categories']}") String categories) {
        if (categories == null || categories.isEmpty()) {
            return item -> item; // If no categories are provided, pass all items through.
        }
        Set<String> categorySet = Arrays.stream(categories.split(","))
                .map(String::trim)
                .collect(Collectors.toSet());

        return product -> {
            if (categorySet.contains(product.getCategory())) {
                return product; // Keep the product if its category is in the set.
            }
            return null; // Discard the product by returning null.
        };
    }

    @Bean
    public ItemProcessor<Product, Product> customiseLinkProcessor(
            @Value("${customise.link.suffix}") String linkSuffix) {
        return product -> {
            if (product.getImageUrl() != null && !product.getImageUrl().isEmpty()) {
                product.setCustomiseLink(product.getImageUrl() + linkSuffix);
            }
            return product;
        };
    }

    @Bean
    public CompositeItemProcessor<Product, Product> compositeProcessor(
            @Qualifier("categoryFilterProcessor") ItemProcessor<Product, Product> categoryFilterProcessor,
            @Qualifier("customiseLinkProcessor") ItemProcessor<Product, Product> customiseLinkProcessor) {
        CompositeItemProcessor<Product, Product> processor = new CompositeItemProcessor<>();
        processor.setDelegates(Arrays.asList(categoryFilterProcessor, customiseLinkProcessor));
        return processor;
    }

    @Bean
    public Step csvImportStep(  JobRepository jobRepository,
                                 FlatFileItemReader<Product> reader,
                                 CompositeItemProcessor<Product, Product> compositeProcessor,
                                 JpaItemWriter<Product> writer,
                                 PlatformTransactionManager transactionManager) {
        return new StepBuilder("csvImportStep", jobRepository)
                .<Product, Product>chunk(this.chunkSize, transactionManager)
                .reader(reader)
                .processor(compositeProcessor)
                .writer(writer)
                .allowStartIfComplete(true)
                .build();
    }

    @Bean
    public Step anotherStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        return new StepBuilder("anotherStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    System.out.println("This is another step in the job.");
                    return org.springframework.batch.repeat.RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }
    
    @Bean
    public Job csvImportJob(JobRepository jobRepository, Step csvImportStep, Step anotherStep) {
        return new JobBuilder("csvImportJob", jobRepository)
                // .start(csvImportStep)
                .incrementer(new RunIdIncrementer())
                .flow(csvImportStep)
                // .next(anotherStep)
                .end()
                .build();
    }
}
