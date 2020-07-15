# A simple CLI multiplayer game in Java

## Instructions
* Once extracted, use the _**make**_ command in the terminal in the _src_ folder
* To start the server execute: _java Server **portnumber** **registrynumber** **maxgames** **maxplayers**_ 
  * An example would look like this: _java Server 50015 0 5 16_
* To start a client execute: _java Client **hostname** **portnumber**_ 
  * **Note:** The hostname takes the name of your machine so use that.

## Functionality
This application has been developed utilizing multithreading and the Java Remote Method Invocation API.

Whenever anything happens in the game, everything is logged to the server. In this case, it can be seen in the
terminal on which the server was started.
Users can do the following:
* Register with a unique name.
* Create, remove, join and exit MUD games.
* Move around once a game is entered.
* Interact with the world -> pick up items, open invetory, look around.
* See if there are other players in the world.
* Open a menu with help.

Utilizing multithreading I have implemented a sort of queue when joining the server or a game. If a user tries to enter a full server/game they will receive a message and be added to the queue. After some time a thread will make an attempt to join again untill there is a free spot.
**Note:** As deadlocks and race conditions are a big issue when using multiple threads I have implement locks which ensure that processes will not overwrite eachother and everything can run concurrently without any issues.
