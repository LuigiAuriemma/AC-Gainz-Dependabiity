package benchmark;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Fork(1)
@Warmup(iterations = 10, time = 1)
@Measurement(iterations = 10, time = 1)
public class BatchQueryBenchmark {

    private List<String> cartItems;

    // SIMULAZIONE COSTO DB
    // Questo valore rappresenta il "lavoro" o l'attesa per comunicare col DB.
    // Più è alto, più evidente sarà il problema dell'N+1.
    private static final int DB_LATENCY_TOKENS = 5000;

    @Setup
    public void setup() {
        // Carrello con 20 prodotti
        cartItems = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            cartItems.add("PROD_ID_" + i);
        }
    }

    // --- METODO 1: N+1 Problem (Query nel ciclo) ---
    // Qui paghiamo la latenza per OGNI prodotto (20 volte)
    @Benchmark
    public void testLoopInsert_WithLatency(Blackhole bh) {
        for (String item : cartItems) {
            String sql = "INSERT INTO carrello (id_utente, id_prodotto) VALUES (1, '" + item + "')";

            // SIMULIAMO LA CHIAMATA AL DB (Latenza pagata 20 volte)
            bh.consumeCPU(DB_LATENCY_TOKENS);
        }
    }

    // --- METODO 2: Batch Optimization (Query unica) ---
    // Qui paghiamo la latenza UNA sola volta per tutto il carrello
    @Benchmark
    public void testBatchInsert_WithLatency(Blackhole bh) {
        StringBuilder sql = new StringBuilder("INSERT INTO carrello (id_utente, id_prodotto) VALUES ");

        int size = cartItems.size();
        for (int i = 0; i < size; i++) {
            sql.append("(1, '").append(cartItems.get(i)).append("')");
            if (i < size - 1) {
                sql.append(", ");
            }
        }

        // SIMULIAMO LA CHIAMATA AL DB (Latenza pagata 1 volta sola)
        bh.consumeCPU(DB_LATENCY_TOKENS);
    }
}