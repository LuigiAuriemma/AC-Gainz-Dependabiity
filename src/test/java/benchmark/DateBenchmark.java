package benchmark;

import org.openjdk.jmh.annotations.*;

import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.concurrent.TimeUnit;

@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Fork(1)
@Warmup(iterations = 2, time = 1)
@Measurement(iterations = 5, time = 1)
public class DateBenchmark {

    private Date dataDiNascita;

    // Ottimizzazione: Il formatter moderno si crea UNA volta sola ed è Thread-Safe
    private static final DateTimeFormatter MODERN_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Setup
    public void setup() {
        // Simuliamo una data di nascita (oggi)
        dataDiNascita = new Date();
    }

    // --- METODO 1: Il tuo approccio attuale (Lento) ---
    // Crea una nuova istanza di SimpleDateFormat ad ogni chiamata.
    // Questo genera molta "spazzatura" per il Garbage Collector.
    @Benchmark
    public String testNewSimpleDateFormat() {
        return new SimpleDateFormat("yyyy-MM-dd").format(dataDiNascita);
    }

    // --- METODO 2: Approccio Java 8+ (Veloce & Thread-Safe) ---
    // Usa le nuove API java.time che non richiedono istanziazione continua.
    @Benchmark
    public String testJava8DateTimeFormatter() {
        // Dobbiamo convertire la vecchia Date in LocalDate (operazione veloce)
        return dataDiNascita.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
                .format(MODERN_FORMATTER);
    }
}