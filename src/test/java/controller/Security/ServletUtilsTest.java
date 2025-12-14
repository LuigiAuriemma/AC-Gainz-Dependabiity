package controller.Security;

import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

/**
 * Test class for ServletUtils to kill mutants.
 */
class ServletUtilsTest {

    private final ByteArrayOutputStream errContent = new ByteArrayOutputStream();
    private final PrintStream originalErr = System.err;

    @BeforeEach
    void setUpStreams() {
        System.setErr(new PrintStream(errContent));
    }

    @AfterEach
    void restoreStreams() {
        System.setErr(originalErr);
    }

    @Test
    @DisplayName("sendErrorSafe catches IOException and logs to System.err")
    void sendErrorSafe_ioException_logsToSystemErr() throws IOException {
        HttpServletResponse response = mock(HttpServletResponse.class);
        String errorMessage = "Test Error Message";
        int errorCode = 500;

        // Force sendError to throw IOException
        doThrow(new IOException("Simulated IO Error")).when(response).sendError(errorCode, errorMessage);

        // Execute method
        ServletUtils.sendErrorSafe(response, errorCode, errorMessage);

        // Verify that System.err contains the expected log message
        String output = errContent.toString();
        assertTrue(output
                .contains("CRITICAL: Impossibile inviare la pagina di errore al client (500) - Simulated IO Error"));
    }

    @Test
    @DisplayName("sendErrorSafe success path")
    void sendErrorSafe_success_noLog() throws IOException {
        HttpServletResponse response = mock(HttpServletResponse.class);
        String errorMessage = "OK";
        int errorCode = 200;

        // Execution success
        ServletUtils.sendErrorSafe(response, errorCode, errorMessage);

        verify(response).sendError(errorCode, errorMessage);
        // Verify stream does not contain the error log
        // (checking isEmpty() might be flaky if other tools write to stderr)
        assertTrue(!errContent.toString().contains("CRITICAL:"));
    }
}
