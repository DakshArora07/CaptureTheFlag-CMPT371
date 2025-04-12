# CMPT 371 Group 7: Capture the Flag

A real-time multiplayer Capture the Flag game implemented in Java with JavaFX. Players join either the red or blue team and compete to capture flags in a maze-like arena.

## Features

- **Real-time Multiplayer**: Connect multiple clients to a central server over TCP/IP
- **Team-based Gameplay**: Join the red or blue team and compete to capture flags
- **Dynamic Grid Environment**: Navigate through a maze-like arena with walls and flags
- **Flag Capture Mechanics**: Hold 'C' between 3 to 4 seconds to capture a flag
- **Smooth Player Movement**: Use WASD keys for responsive movement
- **Visual Feedback**: Color-coded teams with player names and status indicators
- **Game State Management**: Server tracks player positions, flag statuses, and win conditions
- **Lobby System**: Join existing games or host your own
- **Resilient Networking**: Handles player connections and disconnections gracefully

## Prerequisites

- Java 17 or higher
- [Maven](https://maven.apache.org) for building the project
- JavaFX (included in the Maven dependencies)


## Building the Project

To build the project, use the following Maven command:
``` bash
mvn clean package
```

This will test and compile the project and ensure that all required dependencies are downloaded. Then, this will generate a jar file in the target/ directory, which can be used to distribute or run the game.


You can also use the following Maven command:

``` bash
mvn clean install
```

This will do the same as the previous command, and will also put the jar file and other files in your local repository.

## Running the Game

Once the project is built, you can run the maze game using the following Maven command after building:

``` bash
mvn clean javafx:run
```

This will start the game, and you will be able to interact with it.


You can also run the game from the executable jar file by opening the directory of the jar file in terminal and using the following command:

``` bash
java -jar Game-1.0-SNAPSHOT.jar
```


## How to Run

### Option 1: Hosting a New Game

- In the main menu, click **"NEW GAME"**
- The application will display your IP address
- Share this IP with players who want to join your game
- Click "Start Game" to host the server and enter the waiting room
- The game will automatically start once 4 players have joined

### Option 2: Joining an Existing Game

- In the main menu, click **"JOIN GAME"**
- Enter the host's IP address in the provided field
- Click "Join" to connect to the host's game

## How to Play

1. **Team Selection**: Choose to join either the red or blue team
2. **Enter Your Name**: Create a player name (2 characters maximum)
3. **Game Controls**:
   - **W**: Move up
   - **A**: Move left
   - **S**: Move down
   - **D**: Move right
   - **C**: Hold to capture a flag (must hold between 3 to 4 seconds while standing on a flag)

4. **Objective**:
   - Capture more flags than the opposing team
   - The first team to capture 4 flags wins
   - Avoid enemy players who can interrupt your capture attempts

## Game Mechanics

- **Flags**: Represented by flag icons on the map
- **Capturing**: Stand on a flag and hold 'C' between 3 to 4 seconds to capture it
- **Team Bases**: Each team has spawn points where players respawn after unsuccessful capture attempts
- **Collision**: Players cannot move through walls or other players (except on flag positions)
- **Flag Status**: Captured flags change color to indicate which team captured them

## Project Structure

- `src/main/java/sfu/cmpt371/group7/game/`
   - `client/`: Client-side code for UI and player interaction
      - `Console.java`: Initial team/name selection screen
      - `Maze.java`: Main game area with grid, movement, and flag capture logic
      - `Menu.java`: Main menu for hosting or joining games
      - `Results.java`: End-game results display
   - `model/`: Game entity models
      - `Flag.java`: Represents flags that can be captured
      - `Player.java`: Represents players with team, position, and status
   - `server/`: Server-side code for game management
      - `Server.java`: Manages connections, game state, and win conditions
   - `Game.java`: Main entry point for the application

## Network Protocol

The game uses a simple text-based protocol over TCP for client-server communication for eg:

- `teamSelection <team> <name>`: Player selects a team and provides a name
- `movePlayer <name> <x> <y>`: Updates a player's position
- `captureDuration <name> <flagName> <duration>`: Attempts to capture a flag
- `flagCaptured <name> <flagName>`: Notifies that a flag was captured
- `gameOver <winner>`: Indicates the game has ended with a winner


## Troubleshooting

- **Connection Issues**: Ensure firewall settings allow connections on port 65000
- **Display Problems**: Verify that your system meets JavaFX requirements
- **Build Errors**: Check that you have Java 17+ and Maven properly installed

## Contributing
Since this project is part of a university course group project, contributions were made by team members only.

## License
This project is not open-source and is part of a private course project at Simon Fraser University. Unauthorized redistribution or modification is prohibited.

## Acknowledgements
This game was developed by Ayush Arora, Daksh Arora, Hetmay Ketan Vora and Kabir Singh Sidhu,
students at Simon Fraser University as part of the CMPT371: Data communication and networking. Special thanks to our course instructors and teaching assistants for their guidance throughout the project.


--- 

Happy gaming folks ! 🎮
```
