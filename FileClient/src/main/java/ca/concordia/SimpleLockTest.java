package ca.concordia;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;


public class SimpleLockTest
{
    public static void main(String[] args) {
        System.out.println("Simple Lock Test");
        System.out.println("Starting 3 clients and running simultaneously");


        // Start 3 clients/threads at the same time and all of them run testClient at the same time
        new Thread(() -> testClient(1)).start();
        new Thread(() -> testClient(2)).start();
        new Thread(() -> testClient(3)).start();
    }

    // Each client does: CREATE, WRITE, then disconnect
    private static void testClient(int id) {
        try {
            Socket socket = new Socket("localhost", 12345);
            PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            System.out.println("Client " + id + " connected");
            // These act like as if we're inputting them in the termial
            // CREATE
            writer.println("CREATE file" + id + ".txt");
            reader.readLine();

            // WRITE
            writer.println("WRITE file" + id + ".txt data" + id);
            reader.readLine();

            // QUIT
            writer.println("QUIT");
            socket.close();

            System.out.println("Client " + id + " done");

        }
        catch (Exception e) {
            System.out.println("Client " + id + " error: " + e.getMessage());
        }
    }
}