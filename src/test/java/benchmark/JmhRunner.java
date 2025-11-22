package benchmark;

import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.Currency;
import java.util.Date;

public class JmhRunner {

    public static void main(String[] args) throws RunnerException {
        OptionsBuilder optBuilder = new OptionsBuilder();

        // 1. Benchmark JSON (Filtri Prodotti & Carrello)
        // Questo testa la velocità di conversione da Oggetto Java a JSON
       // optBuilder.include(JsonFilterBenchmark.class.getSimpleName());

        // 2. Benchmark REGEX (Validazione Login)
        // Questo testa l'ottimizzazione dei pattern per Email e Password
        //optBuilder.include(RegexBenchmark.class.getSimpleName());

        // 3. Benchmark SimpleDateFormat contro DateTimeFormatter (per eventuale miglioria)
        // Questo testa la velocità di creazione delle due classi per lo stesso utilizzo
        //optBuilder.include(DateBenchmark.class.getSimpleName());

        // 4. Benchmark per parsing stringa della descrizione
        // Questo testa la velocità di parsing della descrizione dei prodotti all'interno dell'ordine visualizzabile nell'Area Personale
        //optBuilder.include(OrderParsingBenchmark.class.getSimpleName());

        // 5. Benchmark per testare l'N+1 problem che abbiamo nel salvare il carrello nel momento del logout
        //optBuilder.include(BatchQueryBenchmark.class.getSimpleName());

        // 6. Benchmark per testare l'attuale utilizzo di float (possibili problemi con arrotondamenti) rispetto a BigDecimal che è piu sicuro per utilizzi finanziari
        optBuilder.include(CurrencyBenchmark.class.getSimpleName());


        Options opt = optBuilder
                .forks(1) // Esegui un solo fork per velocizzare i test in sviluppo
                .build();

        new Runner(opt).run();
    }
}