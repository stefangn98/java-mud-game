import java.util.*;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.text.SimpleDateFormat;

public class ServerImpl implements ServerInterface {
    private String serverName;
    // fields for users
    /**
     * Was thinking of having lists of ClientImpl but it makes
     * things a lot more complicated when it comes to authentication.
     */
    private ArrayList<String> players = new ArrayList<>();   // list of players
    private ArrayList<String> playersWaiting = new ArrayList<>();    // list of players waiting to enter
    private Integer maxPlayers; // maximum allowed connected players
    private boolean serverLock = false;    // lock on server (used so that no more than 1 person can use server functionality at a time)
    private Integer inventoryLimit; // maximum inventory limit (maybe remove from here)

    // fields for MUD(s)
    private Map<String, MUD> allMUD = new HashMap<>();  // hash map of all muds on the server
    private Integer maxMUDS;    // maximum allowed muds on server


    public String menu() {
        String msg = "\t\t\t\t|Main Menu|";
        msg += "\n\t|Currently connected to server -> " + serverName;
        // You are on server " + serverName;
        msg += "\n\t|Create a new mud game -> /create <gamename>";
        msg += "\n\t|Join a mud game -> /join <gamename>";
        msg += "\n\t|Exit server -> /disconnect";
        msg += "\n\t|List of currently active MUD games: ";
        msg += this.listCurrentMUDs(true);

        return msg;
    }

    // add a possibility for users to specify custom files for edges,vertices and msgs
    public void createDungeonInstance(String name, int num, ClientImpl client) {
        if(!this.spotForMUD()) {    // if there is a spot for an instance, create it
            String thg = "mymud.thg";
            String msg = "mymud.msg";
            String edg = "mymud.edg";

            MUD mud = new MUD(edg, msg, thg, num);
            this.notice("\tUser [" + client.getUsername() + "] has created a MUD named [" + name + "].", false);
            this.allMUD.put(name, mud);
        } else {
            // think about changing this from void to boolean
            this.notice("\tUser [" + client.getUsername() + "] failed to create an instance. No free spots left.", true);
            
        }
    }

    // list all active muds
    // uses a flag because i need a different representation for 
    // when the menu is shown and when the players asks for a listing
    public String listCurrentMUDs(boolean flag) {
        Iterator iterator = allMUD.entrySet().iterator();
        String msg;
        if(flag) {
            msg = "";
        } else {
            msg = "\t|List of currently active MUD games: ";
        }
        if(allMUD.size() <= 0) {
            msg += "\n\t\t*No MUD games active.";
            return msg;
        }
        while(iterator.hasNext()) {
            Map.Entry pair = (Map.Entry)iterator.next();    // get each pair and unpack it
            String name = (String)pair.getKey();    // the name of the mud game
            MUD game = (MUD)pair.getValue();    // the actual mud game instance
            Integer currentPlayers = game.getPlayers().size();  // get the number of players in the mud
            Integer maxPlayers = game.getMaxPlayers();  // get the maximum allowed players in the mud

            msg += "\n\t\t├-> " + name + " (" + currentPlayers + "/" + maxPlayers + ")"; // add a message with info
        }
        return msg;
    }

    public String listCurrentPlayersServer() {
        String players = "\n\t|List of online players: ";
        for(String user : this.players) {
            players += "\n\t\t├->[" + user + "]";
        }
        return players;
    }

    public boolean mudExists(String name) {
        if(this.allMUD.keySet().contains(name)) {
            return true;
        }
        return false;
    }

    // used to check if there's a spot for another dungeon instance
    // I realised it's inverted waaaaaaaaaay too far into development 
    // and changing it creates a lot of trouble so it will remain like this.
    public boolean spotForMUD() {
        if(this.allMUD.size() >= this.maxMUDS) {
            return true;
        }
        return false;
    }

    /*
     * This could be done in Server.Java (like in the practical) 
     * and not in a separate method
     * inside this implementation class but that would mean
     * having an empty constructor/creator or not having one at all
     * Personally I like it better this way because stuff is more segmented
     * and easier to find/edit. 
     */ 
    // code taken from the practical on rmi
    private void createServer(int port_reg, int port_serv) throws RemoteException {
        try {
            this.serverName = (InetAddress.getLocalHost()).getCanonicalHostName();
        } catch(UnknownHostException e) {
            System.err.println("Error, cannot get hostname: " + e.getMessage());
            // e.printStackTrace();
        }

        System.setProperty("java.security.policy", ".policy");
        System.setSecurityManager(new SecurityManager());

        ServerInterface mud_interface = (ServerInterface)UnicastRemoteObject.exportObject(this, port_serv);

        String url = "rmi://" + this.serverName + ":" + port_reg + "/mud";
        try {
            Naming.rebind(url, mud_interface);
        } catch(MalformedURLException e) {
            System.err.println("Error, malformed url: " + e.getMessage());
            // e.printStackTrace();
        }

        this.notice("\tServer registered on " + url,false);
        this.notice("\tHostname: \t" + this.serverName,false);
        this.notice("\tServer port: \t" + port_serv,false);
        this.notice("\tRegistry port: \t" + port_reg,false);
        this.notice("\tMaximum number of dungeons: \t" + this.maxMUDS, false);
        this.notice("\tMaximum number of players: \t" + this.maxPlayers, false);

        this.notice("\tServer is running. . .", false);

    }

    // This is just a custom fancy way of displaying 
    // notices to the console with a timestamp and date
    public void notice(String msg, boolean err) {
        SimpleDateFormat formatter = new SimpleDateFormat("[dd/MM/yyyy HH:mm:ss] ");
        String date = formatter.format(new Date());
        
        if(!err) {
            System.out.println(date + msg);
        } else {
            System.err.println(date + msg);
        }
    }

    public boolean playerExists(ClientImpl client) {
        if(this.players.contains(client.getUsername())) {
            return true;
        }
        return false;
    }

    // handle logging in to the server
    public boolean playerLogIn(ClientImpl client) {
        // if the server is not full
        if(this.players.size() < this.maxPlayers) {
            // if the player is already waiting to join
            if(this.playersWaiting.contains(client.getUsername())) {
                this.playersWaiting.remove(client.getUsername());
            }
            // add the client to players (essentially he has connected)
            this.players.add(client.getUsername());
            String msg1 = "\tUser [" + client.getUsername() + "] has joined the server. Capacity " + this.players.size() + "/" + this.maxPlayers;
            String msg2 = "\tThere are currently " + this.playersWaiting.size() + " players waiting to join the server.";
            this.notice(msg1, false);
            this.notice(msg2, false);
            return true;
        } else {    // if the server is full
            // we check if the player has made an attempt to log in previously
            // and has been added to the waiting list
            if(!this.playersWaiting.contains(client.getUsername())) {
                this.playersWaiting.add(client.getUsername());
                String msg = "\tUser [" + client.getUsername() + "] has attempted to join the server."
                + " Server is full and there are " + this.playersWaiting.size() + " players waiting to connect.";
                this.notice(msg, false);
            }
            return false;
        }
    }

    public void playerDisconnect(ClientImpl client) {
        this.players.remove(client.getUsername());
        this.playersWaiting.remove(client.getUsername());

        String msg = "\tUser [" + client.getUsername() + "] has left the server. Capacity [" + this.players.size() + "/" + this.maxPlayers + "]";
        this.notice(msg, false);
    }

    public boolean isServerFull() {
        if(this.players.size() >= this.maxPlayers) {
            return true;
        }
        return false;
    }

    public Integer getInventoryLimit() {
        return this.inventoryLimit;
    }

    public List<String> setPlayerInventory() {
        List<String> inv = new ArrayList<>();
        for(int i = 0; i < this.getInventoryLimit(); i++) {
            inv.add("[ ]");
        }
        return inv;
    }

    /**
     * All methods below have to do with players interacting with a MUD instance
     * via the server thus a server lock is utilised in each one.
     * Making sure that one player at a time accesses the remote methods and 
     * no issue occur.
     * The lock should always be set to "false" initially and when each method
     * enters its critical section, it is set to "true" not allowing 
     * other methods to execute
     */
    public boolean playerJoinDungeon(ClientImpl client, String mud_name) {
        while(this.serverLock) {
            try {
                Thread.sleep(250);  // wait untill pinging again to see if the server is free
            } catch(InterruptedException e ) {
                e.printStackTrace();
            }
        }

        this.setServerLock(true);   // set the lock to true and enter the "critical section"

        MUD current = this.getMUD(mud_name); // retrieve the appropriate dungeon instance for the user
        // System.out.println(current.toString());
        if(current.addPlayer(client.getUsername())) {
            String msg = "\tUser [" + client.getUsername() + "] has joined the dungeon " + mud_name + 
                            " [" + current.getPlayers().size() + "/" + current.getMaxPlayers() + "]";
            this.notice(msg, false);
            this.setServerLock(false);
            return true;
        } else {
            String msg = "\tUser [" + client.getUsername() + "] attempted to join the dungeon " + mud_name + 
                            " [" + current.getPlayers().size() + "/" + current.getMaxPlayers() + "]";
            this.notice(msg, true);
            this.setServerLock(false);
            return false;
        }

    }

    // set the players' start location to the muds start location specified
    public String setStartLocation(ClientImpl client) {
        while(this.serverLock) {
            try {
                Thread.sleep(250);
            } catch(InterruptedException e) {
                e.printStackTrace();
            }
        }

        this.setServerLock(true);

        MUD current = this.getMUD(client.getMUDName()); // get the users current mud instance
        current.addThing(current.startLocation(), client.getUsername());    // add the user to the start position

        this.setServerLock(false);

        return current.startLocation();

    }

    // move the player to a new location
    public String playerMove(ClientImpl client, String dest) {
        while(this.serverLock) {
            try {
                Thread.sleep(250);
            } catch(InterruptedException e) {
                e.printStackTrace();
            }
        }

        this.setServerLock(true);

        MUD current = this.getMUD(client.getMUDName());
        String destination = current.moveThing(client.getUserLocation(), dest, client.getUsername());

        this.setServerLock(false);

        return destination;
    }

    // give a description of the current location the player is in
    public String locationInfo(ClientImpl client) {
        while(this.serverLock) {
            try {
                Thread.sleep(250);
            } catch(InterruptedException e) {
                e.printStackTrace();
            }
        }

        this.setServerLock(true);

        MUD current = this.getMUD(client.getMUDName());
        String info = current.locationInfo(client.getUserLocation());

        this.setServerLock(false);

        return info;
    }

    // when a user picks up an item, remove it from the dungeon instance
    public void itemPickedUp(ClientImpl client, String item) {
        while(this.serverLock) {
            try {
                Thread.sleep(250);
            } catch(InterruptedException e) {
                e.printStackTrace();
            }
        }

        this.setServerLock(true);

        MUD current = this.getMUD(client.getMUDName());
        current.delThing(client.getUserLocation(), item);

        this.notice("\tUser [" + client.getUsername() + "] has picked up a(n) [" + item + 
                    "] \n\t\t\tfrom location [" + client.getUserLocation() + "] in dungeon instance [" + client.getMUDName() +"].",false);

        this.setServerLock(false);
    }

    // check if an item exists at the players location
    public boolean itemExists(ClientImpl client, String item) {
        while(this.serverLock) {
            try {
                Thread.sleep(250);
            } catch(InterruptedException e) {
                e.printStackTrace();
            }
        }

        this.setServerLock(true);

        MUD current = this.getMUD(client.getMUDName());
        boolean exists = current.existsThing(client.getUserLocation(), item);

        this.setServerLock(false);

        return exists;
    }

    // a method which is used to display the dungeon to the user
    public String displayDungeon(String name) {
        MUD temp = this.getMUD(name);
        return temp.toString();
    }

    public void playerForceShutdown(ClientImpl client) {
        MUD temp = this.getMUD(client.getMUDName());
        String msg = "\tUser [" + client.getUsername() + "] has left the instance [" + client.getMUDName() + "]. Inventory emptied." ;
        temp.removePlayer(client);
        this.notice(msg, false);

    }

    // used when picking up items, to detect if we try to pick up another player
    public boolean isUser(ClientImpl client, String item) {
        MUD temp = this.getMUD(client.getMUDName());
        if(temp.getPlayers().contains(item)) {
            return true;
        }
        return false;
    }

    private void setServerLock(boolean flag) {
        this.serverLock = flag;
    }

    private void setMaxPlayers(int maxPlayers) {
        this.maxPlayers = maxPlayers;
    }

    private void setMaxMUDs(int maxMUDs) {
        this.maxMUDS = maxMUDs;
    }

    private void setInventoryLimit(int limit) {
        this.inventoryLimit = limit;
    }

    // find and return a mud with a given name if it exists
    // also used on the Client side when a client forcefully exits out of the game
    private MUD getMUD(String name) {
        if(this.allMUD.containsKey(name)) {
            return this.allMUD.get(name);
        } else {
            System.err.println("Error, no such MUD exists.");
            return null;
        }
    }


    ServerImpl(int port_reg, int port_serv, int maxMUD, int maxPlayers, int maxInventory) throws RemoteException {
        System.out.println("");
        // create the registry connection
        LocateRegistry.createRegistry(port_reg);

        // set all variables to the passed args
        this.setMaxPlayers(maxPlayers);
        this.setMaxMUDs(maxMUD);
        this.setInventoryLimit(maxInventory);

        // create the server
        createServer(port_reg, port_serv);

        // System.out.println("Server created successfully");
    }
}