package benchmark;

import controller.Filters.GenericFilterServlet;
import model.Prodotto;
import model.Variante;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
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

//BENCHMARK SUL METODO getJsonObject IN Filters.GenericFilterServlet
public class JsonFilterBenchmark {

    private Prodotto prodottoSingolo;
    private List<Prodotto> listaProdotti;

    @Setup
    public void setup() {
        // 1. PREPARAZIONE DATI (SIMULAZIONE DB)
        // Non usiamo il DAO qui per non sporcare il test con la lentezza del DB.
        // Creiamo i dati in memoria.

        // Setup Prodotto Singolo
        prodottoSingolo = creaProdottoFinto(1);

        // Setup Lista di 50 Prodotti (Scenario Reale)
        listaProdotti = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            listaProdotti.add(creaProdottoFinto(i));
        }
    }

    // Helper per creare dati finti
    private Prodotto creaProdottoFinto(int id) {
        Prodotto p = new Prodotto();
        p.setIdProdotto("PROD-" + id);
        p.setNome("Prodotto Test " + id);
        p.setCategoria("Integratori");
        p.setCalorie(150);
        p.setImmagine("img/test.jpg");

        // Importante: il metodo getJsonObject accede a p.getVarianti().get(0)
        // Dobbiamo assicurarci che esistano le varianti per evitare NullPointerException nel benchmark
        List<Variante> varianti = new ArrayList<>();
        Variante v = new Variante();
        v.setIdVariante(id * 100);
        v.setPrezzo(29.99f);
        v.setSconto(10);
        v.setGusto("Cioccolato");
        v.setPesoConfezione(1000);
        varianti.add(v);

        p.setVarianti(varianti);
        return p;
    }

    // --- BENCHMARK 1: Conversione Singola ---
    @Benchmark
    public JSONObject testSingleJsonConversion() {
        // Testiamo direttamente il metodo statico della tua Servlet
        return GenericFilterServlet.getJsonObject(prodottoSingolo);
    }

    // --- BENCHMARK 2: Conversione Lista (Scenario Reale) ---
    @Benchmark
    public JSONArray testListJsonConversion() {
        // Simulo la logica che ho dentro 'sendJsonResponse'
        JSONArray jsonArray = new JSONArray();

        for (Prodotto p : listaProdotti) {
            JSONObject jsonObject = GenericFilterServlet.getJsonObject(p);
            if (jsonObject != null) {
                jsonArray.add(jsonObject);
            }
        }
        return jsonArray;
    }
}