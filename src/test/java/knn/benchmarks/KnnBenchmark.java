package knn.benchmarks;

import knn.*;
import knn.hnsw.HnswIndex;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import smile.read;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@State(Scope.Benchmark)
public class KnnBenchmark {

    private static int iteration;

    private HnswIndex<String, float[], TestItem, Float> index;

    @Setup(Level.Invocation)
    public void iteration() {
        iteration = iteration >= 63500 ? 0 : iteration + 1;
    }

    @Setup(Level.Trial)
    public void buildHnsw() {
        index = HnswIndex
                .newBuilder(37, DistanceFunctions.FLOAT_COSINE_DISTANCE, 64000)
                .withCustomSerializers(new JavaObjectSerializer<String>(), new JavaObjectSerializer<TestItem>())
                .withM(12)
                .withEfConstruction(250)
                .withEf(10)
                .withRemoveEnabled()
                .build();

        var dots = read.INSTANCE.csv("src/main/resources/kddcup.http.data_10_percent_corrected", ',', false, '"', '\\', null);
        AtomicInteger i = new AtomicInteger();
        dots.stream().forEach(tuple -> {
            index.add(new TestItem(String.valueOf(i.incrementAndGet()), toFloats(tuple.toArray())));
        });
        System.out.println("Built an index");
    }

    private float[] toFloats(double[] doubles) {
        float[] floats = new float[doubles.length];
        for (int i = 0; i < doubles.length; i++) {
            floats[i] = (float) doubles[i];
        }
        return floats;
    }

    @Benchmark
    public void findNeighbors(Blackhole bh, KnnBenchmark knn) {
        List<SearchResult<TestItem, Float>> nearest = knn.index.findNeighbors(String.valueOf(iteration), 10);
        bh.consume(nearest);
    }
}