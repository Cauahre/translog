package br.com.translog.persistencia;

import br.com.translog.modelo.Cliente;
import br.com.translog.modelo.TipoCliente;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.lang.reflect.Type;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

/**
 * Configura o Gson do projeto.
 * O Gson não sabe lidar sozinho com java.time nem com herança, então registramos:
 * adaptadores de data e um adaptador que grava o campo "tipo" do cliente para saber
 * qual subclasse recriar na leitura.
 */
public final class GsonFactory {

    private GsonFactory() {
    }

    public static Gson criar() {
        return new GsonBuilder()
                .setPrettyPrinting()
                .registerTypeAdapter(LocalDate.class, new LocalDateAdapter().nullSafe())
                .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter().nullSafe())
                .registerTypeAdapter(Cliente.class, new ClienteAdapter())
                .create();
    }

    private static class LocalDateAdapter extends TypeAdapter<LocalDate> {
        @Override
        public void write(JsonWriter out, LocalDate value) throws IOException {
            out.value(value.toString());
        }

        @Override
        public LocalDate read(JsonReader in) throws IOException {
            String texto = in.nextString();
            try {
                return LocalDate.parse(texto);
            } catch (DateTimeParseException e) {
                throw new JsonParseException("Data inválida no arquivo: " + texto, e);
            }
        }
    }

    private static class LocalDateTimeAdapter extends TypeAdapter<LocalDateTime> {
        @Override
        public void write(JsonWriter out, LocalDateTime value) throws IOException {
            out.value(value.toString());
        }

        @Override
        public LocalDateTime read(JsonReader in) throws IOException {
            String texto = in.nextString();
            try {
                return LocalDateTime.parse(texto);
            } catch (DateTimeParseException e) {
                throw new JsonParseException("Data/hora inválida no arquivo: " + texto, e);
            }
        }
    }

    private static class ClienteAdapter implements JsonSerializer<Cliente>, JsonDeserializer<Cliente> {

        private static final String CAMPO_TIPO = "tipo";

        @Override
        public JsonElement serialize(Cliente cliente, Type tipo, JsonSerializationContext contexto) {
            JsonObject json = contexto.serialize(cliente, cliente.getClass()).getAsJsonObject();
            json.addProperty(CAMPO_TIPO, cliente.getTipo().name());
            return json;
        }

        @Override
        public Cliente deserialize(JsonElement json, Type tipo, JsonDeserializationContext contexto) {
            JsonObject objeto = json.getAsJsonObject();
            JsonElement campoTipo = objeto.get(CAMPO_TIPO);
            if (campoTipo == null) {
                throw new JsonParseException("Cliente sem o campo 'tipo' no arquivo.");
            }
            TipoCliente tipoCliente;
            try {
                tipoCliente = TipoCliente.valueOf(campoTipo.getAsString());
            } catch (IllegalArgumentException e) {
                throw new JsonParseException("Tipo de cliente desconhecido: " + campoTipo.getAsString(), e);
            }
            return contexto.deserialize(objeto, tipoCliente.getClasse());
        }
    }
}
