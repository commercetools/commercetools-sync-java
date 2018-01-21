package com.commercetools.sync.benchmark;


import org.junit.Test;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ProductSyncBenchmark {

    @Test
    @SuppressWarnings("PMD")
    public void product_benchmark() {
        try {
            replaceInFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void replaceInFile() throws IOException {
        System.out.println("Staring file reading..");
        final Path path = Paths.get(System.getenv("TRAVIS_BUILD_DIR") + "git_temp_dir/docs/BENCHMARKS.md");
        final Charset charset = StandardCharsets.UTF_8;
        final String search = "#test";
        final String replacement = "auto injected from product benchmark";

        String content = new String(Files.readAllBytes(path), charset);

        System.out.println("File '" + path + "' contents:\n" + content);

        content = content.replaceAll(search, replacement);

        System.out.println("After replacement contents:\n" + content);
        Files.write(path, content.getBytes(charset));
    }
}
