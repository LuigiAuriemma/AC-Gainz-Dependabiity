package controller.Security;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

public class ServletUtils {

    /**
     * Invia un errore HTTP gestendo internamente l'eccezione IOException.
     * Questo metodo serve per soddisfare le regole di SonarQube evitando try-catch annidati nelle Servlet.
     *
     * @param response L'oggetto HttpServletResponse
     * @param errorCode Il codice di errore HTTP (es. 500, 400)
     * @param message Il messaggio da inviare
     */
    public static void sendErrorSafe(HttpServletResponse response, int errorCode, String message) {
        try {
            response.sendError(errorCode, message);
        } catch (IOException e) {
            // Log dell'errore su console di errore (o usa un logger se configurato)
            System.err.println("CRITICAL: Impossibile inviare la pagina di errore al client (" + errorCode + ") - " + e.getMessage());
        }
    }
}