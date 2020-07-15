import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.net.ConnectException;
import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

class ClientImpl implements ClientInterface, Serializable {
    // information about the server
    private ServerInterface remoteServer;
    private String hostname;
    private int port;

    // name of the dungeon
    private String mudName = "";

    // information about the client
    private String username;
    private String location;
    private List<String> inventory = new ArrayList<>();
    private boolean playing;    // used to indicate if the player is currently playing in a mud

    // used to display changes to the mud world to a user
    public String notice = "";

    // getters
    public String getUsername() {
        return this.username;
    }

    public String getUserLocation() {
        return this.location;
    }

    public String getMUDName() {
        return this.mudName;
    }

    public List<String> getInventory() {
        return this.inventory;
    }

    // setters (about server and then user)
    private void setHost(String name) {
        this.hostname = name;
    }

    private void setPort(int port) {
        this.port = port;
    }

    private void setUsername(String name) {
        this.username = name.replace(" ","");   // make sure there are no spaces in the username
    }
    
    private void setLocation(String location) {
        this.location = location;
    }

    private void setInventory() throws RemoteException {
        this.inventory = this.remoteServer.setPlayerInventory();
    }

    private void setMUDName(String name) {
        this.mudName = name;
    }

    private void menu() throws RemoteException {
        try {
            // while the player is not in a game, display the menu
            if(!this.playing) {
                System.out.println(this.remoteServer.menu());
            }

            String command = this.chooseAction("").toLowerCase().trim();

            while(command.startsWith("/")) {
                String[] newCommand = command.split(" ");
                if(newCommand[0].equals("/create")) {
                    this.createDungeonInstance(newCommand[1]);
                } else if(newCommand[0].equals("/join")) {
                    this.joinDungeonInstance(newCommand[1]);
                } else if(newCommand[0].equals("/disconnect")) {
                    this.disconnectFromServer();
                } else if(newCommand[0].equals("/1")) {
                    this.listMUDS();
                } else if(newCommand[0].equals("/2")) {
                    this.listUsers();
                } else if(newCommand[0].equals("/menu")) {
                    System.out.println(this.remoteServer.menu());
                } else if(newCommand[0].equals("/inv")) {
                    this.openInventory();
                } else {
                    System.err.println("Error, command does not exist.");
                }
                command = this.chooseAction("").toLowerCase().trim();
            }
            System.err.println("Error, command must start with '/'.");   
        } catch(NullPointerException e) {
            assert true;
        }
    }

    private void establishServerConnection() throws RemoteException {
        try {
            String url = "rmi://" + this.hostname + ":" + this.port + "/mud";
            this.remoteServer = (ServerInterface) Naming.lookup(url);
        } catch(NotBoundException e) {
            System.err.println("Not bound url: " + e.getMessage());
        } catch(MalformedURLException e) {
            System.err.println("Malformed url: " + e.getMessage());
        }
    }

    // This method should disconnect the user from the server
    // and also remove him from any MUDs 
    public void abort() throws RemoteException {
        this.disconnectFromServer();
        System.err.println("User aborted. Shutting app.");
    }
    
    // a method which is used when we need the user to choose an action
    // we have to pass it a string (it could be empty)
    // if we do, the string we've passed is displayed (sort of like an info message)
    // if we do not, a generic <username> -> is displayed and waits for an action
    // this is basically acting like a wrapper around the built in Scanner class
    // but I prefer to use bufferedreader instead 
    private String chooseAction(String msg) {
        if(!msg.equals("")) {
            System.out.print(msg);
        } else {
            System.out.print(this.username + ": ");
        }
        BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
        try {
            return input.readLine();
        } catch(IOException e) {
            System.err.println("Error, wrong action: " + e.getMessage());
            return chooseAction(msg);
        }
    }

    // a method that handles logging in to the game server
    /**
     * This can be done in a more compact way with less remote calls
     * Will make sure to fix it if I have time left.
     * What this does -> it checks to see if the server is full
     * if it is not -> makes a remote call to the log in method and logs in the user
     * if it is -> makes a remote call to the same method but this time it enters 
     * the block which handles when the server is full. Then every 5 seconds (so as to
     * not overload the server) we make a call to the same method to see if room has been freed.
     */
    private void joinGameServer() throws RemoteException {
        boolean isServerFull = this.remoteServer.isServerFull();
        if(isServerFull) {
            System.out.println("Server full. You will be added to the waiting list");
            this.remoteServer.playerLogIn(this);
            while(!this.remoteServer.playerLogIn(this)) {
                try {
                    System.out.println("Waiting for authorization. . .");
                    Thread.sleep(5000);
                } catch(InterruptedException e) {
                    return;
                }
            }
        } else {
            this.remoteServer.playerLogIn(this);
        }
        this.setInventory();    // set up the players inventory (empty array of [ ])
        System.out.println("You have joined the server " + this.hostname);
    }

    private void disconnectFromServer() throws RemoteException {
        System.out.println("Leaving the server. . .");
        this.remoteServer.playerDisconnect(this);
        this.remoteServer = null;
        this.hostname = null;
        this.port = 0;  // cannot set integer to null
        this.username = null;
    }

    // create a MUD
    private void createDungeonInstance(String name) throws RemoteException {
        boolean exists = this.remoteServer.mudExists(name);
        boolean noSpot = this.remoteServer.spotForMUD();
        if(exists) {
            System.err.println("Error, MUD game with that name already exists.");
        } else if(noSpot) {
            System.err.println("Error, maximum number of MUDs created.");
        } else {
            /**
             * Every player can set a limit of users in his created MUD
             * Maybe come back and change that so that all MUDs
             * have the same limit (set by the programmer)
             * Tried using a Scanner here but it messes up with IntelliJ
             * Advice taken from https://stackoverflow.com/questions/14359930/in-java-how-do-i-check-if-input-is-a-number
             */
            String input = this.chooseAction("Maximum number of players: ");
            int num = 2;    // default
            try {
                num = Integer.parseInt(input);
            } catch(NumberFormatException e) {
                System.err.println("Error, wrong input.");
                this.menu();
            }
            num = Math.abs(num);    // ensure no negative numbers are processed
            this.remoteServer.createDungeonInstance(name, num, this);
            System.out.println("Dungeon created.");
            /**
             * think about immediately adding the user to a dungeon
             * they have created
             */
            // this.joinDungeonInstance(name);
            this.menu();
        }
    }

    // TODO: add a queue 
    private void joinDungeonInstance(String name) throws RemoteException {
        boolean exists = this.remoteServer.mudExists(name);
        if(exists) {
            this.setMUDName(name);
            // proceed with gameplay
                        
            // used if and only if the user forcefully exits out of the game
            ClientImpl user = this;
            // just wrapping it in comments so it's clear

            // add a shutdown hook which handles unexpected shutdowns
            // such as the user pressing ctrl+c
            // it intercepts the signal, handles it appropriately (empties the inventory, disconnects the user)
            // and then proceeds to shut the application down
            Runtime.getRuntime().addShutdownHook(new Thread() {
                public void run() {
                    try {
                        System.out.println("\nForcefully shutdown. . . Emptying inventory and data is deleted.");
                        user.remoteServer.playerForceShutdown(user);    // call a remote method which removes the players data from the mud instance
                        user.disconnectFromServer();    // disconnect the user from the server
                        return;
                    } catch(RemoteException e) {
                        e.printStackTrace();
                    }
                } 
            });
            this.gameLoop();
        } else {
            System.err.println("Error, no instance matches the given name.");
        }
    }

    private void gameLoop() throws RemoteException {
        // main game loop
        // if the player cannot join a dungeon instance, display a message and return to the main menu
        if(!this.remoteServer.playerJoinDungeon(this, this.getMUDName())) { // playerJoinDungeon returns a boolean, if it's false it means the player hasn't(can't) join the specified mud
            System.err.println("Error, cannot join the specified dungeon instance. Try again later.");
            this.menu();
        } else {
            // player has successfully joined the dungeon
            System.out.println("You have successfully joined " + this.getMUDName());
            this.playing = true;    // flag to signal the user is playing
            this.gameplayInfo();    // display information about commands
            this.setLocation(this.remoteServer.setStartLocation(this)); // set the start location
            System.out.println("Start location has been set to [" + this.getUserLocation() + "].");
            // this.displayMUD();  // used to display the mud for the user (for debugging)

            while(this.playing) {   // the main loop
                
                String command = this.chooseAction("").toLowerCase().trim();    // ask for command

                if(command.startsWith("/")) {
                    if(!this.notice.equals("")) {
                        System.out.println(this.notice);
                    }
                    String[] newCommand = command.split(" ");
                    if(newCommand[0].equals("/help")) {
                        this.gameplayInfo();
                    } else if(newCommand[0].equals("/move")) {
                        if(newCommand[1].matches("north|east|south|west")) {
                            this.playerMove(newCommand[1]);
                        } else {
                            System.err.println("Error, wrong direction.");
                        }
                    } else if(newCommand[0].equals("/display")) {
                        this.displayMUD();  // TODO: REMOVE THIS IN FINAL BUILD. USED FOR DEBUGGING
                    } else if(newCommand[0].equals("/look")) {
                        System.out.println(this.playerLook());
                    } else if(newCommand[0].equals("/exit")) {
                        this.remoteServer.playerForceShutdown(this);
                        System.out.println("You have left the dungeon [" + this.getMUDName() + "].");
                        this.playing = false;
                        this.setInventory();
                        this.menu();
                    } else if(newCommand[0].equals("/pick")) {
                        this.playerPickUp(newCommand[1]);
                    } else if(newCommand[0].equals("/inventory")) {
                        this.openInventory();
                    }
                } else {
                    System.err.println("Error, invalid command. Try again.");
                }
            }

        }
        System.out.println("exiting game loop");
    }

    private void playerPickUp(String thing) throws RemoteException {
        boolean exists = this.remoteServer.itemExists(this, thing);
        boolean hasFreeSpace = this.hasFreeInventorySpace();
        boolean isUser = this.remoteServer.isUser(this, thing);

        if(exists && hasFreeSpace) {
            System.out.println("Picking up an item. . .");
            for(int i = 0; i < this.getInventory().size(); i++) {
                if(this.getInventory().get(i).equals("[ ]")) {
                    this.getInventory().set(i, "[" + thing + "]");
                    System.out.println("Item [" + thing + "] picked up.");
                    this.remoteServer.itemPickedUp(this, thing);
                    break;
                }
            }
        } else if(!hasFreeSpace){
            System.out.println("You do not have free space in your inventory.");
        } else if(isUser) {
            System.out.println("Fellow adventurers prefer to not be picked up.");
        } else {
            System.out.println("Looking around, you cannot seem to find such an item.");
        }
    }

    // return true if the player has a free spot in their inventory
    private boolean hasFreeInventorySpace() {
        return this.getInventory().contains("[ ]");
    }

    // display information about the current location the player is in
    private String playerLook() throws RemoteException {
        return this.remoteServer.locationInfo(this);
    }

    // handle player movement around the mud
    private void playerMove(String dest) throws RemoteException {
        this.setLocation(this.remoteServer.playerMove(this, dest)); // set the location
        System.out.println("Your new location is [" + this.getUserLocation() + "].");
        this.playerLook();  // display information about the new location
    }

    // only used for debugging; will remove later
    private void displayMUD() throws RemoteException {
        System.out.println(this.remoteServer.displayDungeon(this.getMUDName()));
    }

    // display the players inventory
    private void openInventory() {
        String inventory = "";

        for(String space : this.getInventory()) {
            inventory += space + " ";
        }

        System.out.println("Inventory: " + inventory);
    }

    // list all existing dungeon instances
    private void listMUDS() throws RemoteException {
        System.out.println(this.remoteServer.listCurrentMUDs(false));
    }

    // list all users
    private void listUsers() throws RemoteException {
        System.out.println(this.remoteServer.listCurrentPlayersServer());
    }

    private void gameplayInfo() {
        System.out.println(
            "\t\t\t|HELP|\n" + 
            "\t|If you wish to move\n" + 
            "\t|Move North: \t\t /move north\n" + 
            "\t|Move East: \t\t /move east\n" +
            "\t|Move South: \t\t /move south\n" +
            "\t|Move West: \t\t /move west\n" +
            "\t|~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~|\n" +
            "\t|If you wish to interact with the world\n" +
            "\t|Look around: \t\t /look\n" +
            "\t|Pick an item: \t\t /pick <item>\n" + 
            "\t|Open inventory: \t\t /inventory\n" +
            "\t|~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~|\n" +
            "\t|If you wish to interact with the game\n" +
            "\t|Exit the instance: \t\t /exit\n" +
            "\t|Others in dungeon: \t\t /who\n" +
            "\t|Help menu: \t\t /help\n"
            );
    }

    ClientImpl(String _hostname, int _port) throws RemoteException {
        System.setProperty("java.security.policy" , ".policy");
        System.setSecurityManager(new SecurityManager());
        try {
            this.setHost(_hostname);    // set the host
            this.setPort(_port);    // set the port
            System.out.println("Hostname: " + this.hostname + " on port: " + this.port + "\n"); // info for debugging purpose
            this.establishServerConnection(); // establish a connection to the server (sets up RMI)
            // next couple of lines handle "registering"
            // ask the user for a name, check if it already exists in the server
            // if it does, ask again
            // if it doesn't, set it and proceed
            this.setUsername(this.chooseAction("Enter a username: "));
            while(this.remoteServer.playerExists(this)) {
                System.out.println("A user with this name already exists. Choose a different one.");
                this.setUsername(this.chooseAction("Enter a username: "));
            }
            this.joinGameServer();
            this.menu();
        } catch(NullPointerException e) {
            this.abort();;
        }
    }

}






