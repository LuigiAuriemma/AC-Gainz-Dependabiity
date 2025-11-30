package benchmark;

import model.Carrello;
import model.DettaglioOrdine;
import model.Ordine;
import model.Utente;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS) // Misuriamo in microsecondi (è codice veloce)
@Fork(1)
@Warmup(iterations = 15, time = 1)
@Measurement(iterations =  10, time = 1)

// BENCHMARK SULLA LOGICA DI CREAZIONE ORDINE (tratta dalla doPost di OrdineServlet)
public class OrderProcessingBenchmark {

    private Utente utente;
    private List<Carrello> carrelloPiccolo;
    private List<Carrello> carrelloGrande;

    // Simuliamo l'ID che verrebbe dal DB
    private static final int FAKE_ORDER_ID = 123;

    @Setup
    public void setup() {
        // 1. PREPARAZIONE DATI
        // Creiamo tutto in memoria (POJO) per non avere dipendenze esterne

        utente = new Utente();
        utente.setEmail("benchmark@studenti.unisa.it");

        // Scenario A: Carrello Piccolo (es. 5 oggetti)
        carrelloPiccolo = creaCarrelloFinto(5);

        // Scenario B: Carrello Grande (es. 50 oggetti - Stress Test)
        carrelloGrande = creaCarrelloFinto(50);
    }

    // Helper per popolare il carrello
    private List<Carrello> creaCarrelloFinto(int nElementi) {
        List<Carrello> cart = new ArrayList<>();
        for (int i = 0; i < nElementi; i++) {
            Carrello item = new Carrello();
            item.setIdProdotto(String.valueOf(i));
            item.setIdVariante(i * 10);
            item.setQuantita(2); // Supponiamo 2 pezzi per ogni articolo
            cart.add(item);
        }
        return cart;
    }

    // --- BENCHMARK 1: Elaborazione Ordine Piccolo ---
    @Benchmark
    public void testCheckoutLogic_SmallCart(Blackhole bh) {
        // Eseguiamo la logica di business estratta dalla tua Servlet
        List<DettaglioOrdine> result = eseguiLogicaCheckout(utente, carrelloPiccolo);
        bh.consume(result);
    }

    // --- BENCHMARK 2: Elaborazione Ordine Grande (Stress) ---
    @Benchmark
    public void testCheckoutLogic_LargeCart(Blackhole bh) {
        // Eseguiamo la stessa logica su un carrello molto più grande
        List<DettaglioOrdine> result = eseguiLogicaCheckout(utente, carrelloGrande);
        bh.consume(result);
    }

    // Questo metodo replica ESATTAMENTE la logica che ho dentro la doPost.
    private List<DettaglioOrdine> eseguiLogicaCheckout(Utente u, List<Carrello> cart) {

        // 1. Creazione oggetto Ordine (Simulazione parte iniziale doPost)
        Ordine ordine = new Ordine();
        ordine.setEmailUtente(u.getEmail());

        // 2. Creazione dettagli ordine (Il ciclo for della tua Servlet)
        List<DettaglioOrdine> dettaglioOrdine = new ArrayList<>();

        for (Carrello cartItem : cart) {
            DettaglioOrdine dettaglioOrdineItem = new DettaglioOrdine();
            // Usiamo l'ID simulato invece di chiamare dao.getLastInsertedId()
            dettaglioOrdineItem.setIdOrdine(FAKE_ORDER_ID);
            dettaglioOrdineItem.setIdVariante(cartItem.getIdVariante());
            dettaglioOrdineItem.setIdProdotto(cartItem.getIdProdotto());
            dettaglioOrdineItem.setQuantita(cartItem.getQuantita());

            dettaglioOrdine.add(dettaglioOrdineItem);
        }

        return dettaglioOrdine;
    }

}
