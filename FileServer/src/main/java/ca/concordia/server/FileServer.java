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
    private final int port;

    public FileServer(int port, String fileSystemName, int totalSize) {
        // Initialize the FileSystemManager
        try {
            this.fsManager = new FileSystemManager(fileSystemName, totalSize);

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

                ClientHandler clientHandler = new ClientHandler(clientSocket, fsManager);
                Thread clientThread = new Thread(clientHandler);
                clientThread.start();
            }

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Could not start server on port " + port);
        }
    }


   private static class ClientHandler implements Runnable
   {
          private final Socket  clientSocket;
          private final FileSystemManager fsManager;

          public ClientHandler(Socket clientSocket, FileSystemManager fsManager)
          {
              this.clientSocket = clientSocket;
              this.fsManager = fsManager;

          }

          @Override
          public void run()
          {
              try(BufferedReader reader = new BufferedReader(
                      new InputStreamReader(clientSocket.getInputStream()));
                  PrintWriter writer = new PrintWriter(clientSocket.getOutputStream(), true))
              {
                  System.out.println("Thread: " + Thread.currentThread().getName() + " handling client: " + clientSocket);

                  String commandline;
                  while ((commandline= reader.readLine()) != null)
                  {
                      System.out.println("Received commandline from client: "  + commandline);
                      handleCommand(commandline, writer);


              }

          }
              catch (Exception ex)
              {
                  System.err.println("Error handling cilent" + clientSocket + ": " + ex.getMessage());
                  ex.printStackTrace();
              }
              finally
              {
                  try
                  {
                      clientSocket.close();
                      System.out.println("Client disconnected:"+ clientSocket);
                  }
                  catch (Exception ex){

                  }
              }

              }
private void handleCommand(String commandline, PrintWriter writer)
{
    try
    {
        String[] parts = commandline.split(" ",3);
        String command = parts[0].toUpperCase();

        switch (command)
        {
            case "CREATE":
                if (parts.length < 2){
                    writer.println("Please write under the format CREATE < filename>");
                    break;
                }
                if (parts[1].length()> 11){
                    writer.println("Please write a filename of 11 characters or under");
                    break;
                }
            fsManager.createFile(parts[1]);
            writer.println("SUCCESS: File '" + parts[1] + "' created.");
            writer.flush();
            break;

            case "WRITE":
                if (parts.length < 3) {
                    writer.println(
                            "Please write under the format WRITE: <file Name> <Content>");
                    break;
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
    catch (Exception ex)
    {
         writer.println("ERROR: " + ex.getMessage());
}



   }
}
}