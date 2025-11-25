package controller;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import model.Prodotto;
import model.ProdottoDAO;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.*;

class LoadOnStartupTest {

    @Test
    void init_Success_SavesProductsToContext() throws ServletException {
        // 1. Mock dell'ambiente Servlet
        LoadOnStartup servlet = new LoadOnStartup();
        ServletConfig mockConfig = mock(ServletConfig.class);
        ServletContext mockContext = mock(ServletContext.class);

        // Quando la servlet chiede il contesto al config, restituiamo il mockContext
        when(mockConfig.getServletContext()).thenReturn(mockContext);

        // Prepariamo una lista finta di prodotti
        List<Prodotto> fakeList = new ArrayList<>();
        Prodotto p = new Prodotto();
        p.setIdProdotto("TEST1");
        fakeList.add(p);

        // 2. Mockiamo la costruzione di ProdottoDAO
        // Questo intercetta il "new ProdottoDAO()" dentro il metodo init
        try (MockedConstruction<ProdottoDAO> mockedDao = Mockito.mockConstruction(ProdottoDAO.class,
                (mock, context) -> {
                    // Istruiamo il mock a restituire la nostra lista finta
                    when(mock.doRetrieveAll()).thenReturn(fakeList);
                })) {

            // 3. Esecuzione del metodo init
            servlet.init(mockConfig);

            // 4. Verifiche
            ProdottoDAO daoMock = mockedDao.constructed().get(0);

            // Verifichiamo che il DAO sia stato chiamato
            verify(daoMock).doRetrieveAll();

            // Verifichiamo che la lista sia stata salvata nel Context con la chiave giusta
            verify(mockContext).setAttribute("Products", fakeList);
        }
    }
}