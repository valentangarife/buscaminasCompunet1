package co.icesi.buscaminas.client;
import co.icesi.buscaminas.controllers.dtos.Request;
import co.icesi.buscaminas.controllers.dtos.Response;
import co.icesi.buscaminas.model.Cell;

import java.io.IOException;
import java.util.HashMap;
import java.util.Scanner;

public class MainClient {

    private static String HOST;
    private static int PORT;

    private static final co.icesi.buscaminas.client.BuscaminasTCPClient client = new co.icesi.buscaminas.client.BuscaminasTCPClient();
    private static final Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        configurarConexion(args);

        int opcion;
        do {
            System.out.println("\n=============================================");
            System.out.println("      BUSCAMINAS DISTRIBUIDO - CLIENTE TCP");
            System.out.println("=============================================");
            System.out.println("[1] Iniciar nueva partida (Filas, Columnas, Minas)");
            System.out.println("[2] Destapar celda (Fila, Columna)");
            System.out.println("[3] Marcar / Desmarcar bandera (Fila, Columna)");
            System.out.println("[4] Consultar estado actual del tablero");
            System.out.println("[5] Rendirse y revelar tablero completo");
            System.out.println("[6] Salir");
            System.out.print("Seleccione una opcion: ");

            opcion = leerEntero();

            try {
                switch (opcion) {
                    case 1 -> iniciarPartida();
                    case 2 -> jugarCelda("SELECT_CELL");
                    case 3 -> jugarCelda("MARK_CELL");
                    case 4 -> consultarTablero();
                    case 5 -> rendirse();
                    case 6 -> System.out.println("\nSaliendo del Buscaminas... ¡Gracias por jugar!");
                    default -> System.out.println("\nOpcion invalida.");
                }
            } catch (IOException e) {
                System.out.println("Error de comunicacion con el servidor: " + e.getMessage());
            }
        } while (opcion != 6);

        scanner.close();
    }

    private static void configurarConexion(String[] args) {
        if (args.length >= 2) {
            HOST = args[0];
            PORT = Integer.parseInt(args[1]);
        } else {
            System.out.print("IP del servidor (Enter = localhost): ");
            String host = scanner.nextLine().trim();
            HOST = host.isEmpty() ? "localhost" : host;

            System.out.print("Puerto del servidor (Enter = 12345): ");
            String portStr = scanner.nextLine().trim();
            PORT = portStr.isEmpty() ? 12345 : Integer.parseInt(portStr);
        }
        System.out.println("Conectando a " + HOST + ":" + PORT + " ...");
    }

    private static void iniciarPartida() throws IOException {
        System.out.print("Filas: ");
        int n = leerEntero();
        System.out.print("Columnas: ");
        int m = leerEntero();
        System.out.print("Minas: ");
        int minas = leerEntero();

        Request req = new Request();
        req.action = "INIT_GAME";
        req.data = new HashMap<>();
        req.data.put("n", String.valueOf(n));
        req.data.put("m", String.valueOf(m));
        req.data.put("minas", String.valueOf(minas));

        Response resp = client.sendRequest(HOST, PORT, req);
        manejarRespuesta(resp);
    }

    private static void jugarCelda(String accion) throws IOException {
        System.out.print("Fila (i): ");
        int i = leerEntero();
        System.out.print("Columna (j): ");
        int j = leerEntero();

        Request req = new Request();
        req.action = accion;
        req.data = new HashMap<>();
        req.data.put("i", String.valueOf(i));
        req.data.put("j", String.valueOf(j));

        Response resp = client.sendRequest(HOST, PORT, req);
        manejarRespuesta(resp);
    }

    private static void consultarTablero() throws IOException {
        Request req = new Request();
        req.action = "GET_BOARD";
        req.data = new HashMap<>();

        Response resp = client.sendRequest(HOST, PORT, req);
        manejarRespuesta(resp);
    }

    private static void rendirse() throws IOException {
        Request req = new Request();
        req.action = "SOW_ALL";
        req.data = new HashMap<>();

        Response resp = client.sendRequest(HOST, PORT, req);
        manejarRespuesta(resp);
        System.out.println("Te has rendido. Tablero completo revelado.");
    }

    private static void manejarRespuesta(Response resp) {
        if (resp == null) {
            System.out.println("No se recibio respuesta del servidor.");
            return;
        }
        if (!"OK".equals(resp.status)) {
            Object msg = resp.data != null ? resp.data.get("message") : null;
            System.out.println("ERROR del servidor: " + (msg != null ? msg : "sin detalle"));
            return;
        }

        Cell[][] board = client.extractBoard(resp);
        if (board != null) {
            imprimirTablero(board);
        }

        Object gameEndObj = resp.data.get("gameEnd");
        Object winObj = resp.data.get("win");
        boolean gameEnd = gameEndObj != null && Boolean.parseBoolean(gameEndObj.toString());
        boolean win = winObj != null && Boolean.parseBoolean(winObj.toString());

        if (gameEnd) {
            if (win) {
                System.out.println("\n*** GANASTE! Felicidades, encontraste todas las celdas seguras. ***");
            } else {
                System.out.println("\n*** BOOM! Pisaste una mina. Derrota. ***");
                try {
                    rendirse();
                } catch (IOException e) {
                    System.out.println("No se pudo revelar el tablero final: " + e.getMessage());
                }
            }
        }
    }

    private static void imprimirTablero(Cell[][] board) {
        System.out.print("    ");
        for (int j = 0; j < board[0].length; j++) {
            System.out.printf("%2d ", j);
        }
        System.out.println();

        for (int i = 0; i < board.length; i++) {
            System.out.printf("%2d  ", i);
            for (int j = 0; j < board[i].length; j++) {
                System.out.print("[" + board[i][j].toString() + "] ");
            }
            System.out.println();
        }
    }

    private static int leerEntero() {
        while (!scanner.hasNextInt()) {
            System.out.print("Ingrese un numero valido: ");
            scanner.next();
        }
        int valor = scanner.nextInt();
        scanner.nextLine();
        return valor;
    }
}
