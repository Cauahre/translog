package br.com.translog.persistencia;

import br.com.translog.modelo.Cliente;
import br.com.translog.modelo.ClienteEmpresarial;
import br.com.translog.modelo.ClientePrioritario;
import br.com.translog.modelo.TipoCliente;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RepositorioJsonTest {

    private final Gson gson = GsonFactory.criar();
    private final Type tipo = new TypeToken<List<Cliente>>() { }.getType();
    private Path pasta;

    @BeforeEach
    void criarPasta() throws IOException {
        pasta = Files.createTempDirectory("translog-teste");
    }

    @Test
    void arquivoAusenteComecaVazio() {
        RepositorioJson<Cliente> repo = new RepositorioJson<>(pasta.resolve("clientes.json"), tipo, gson);
        assertTrue(repo.estaVazio());
        assertTrue(repo.getAviso().isEmpty());
    }

    @Test
    void salvaERecarregaMantendoOTipoDoCliente() {
        Path arquivo = pasta.resolve("clientes.json");
        RepositorioJson<Cliente> repo = new RepositorioJson<>(arquivo, tipo, gson);
        repo.salvar(new ClienteEmpresarial("Empresa Ltda", "99351819000160", "86999990000", null));
        repo.salvar(new ClientePrioritario("Prioritária Ltda", "09378657000161", null, "contato@prioritaria.com"));

        RepositorioJson<Cliente> recarregado = new RepositorioJson<>(arquivo, tipo, gson);
        List<Cliente> clientes = recarregado.listarTodos();

        assertEquals(2, clientes.size());
        assertEquals(TipoCliente.EMPRESARIAL, recarregado.buscarPorId(1).orElseThrow().getTipo());
        assertTrue(recarregado.buscarPorId(2).orElseThrow() instanceof ClientePrioritario);
        assertEquals("contato@prioritaria.com", recarregado.buscarPorId(2).orElseThrow().getEmail());
    }

    @Test
    void arquivoCorrompidoEhIsoladoESistemaContinua() throws IOException {
        Path arquivo = pasta.resolve("clientes.json");
        Files.writeString(arquivo, "{ isso não é json [");

        RepositorioJson<Cliente> repo = new RepositorioJson<>(arquivo, tipo, gson);

        assertTrue(repo.estaVazio());
        assertTrue(repo.getAviso().isPresent());
        try (var arquivos = Files.list(pasta)) {
            assertTrue(arquivos.anyMatch(p -> p.getFileName().toString().contains(".corrompido-")));
        }
    }

    @Test
    void idsContinuamDepoisDoMaiorExistente() {
        Path arquivo = pasta.resolve("clientes.json");
        RepositorioJson<Cliente> repo = new RepositorioJson<>(arquivo, tipo, gson);
        repo.salvar(new ClienteEmpresarial("A Ltda", "99351819000160", null, null));
        repo.salvar(new ClienteEmpresarial("B Ltda", "09378657000161", null, null));
        repo.remover(1);

        Cliente novo = repo.salvar(new ClienteEmpresarial("C Ltda", "97543231000183", null, null));
        assertEquals(3, novo.getId());
    }
}
