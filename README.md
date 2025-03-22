## CMPT 371 Group Project: Capture the Flag

This repository implements a simple multiplayer Capture the Flag game written in Java. Players connect to a central server, choose a team, and try to capture flags in a maze-like arena.

### Features
- **Multiplayer**: Connect multiple clients to a single server.
- **JavaFX UI**: Displays a grid-based maze, animated startup, and a 3-minute countdown.
- **Maven-based**: Easy to build and run with Maven.

### Prerequisites
- Java 17 or higher
- [Maven](https://maven.apache.org)

### How to Run
1. **Clone and navigate** to the project folder:
   ```bash
   git clone git@github.com:DakshArora07/CMPT371-Group7.git
   ```
2. **Set environment variables** (for example in `var.env`):  
   ```
   ADDRESS = "localhost" OR "<IP_ADDRESS>"
   PORT_NUMBER = 12345
   ```
3. **Build** with Maven:
   ```bash
   mvn package
   ```
4. **Start the server** by running the main class in `Server.java`:
   ```bash
   mvn exec:java -Dexec.mainClass=sfu.cmpt371.group7.game.server.Server
   ```
   It will listen for incoming client connections on the specified port.
5. **Launch the client** by running the `Console` application:
   ```bash
   mvn exec:java -Dexec.mainClass=sfu.cmpt371.group7.game.ui.Console
   ```
   You will be prompted to create or join a team.

Enjoy your Capture the Flag game! Feel free to customize the UI or logic as needed.