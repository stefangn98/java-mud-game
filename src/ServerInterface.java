import java.rmi.Remote;
import java.rmi.RemoteException;

import java.util.List;

public interface ServerInterface extends Remote {
    void notice(String msg, boolean error) throws RemoteException;
    String menu() throws RemoteException;
    boolean isServerFull() throws RemoteException;
    String displayDungeon(String name) throws RemoteException;

    boolean playerExists(ClientImpl client) throws RemoteException;
    boolean playerLogIn(ClientImpl client) throws RemoteException;
    boolean playerJoinDungeon(ClientImpl client, String mud_name) throws RemoteException;
    void playerDisconnect(ClientImpl client) throws RemoteException;
    boolean mudExists(String name) throws RemoteException;
    boolean spotForMUD() throws RemoteException;
    void createDungeonInstance(String name, int num, ClientImpl client) throws RemoteException;
    String setStartLocation(ClientImpl client) throws RemoteException;
    String playerMove(ClientImpl client, String destination) throws RemoteException;
    String locationInfo(ClientImpl client) throws RemoteException;
    // void playerQuitDungeon(ClientImpl client) throws RemoteException;
    void playerForceShutdown(ClientImpl client) throws RemoteException;
    boolean itemExists(ClientImpl client, String item) throws RemoteException;
    void itemPickedUp(ClientImpl client, String item) throws RemoteException;
    String listCurrentMUDs(boolean flag) throws RemoteException;
    String listCurrentPlayersServer() throws RemoteException;
    List<String> setPlayerInventory() throws RemoteException;
    boolean isUser(ClientImpl client, String item) throws RemoteException;
}