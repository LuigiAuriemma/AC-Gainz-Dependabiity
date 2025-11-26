package benchmark;

import model.Gusto;
import org.openjdk.jmh.annotations.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS) // Misuriamo in microsecondi
@Fork(1)
@Warmup(iterations = 2, time = 1)
@Measurement(iterations = 5, time = 1)

public class doRetriveAllGustoBenchmark {
    // --- STATE: Prepariamo i dati per simulare il DB ---
    @State(Scope.Thread)
    public static class GustoState {

        // Dati grezzi simulati (come se venissero dal ResultSet)
        public List<Integer> rawIds;
        public List<String> rawNames;

        // Oggetto Gusto per testare la generazione della Query (doSave)
        public Gusto gustoPerSave;

        @Param({"10", "1000"}) // Testiamo con 10 e con 1000 righe
        public int datasetSize;

        @Setup(Level.Trial)
        public void setup() {
            // 1. Setup per il Mapping (doRetrieveAll)
            rawIds = new ArrayList<>();
            rawNames = new ArrayList<>();
            for (int i = 0; i < datasetSize; i++) {
                rawIds.add(i);
                rawNames.add("Gusto Delizioso " + i);
            }

            // 2. Setup per la Query Generation (doSaveGusto)
            gustoPerSave = new Gusto();
            gustoPerSave.setIdGusto(999);
            gustoPerSave.setNome("Cioccolato Fondente Extra");
        }
    }

    // --- BENCHMARK 1: Mapping Logic (Simulazione doRetrieveAll) ---
    // Misura quanto tempo impiega Java a trasformare i dati grezzi in Oggetti Gusto
    @Benchmark
    public List<Gusto> testRetrieveAllMapping(GustoState state) {
        List<Gusto> gusti = new ArrayList<>();

        // Simuliamo il while(resultSet.next()) iterando sulle liste preparate
        for (int i = 0; i < state.datasetSize; i++) {
            Gusto gusto = new Gusto();
            // Simuliamo resultSet.getInt("id_gusto")
            gusto.setIdGusto(state.rawIds.get(i));
            // Simuliamo resultSet.getString("nomeGusto")
            gusto.setNome(state.rawNames.get(i));

            gusti.add(gusto);
        }
        return gusti;
    }

    // --- BENCHMARK 2: Query Building (Logica doSaveGusto) ---
    // Misura l'efficienza dello StringBuilder e della logica condizionale nel creare la query
    @Benchmark
    public String testSaveQueryBuilder(GustoState state) {
        Gusto g = state.gustoPerSave;

        // Copia esatta della logica del tuo metodo doSaveGusto (senza la parte JDBC)
        StringBuilder stringBuilder = new StringBuilder("INSERT INTO gusto (");
        boolean first = true;

        if (g.getIdGusto() > 0) {
            stringBuilder.append("id_gusto");
            // Nota: saltiamo parameters.add() qui perché stiamo testando la costruzione della stringa SQL
            first = false;
        }

        if (g.getNomeGusto() != null) {
            if (!first) {
                stringBuilder.append(", ");
            }
            stringBuilder.append("nomeGusto");
            first = false;
        }

        stringBuilder.append(") VALUES (");

        // Simuliamo che ci siano 2 parametri (id e nome)
        int paramCount = 2;
        for (int i = 0; i < paramCount; i++) {
            if (i > 0) {
                stringBuilder.append(", ");
            }
            stringBuilder.append("?");
        }
        stringBuilder.append(")");

        return stringBuilder.toString();
    }
}
