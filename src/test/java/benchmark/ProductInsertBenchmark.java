package benchmark;

import model.Prodotto;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Fork(1)
@Warmup(iterations = 10, time = 1)
@Measurement(iterations = 10, time = 1)

public class ProductInsertBenchmark {
    @State(Scope.Thread)
    public static class UploadState {
        // Dati simulati dalla Request
        public String idProdotto = "PROD-123";
        public String nome = "Proteine Whey";
        public String descrizione = "Proteine in polvere";
        public String categoria = "Integratori";

        // Stringhe numeriche da parsare
        public String sCalorie = "300";
        public String sCarboidrati = "5";
        public String sProteine = "25";
        public String sGrassi = "2";

        // Dati per simulazione Upload
        public String fileNameOriginale = "immagine_prodotto.jpg";
        public String cartellaUpload = "Immagini";

        // Simuliamo quante volte il file esiste già (per testare il ciclo for)
        @Param({"0", "10", "100"})
        public int collisioniFile;
    }

    // --- BENCHMARK 1: Logica di Mapping (Request -> Oggetto) ---
    @Benchmark
    public void testProductParsingLogic(UploadState state, Blackhole bh) {
        // Misuriamo quanto costa parsare i numeri e creare l'oggetto

        Prodotto p = new Prodotto();
        p.setIdProdotto(state.idProdotto);
        p.setNome(state.nome);
        p.setDescrizione(state.descrizione);
        p.setCategoria(state.categoria);

        // Parsing interi (punto critico CPU)
        p.setCalorie(Integer.parseInt(state.sCalorie));
        p.setCarboidrati(Integer.parseInt(state.sCarboidrati));
        p.setProteine(Integer.parseInt(state.sProteine));
        p.setGrassi(Integer.parseInt(state.sGrassi));

        // Consumiamo l'oggetto per evitare Dead Code Elimination
        bh.consume(p);
    }

    // --- BENCHMARK 2: Logica Risoluzione Nome File (Collision Loop) ---
    @Benchmark
    public String testFileNameResolution(UploadState state) {
        // Simuliamo ESATTAMENTE il ciclo for presente in insertProdotto:
        /*
            for (int i = 2; Files.exists(pathDestinazione); i++) {
                destinazione = CARTELLA_UPLOAD + "/" + i + "_" + fileName;
                ...
            }
           Invece di chiamare Files.exists (che tocca il disco), simuliamo la logica
           Supponiamo che il file esista 'state.collisioniFile' volte.
        */

        String destinazione = state.cartellaUpload + "/" + state.fileNameOriginale;



        int i = 2;
        int collisionCount = 0;

        // Questo ciclo simula il costo computazionale di generare stringhe nuove
        // quando un file esiste già.
        while (collisionCount < state.collisioniFile) {
            // Logica presente nella servlet:
            destinazione = state.cartellaUpload + "/" + i + "_" + state.fileNameOriginale;

            i++;
            collisionCount++;
        }

        return destinazione;
    }
}
