# 🏋️‍♂️ AC-Gainz

## 📖 Descrizione del Progetto

**AC-Gainz** è un'applicazione web e-commerce dinamica e completa, progettata per la vendita, la navigazione e la gestione di prodotti, idealmente orientata verso il mondo del fitness e dell'integrazione sportiva. Il software gestisce l'intero ciclo di vita di un acquisto online: dall'esplorazione autonoma del catalogo tramite filtri dinamici, alla gestione del carrello, fino del processo di ordine per l'utente, unita ad un robusto sistema gestionale (back-office) dedicato agli amministratori.

L'architettura del software verte pesantemente sull'approccio **MVC (Model-View-Controller)** per separare logicamente la presentazione, la logica transazionale e la struttura dei dati, combinato con il pattern **DAO (Data Access Object)**, garantendo incapsulamento, scalabilità e astrazione forte nella comunicazione col database.

---

## ✨ Funzionalità Principali e Dinamiche Aziendali

### 👤 Lato Utente (Client-Side / Front-Office)
*   **Gestione dell'Account:** Registrazione, validazione rigorosa dei parametri form e autenticazione (Login) sicuri, con un'Area Utente in cui è possibile revisionare i propri dati.
*   **Navigazione Avanzata:** Il cuore del catalogo prevede ricerca e filtri combinati asincroni: categorie, opzioni di confezionamento (`Confezione`) e di sapori (`Gusto`).
*   **Esperienza di Acquisto Fluida:** Gestione dinamica del modulo Carrello (`Carrello.jsp`, gestito tramite JS e Servlet), calcolo del totale temporaneo e mantenimento persistente dello stato prima del checkout.
*   **Gestione Ordini:** Dopo il checkout, l'ordine entra nel database associato al cliente con i suoi dettagli (`DettaglioOrdine`), pronti ad essere storicizzati per la visualizzazione dell'utente.

### 🛡️ Lato Amministratore (Admin Panel / Back-Office)
*   **Gestione CRUD Catalogo:** Tramite una dashboard dedicata, gli amministratori possono aggiungere, modificare o cancellare informazioni (operato dai vari `insertRowServlet`, `editRowServlet`, `deleteRowServlet`).
*   **Gestione delle Varianti:** Inserimento flessibile per quanto riguarda attributi variabili come i gusti o la composizione della scatola.
*   **Supervisione Ordini:** Ispezione di ogni transazione effettuata sulla piattaforma per gestire la supply-chain e l'invio.

---

## 🛠️ Stack Tecnologico e Architettura

Il progetto fa uso di una sinergia tra framework consolidati del mondo Enterprise Java con pratiche moderne di Deployment e Performance Testing.

### ⚙️ Backend (Logica Applicativa)
*   **Java (Servlets & JSP):** Utilizzate rispettivamente come implementazione standard Controller e View. Le Servlet elaborano le richieste HTTP in entrata ed instradano i risultati verso le JSP (che risiedono sia esposte e in `/WEB-INF/` per motivi di sicurezza).
*   **JDBC (Java Database Connectivity):** Comunicazione a basso livello verso il RDBMS al fine di avere massimo controllo prestazionale, sfruttando Connection Pooling (`ConPool.java`) per ottimizzare i consumi di risorsa di rete.
*   **Pattern Architetturali:** Model, View, Controller e DAO Pattern per le entità (`ProdottoDAO`, `UtenteDAO`, ecc.).

### 💻 Frontend (Presentazione)
*   **HTML5 & CSS3:** Design responsive e moduli suddivisi semanticamente (es. le cartelle per componenti `Header`, `Footer`, `ProductCard`, ecc.).
*   **JavaScript (Vanilla ES6+):** Utilizzato diffusamente per aggiornare le UI in modo asincrono, gestire eventi complessi del carrello (`CartPopUp.js`), validare i moduli d'ingresso di sicurezza (`validateForm.js`) ed effettuare display dinamici (es. `showTastes.js`).

### 📦 Dati, Gestione e Infrastruttura
*   **Database Relazionale (SQL):** MySQL o database equivalente per salvataggio d'informazioni altamente strutturate, provviste di script di initialization in `/DB/CodiceSQL/`.
*   **Docker & Docker Compose:** L'applicazione è completamente containerizzata (web server Java + database Server SQL su network isolati). Garantisce dipendenza di esecuzione nulla (nessun bisogno di un DBMS locale preconfigurato) e riproducibilità immediata.
*   **Apache Maven:** Utilizzato come standard di mercato per la risoluzione delle dipendenze dei binari Java e l'unificazione del ciclo di compila e test (`pom.xml`).

### 🧪 Test, Affidabilità (Dependability) e Benchmarking
Il progetto presenta un'intera infrastruttura dedicata alle performarce asseverando i requirementi di stabilità del sistema (sezione `test/java/benchmark`).
*   **JUnit:** Per testing d'integrazione/unit del pattern Entity e Repository (DAO).
*   **JMH (Java Microbenchmark Harness):** Tool avanzato per l'analisi dei picchi applicativi e colli di bottiglia (`BatchQueryBenchmark`, `JsonFilterBenchmark`, `OrderProcessingBenchmark`). Questo dimostra una chiara attenzione allo scaling sotto flussi di rete gravosi e validazione della Dependability.

---

## 🚀 Requisiti e Guida all'Installazione

Essendo concepito con astrazioni Docker, la fase di running risulta abbattuta drasticamente in pochi passi standard.

### Prerequisiti
1.  **Docker** installato e in esecuzione.
2.  **Docker Compose** abilitato.
3.  *(Opzionale)* **Java 17/21 e Maven 3+** solo in casi di sviluppo senza container base.

### Come avviare l'infrastruttura
1.  **Copia della Repository**
    ```bash
    git clone https://github.com/Raff2812/AC-Gainz.git
    cd AC-Gainz
    ```
2.  **Lancio ed Istanziamento Container**
    Utilizza lo script Maven wrapper configurato o direttamente il tool compose per mettere su stack di App + Database.
    ```bash
    docker-compose up --build
    ```
3.  **Popolamento Dati** (Avviene automaticamente in background grazie al `Dockerfile` custom della cartella `DB/`).
4.  **Accedi al Portale** aprendo il browser alla porta nativa (solitamente http://localhost:8080/ dipendendo dalla vostra variabile web-port).
