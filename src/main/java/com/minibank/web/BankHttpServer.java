package com.minibank.web;

import com.minibank.repository.BankService;
import com.minibank.repository.Migrations;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

public final class BankHttpServer {

    private static final int PORT = 8080;

    private BankHttpServer() {
    }

    public static void main(String[] args) throws IOException {
        Migrations.run();

        BankService bank = new BankService();

        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
        server.createContext("/transfer", new TransferHandler(bank));
        server.createContext("/clients", new ClientHandler(bank));
        server.createContext("/accounts", new AccountHandler(bank));
        server.setExecutor(Executors.newFixedThreadPool(8));
        server.start();

        System.out.println("MiniBank HTTP server запущен на http://localhost:" + PORT);
        System.out.println("Эндпоинты: POST /transfer, POST /clients, POST /accounts");
    }
}
