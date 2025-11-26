package benchmark;

import org.openjdk.jmh.annotations.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS) // Misuriamo in microsecondi
@Fork(1)
@Warmup(iterations = 2, time = 1)
@Measurement(iterations = 5, time = 1)
public class BatchQueryBenchmark {

    private List<String> cartItems;

    // Simuliamo una latenza di rete molto bassa (0.05 millisecondi)
    // In un DB reale su server remoto, questo valore sarebbe molto più alto (es. 20-30ms)
    // rendendo la differenza ancora più abissale.
    private static final int SIMULATED_DB_LATENCY_NS = 50_000; // 50 microsecondi

    @Setup
    public void setup() {
        // Simuliamo un carrello con 20 prodotti
        cartItems = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            cartItems.add("Prodotto_ID_" + i);
        }
    }

    // --- METODO 1: Approccio N+1 (Quello della LogoutServlet attuale) ---
    // Problema: Paga il costo della latenza (simulateDbCall) per OGNI prodotto.
    @Benchmark
    public int testLoopInsert() {
        int processed = 0;
        for (String item : cartItems) {
            // 1. Costruiamo la query singola (CPU)
            String sql = "INSERT INTO carrello (id_utente, id_prodotto) VALUES (1, '" + item + "')";


            processed++;
        }
        return processed;
    }

    // --- METODO 2: Approccio Batch (Ottimizzato) ---
    // Vantaggio: Paga il costo della latenza UNA volta sola per tutto il carrello.
    @Benchmark
    public int testBatchInsert() {
        // 1. Costruiamo l'unica query gigante (CPU - leggermente più costoso qui)
        StringBuilder sql = new StringBuilder("INSERT INTO carrello (id_utente, id_prodotto) VALUES ");
        for (int i = 0; i < cartItems.size(); i++) {
            sql.append("(1, '").append(cartItems.get(i)).append("')");
            if (i < cartItems.size() - 1) {
                sql.append(", ");
            }
        }

        return 1;
    }
}


