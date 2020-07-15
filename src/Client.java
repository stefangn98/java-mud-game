import java.rmi.ConnectException;
import java.rmi.RemoteException;

public class Client {
    public static void main(String[] args) throws RemoteException {
        String hostname = "localhost";
        Integer port = 50015;
        ClientImpl client;

        try {
            hostname = args[0];
            port = Integer.parseInt(args[1]);
        } catch(ArrayIndexOutOfBoundsException e) {
            System.err.println("Error, wrong arguments: " + e.getMessage() + "\n <hostname> <registry port>");
        }

        try {
            client = new ClientImpl(hostname, port);
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                    try {
                        client.abort();
                    } catch(RemoteException e) {
                        System.err.println("Connection error: " + e.getMessage());
                    }
                })
            );
        } catch(ConnectException e) {
            System.err.println("Server is not accessible at this time.");
        }
    }
}