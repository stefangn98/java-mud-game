import java.rmi.RemoteException;

public class Server {
    public static void main(String[] args) throws RemoteException {
        Integer port_reg = 0;
        Integer port_server = 0;
        Integer max_dungeons = 2;
        Integer max_players = 3;

        try {
            port_reg = Integer.parseInt(args[0]);
            port_server = Integer.parseInt(args[1]);
            max_dungeons = Integer.parseInt(args[2]);
            max_players = Integer.parseInt(args[3]);
        } catch(ArrayIndexOutOfBoundsException e) {
            System.err.println("Error, wrong arguments. Default ones set.\n <rmi_registry_port> <rmi_server_port> <max_dungeons_allowed> <max_players_allowed>\n");
        }

        new ServerImpl(port_reg, port_server, max_dungeons, max_players, 16);
    }
}