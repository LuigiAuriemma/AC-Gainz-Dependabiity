package benchmark;

import org.openjdk.jmh.annotations.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS) // Operazioni veloci, usiamo nanosecondi
@Fork(1)
@Warmup(iterations = 2, time = 1)
@Measurement(iterations = 5, time = 1)
public class CurrencyBenchmark {

    private List<Float> floatPrices;
    private List<BigDecimal> decimalPrices;
    private int discountPercent = 20; // Sconto del 20%

    @Setup
    public void setup() {
        floatPrices = new ArrayList<>();
        decimalPrices = new ArrayList<>();
        Random r = new Random(42); // Seed fisso per riproducibilità

        // Simuliamo 50 prodotti nel carrello con prezzi casuali
        for (int i = 0; i < 50; i++) {
            float val = 10.0f + r.nextFloat() * 100.0f; // Prezzo tra 10.00 e 110.00
            floatPrices.add(val);
            decimalPrices.add(BigDecimal.valueOf(val));
        }
    }

    // --- METODO 1: Float (Attuale - Veloce ma Impreciso) ---
    // Simula la logica del tuo CarrelloServlet
    @Benchmark
    public float testFloatMath() {
        float total = 0;
        for (float price : floatPrices) {
            // Calcolo sconto
            float discounted = price * (1 - (float) discountPercent / 100);
            // Arrotondamento manuale
            discounted = Math.round(discounted * 100.0f) / 100.0f;
            total += discounted;
        }
        return Math.round(total * 100.0f) / 100.0f;
    }

    // --- METODO 2: BigDecimal (Corretto per E-commerce) ---
    // Più lento, ma gestisce i centesimi senza errori di virgola mobile
    @Benchmark
    public BigDecimal testBigDecimalMath() {
        BigDecimal total = BigDecimal.ZERO;
        // 0.80
        BigDecimal discountFactor = BigDecimal.valueOf(100 - discountPercent).divide(BigDecimal.valueOf(100));

        for (BigDecimal price : decimalPrices) {
            BigDecimal discounted = price.multiply(discountFactor);
            // Arrotondamento corretto (HALF_UP è lo standard finanziario)
            discounted = discounted.setScale(2, RoundingMode.HALF_UP);
            total = total.add(discounted);
        }
        return total.setScale(2, RoundingMode.HALF_UP);
    }
}