package co.icesi.buscaminas.client;

import co.icesi.buscaminas.controllers.dtos.Request;
import co.icesi.buscaminas.controllers.dtos.Response;
import co.icesi.buscaminas.model.Cell;
import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;

public class BuscaminasTCPClient {

    private final Gson gson = new Gson();

    public Response sendRequest(String host, int port, Request request) throws IOException {
        try (Socket socket = new Socket(host, port);
             BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()))) {

            String jsonOut = gson.toJson(request);
            writer.write(jsonOut);
            writer.newLine();
            writer.flush();

            String jsonIn = reader.readLine();
            if (jsonIn == null) {
                throw new IOException("El servidor cerró la conexión sin responder.");
            }
            return gson.fromJson(jsonIn, Response.class);
        }
    }

    public Cell[][] extractBoard(Response response) {
        Object boardRaw = response.data.get("board");
        if (boardRaw == null) return null;
        String boardJson = gson.toJson(boardRaw);
        return gson.fromJson(boardJson, Cell[][].class);
    }
}
