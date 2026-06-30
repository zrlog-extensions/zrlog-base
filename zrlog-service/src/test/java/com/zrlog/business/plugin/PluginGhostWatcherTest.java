package com.zrlog.business.plugin;

import org.junit.Test;

import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.FutureTask;

import static org.junit.Assert.assertTrue;

public class PluginGhostWatcherTest {

    @Test
    public void shouldReadUntilPluginWatcherSocketCloses() throws Exception {
        try (ServerSocket serverSocket = new ServerSocket(0)) {
            FutureTask<Void> serverTask = new FutureTask<>(() -> {
                try (Socket socket = serverSocket.accept();
                     OutputStream outputStream = socket.getOutputStream()) {
                    outputStream.write("alive".getBytes(StandardCharsets.UTF_8));
                }
                return null;
            });
            Thread serverThread = new Thread(serverTask, "plugin-ghost-watcher-test-server");
            serverThread.start();

            new PluginGhostWatcher("127.0.0.1", serverSocket.getLocalPort(), 0).doWatch();

            serverTask.get();
        }
    }

    @Test
    public void shouldReturnWhenWatcherSocketCannotConnect() throws Exception {
        int unusedPort;
        try (ServerSocket serverSocket = new ServerSocket(0)) {
            unusedPort = serverSocket.getLocalPort();
        }

        new PluginGhostWatcher("127.0.0.1", unusedPort, 0).doWatch();

        assertTrue(unusedPort > 0);
    }
}
