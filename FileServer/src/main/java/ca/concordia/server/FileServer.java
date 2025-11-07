package ca.concordia.server;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import ca.concordia.filesystem.FileSystemManager;

public class FileServer {

    private FileSystemManager fsManager;
    private int port;

    public FileServer(int port, String fileSystemName, int totalSize) {
        // Initialize the FileSystemManager
        try {
            this.fsManager = new FileSystemManager(fileSystemName, 10 * 128);

        } catch (Exception e) {
            System.out.println("FileSystemManager failed to init" + e);
        }

        this.port = port;
    }

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(12345)) {
            System.out.println("Server started. Listening on port 12345...");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Handling client: " + clientSocket);
                try (BufferedReader reader =
                        new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                        PrintWriter writer =
                                new PrintWriter(clientSocket.getOutputStream(), true)) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        System.out.println("Received from client: " + line);
                        String[] parts = line.split(" ");
                        String command = parts[0].toUpperCase();

                        switch (command) {
                            case "CREATE":
                                fsManager.createFile(parts[1]);
                                writer.println("SUCCESS: File '" + parts[1] + "' created.");
                                writer.flush();
                                break;

                            case "WRITE":
                                if (parts.length < 3) {
                                    writer.println(
                                            "Please write under the format WRITE: <file Name> <Content>");
                                }

                                fsManager.writeFile(parts[1],
                                        parts[2].getBytes(StandardCharsets.UTF_8));
                                writer.println(
                                        "Success: File " + parts[1] + " has been written to.");
                                break;

                            case "READ":
                                if (parts.length < 2) {
                                    writer.println(
                                            "Please write under the format: READ <filename>");
                                    break;
                                }

                                byte[] data = fsManager.readFile(parts[1]);
                                writer.println("Success reading: "
                                        + new String(data, StandardCharsets.UTF_8));
                                break;

                            case "DELETE":
                                if (parts.length < 2) {
                                    writer.println(
                                            "lease write under the format: DELETE <filename>");
                                    break;
                                }

                                fsManager.deleteFile(parts[1]);
                                writer.println("Success: File " + parts[1] + " has been delete.");
                                break;

                            case "LIST":
                                String[] files = fsManager.listFiles();
                                if (files.length == 0) {
                                    writer.println("Files not found...");

                                } else {
                                    writer.println("Files: " + String.join(", ", files));
                                }
                                break;

                            case "QUIT":
                                writer.println("SUCCESS: Disconnecting.");
                                return;
                            default:
                                writer.println("ERROR: Unknown command.");
                                break;
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    try {
                        clientSocket.close();
                    } catch (Exception e) {
                        // Ignore
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Could not start server on port " + port);
        }
    }

}
