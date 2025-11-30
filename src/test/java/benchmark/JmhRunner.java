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
        //.include(JsonFilterBenchmark.class.getSimpleName());

        // 4. Benchmark per parsing stringa della descrizione
        // Questo testa la velocità di parsing della descrizione dei prodotti all'interno dell'ordine visualizzabile nell'Area Personale
        //optBuilder.include(OrderParsingBenchmark.class.getSimpleName());

        // 3. Benchmark per testare l'N+1 problem che abbiamo nel salvare il carrello nel momento del logout
        //optBuilder.include(BatchQueryBenchmark.class.getSimpleName());

        // 4. Benchmark per testare la velcoità per processare un ordine.
        //optBuilder.include(OrderProcessingBenchmark.class.getSimpleName());

        // 5. Benchmark per testare la doRetriveAll dei gusti per capire se il problema di efficienza è nell'implementazione o l'interazione col db.
        //optBuilder.include(doRetriveAllGustoBenchmark.class.getSimpleName());

        // 6. Benchmark per testare la velcoità dell'inserimento di un nuovo prodotto e check dell'esistenza di un prodotto uguale.
        optBuilder.include(ProductInsertBenchmark.class.getSimpleName());

        Options opt = optBuilder
                .forks(1) // Esegui un solo fork per velocizzare i test in sviluppo
                .build();

        new Runner(opt).run();
    }
}