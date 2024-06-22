package org.example;

import org.example.loom.ScopedValueApplication;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.web.client.RestTemplate;

import static org.example.Commons.prepareRequest;

@BenchmarkMode(Mode.Throughput)
@Warmup(iterations = 2, time = 30)
@Measurement(iterations = 5, batchSize = 5, time = 120)
@Fork(value = 2)
@Threads(100)
@State(Scope.Benchmark)
public class ScopedValuesBenchmark {
    private static final RestTemplate client = new RestTemplate();
    private ConfigurableApplicationContext applicationContext;

    @Setup
    public void setup() {
        applicationContext = new SpringApplicationBuilder(ScopedValueApplication.class)
                .run();
    }

    @TearDown
    public void tearDown() {
        applicationContext.close();
    }

    @Benchmark
    public void structured(Blackhole bh) {
        var request1 = prepareRequest("user:password", "/api/scope");
        var response1 = client.exchange(request1, String.class);
        bh.consume(response1);
    }

    @Benchmark
    public void async(Blackhole bh) {
        var request1 = prepareRequest("user:password", "/api/async");
        var response1 = client.exchange(request1, String.class);
        bh.consume(response1);
    }
}
