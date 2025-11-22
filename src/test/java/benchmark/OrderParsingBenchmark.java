package benchmark;

import model.DettaglioOrdine;
import org.openjdk.jmh.annotations.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Fork(1)
@Warmup(iterations = 2, time = 1)
@Measurement(iterations = 5, time = 1)
public class OrderParsingBenchmark {

    private String descrizioneOrdineComplesso;

    @Setup
    public void setup() {
        // Simuliamo una stringa che proviene dal Database.
        // Immaginiamo un ordine con 3 prodotti diversi.
        descrizioneOrdineComplesso =
                "Prodotto: Whey Gold Standard\n" +
                        "Gusto: Cioccolato Doppio\n" +
                        "Confezione: 1000 grammi\n" +
                        "Quantità: 2\n" +
                        "Prezzo: 59.99 €;" +

                        "Prodotto: Creatina Monoidrato\n" +
                        "Gusto: Neutro\n" +
                        "Confezione: 500 grammi\n" +
                        "Quantità: 1\n" +
                        "Prezzo: 19.50 €;" +

                        "Prodotto: Omega 3 Fish Oil\n" +
                        "Gusto: Nessuno\n" +
                        "Confezione: 200 grammi\n" +
                        "Quantità: 3\n" +
                        "Prezzo: 15.00 €";
    }

    // --- TEST DELLA TUA LOGICA DI PARSING ---
    // Abbiamo copiato il metodo 'parseDescrizione' qui perché nella Servlet è private
    // e non potremmo chiamarlo direttamente.
    @Benchmark
    public List<DettaglioOrdine> testOrderParsing() {
        List<DettaglioOrdine> dettagli = new ArrayList<>();

        // 1. Split dei prodotti (Genera array)
        String[] prodotti = descrizioneOrdineComplesso.split(";");

        for (String prodotto : prodotti) {
            // 2. Split degli attributi (Genera array)
            String[] attributi = prodotto.trim().split("\\n");

            String nomeProdotto = "";
            String gusto = "";
            int pesoConfezione = 0;
            int quantita = 0;
            float prezzo = 0;

            for (String attributo : attributi) {
                attributo = attributo.trim();

                // 3. Molteplici chiamate a replace() generano molte stringhe temporanee
                if (attributo.startsWith("Prodotto:")) {
                    nomeProdotto = attributo.replace("Prodotto:", "").trim();
                } else if (attributo.startsWith("Gusto:")) {
                    gusto = attributo.replace("Gusto:", "").trim();
                } else if (attributo.startsWith("Confezione:")) {
                    pesoConfezione = Integer.parseInt(attributo.replace("Confezione:", "").replace(" grammi", "").trim());
                } else if (attributo.startsWith("Quantità:")) {
                    quantita = Integer.parseInt(attributo.replace("Quantità:", "").trim());
                } else if (attributo.startsWith("Prezzo:")) {
                    prezzo = Float.parseFloat(attributo.replace("Prezzo:", "").replace(" €", "").trim());
                }
            }

            DettaglioOrdine dettaglio = new DettaglioOrdine();
            dettaglio.setNomeProdotto(nomeProdotto);
            dettaglio.setGusto(gusto);
            dettaglio.setPesoConfezione(pesoConfezione);
            dettaglio.setQuantita(quantita);
            dettaglio.setPrezzo(prezzo);

            dettagli.add(dettaglio);
        }

        return dettagli;
    }
}