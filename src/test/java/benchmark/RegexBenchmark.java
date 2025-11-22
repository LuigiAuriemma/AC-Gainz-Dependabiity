package benchmark;

import org.openjdk.jmh.annotations.*;

import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Fork(1)
@Warmup(iterations = 2, time = 1)
@Measurement(iterations = 5, time = 1)
public class RegexBenchmark {

    private String emailUtente;
    private String passwordUtente;

    // --- OPTIMIZATION: Pattern Pre-Compilati (Best Practice) ---
    // Li compiliamo una volta sola all'avvio della classe (static)
    private static final String EMAIL_PATTERN_STR = "^[\\w.%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,8}$";
    private static final Pattern EMAIL_COMPILED = Pattern.compile(EMAIL_PATTERN_STR);

    private static final String PASSWORD_PATTERN_STR = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^\\w\\s]).{8,}$";
    private static final Pattern PASSWORD_COMPILED = Pattern.compile(PASSWORD_PATTERN_STR);

    @Setup
    public void setup() {
        // Dati validi per il test
        emailUtente = "utente.test@universita.it";
        passwordUtente = "PasswordSicura1!";
    }

    // --- METODO 1: Approccio Attuale (Lento) ---
    // Simula esattamente quello che fai nella LoginServlet:
    // Compili il pattern ogni volta che chiami il metodo.
    @Benchmark
    public boolean testRegexCompileEveryTime() {
        // Validazione Email
        Pattern emailRegex = Pattern.compile(EMAIL_PATTERN_STR);
        Matcher emailMatcher = emailRegex.matcher(emailUtente);
        boolean emailOk = emailMatcher.matches();

        // Validazione Password
        Pattern passwordRegex = Pattern.compile(PASSWORD_PATTERN_STR);
        Matcher passwordMatcher = passwordRegex.matcher(passwordUtente);
        boolean passOk = passwordMatcher.matches();

        return emailOk && passOk;
    }

    // --- METODO 2: Approccio Ottimizzato (Veloce) ---
    // Usa i pattern statici pre-compilati.
    @Benchmark
    public boolean testRegexPreCompiled() {
        // Validazione Email (Solo matching, niente compilazione)
        boolean emailOk = EMAIL_COMPILED.matcher(emailUtente).matches();

        // Validazione Password
        boolean passOk = PASSWORD_COMPILED.matcher(passwordUtente).matches();

        return emailOk && passOk;
    }
}