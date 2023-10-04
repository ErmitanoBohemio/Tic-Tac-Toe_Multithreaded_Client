/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Server;

import java.awt.BorderLayout;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.Formatter;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

/**
 *
 * @author OscarFabianHP
 */
//Server side of client/server Tic-Tac-Toe program
//Fig. 28.11

public class TicTacToeServer extends JFrame{
    
    private String[] board = new String[9]; //tic-tac-toe board
    private JTextArea outputArea; //for outputting moves
    private Player[] players; //array fo players
    private ServerSocket server; ////server socket to connect with clients
    private int currentPlayer; //keeps track of player with current move
    private final static int PLAYER_X = 0; //constant for first player
    private final static int PLAYER_O = 1; //constant for second player
    private final static String[] MARKS = {"X", "O"}; //array of marks
    private ExecutorService runGame; //will run player
    private Lock gameLock; //to lock game for synchronization
    private Condition otherPlayerConnected; //to wait for other player
    private Condition otherPlayerTurn; //to wait for other player's turn
    
    private boolean gameOver=false; //para indicar cuando el juego acaba sin ningun ganador (empate)
    private String winnerClient = "";
    private boolean isGameOver=false; //variable usada para indicar a cualquier Player si el juego concluyo (hubo ganador o empate) y asi no entrar a ciclo que valida movimientos una vez termine el juego
    private boolean playAgain=false;
    
    //set up tic-tac-toe server and GUI that displays messages
    public TicTacToeServer(){
        super("Tic-Tac-Toe Server"); //set title of window
        
        //create ExecutorService with a thread for each player
        runGame = Executors.newFixedThreadPool(2);
        gameLock = new ReentrantLock(); //create lock for game
        
        //condition variable for both players being connected
        otherPlayerConnected = gameLock.newCondition();
        
        //condition variable for the other player's turn
        otherPlayerTurn = gameLock.newCondition();
        
        for(int i=0; i<9; i++)
            board[i] = new String(""); //create tic-tac-toe board
        
        //create an instance of inner-class Player to process the client in a separate thread, these threads enable the clients to play the game independently
        players = new Player[2]; //create array of players
        //The first client to connect to the server is player X and the second is player O
        currentPlayer = PLAYER_X; //set current player to first player
        
        try {
            server = new ServerSocket(12345, 2); //set up ServerSocket
        } catch (IOException ioException) {
            ioException.printStackTrace();
            System.exit(1);
        }
        
        outputArea = new JTextArea(); //create JTextArea for output
        
        add(new JScrollPane(outputArea), BorderLayout.CENTER);
        outputArea.setText("Server awaiting connection\n");
        
        setSize(300, 300); //set size of window
        setVisible(true); //show window
    }
    
    //wait for two connections so game can be played
    public void execute(){
        //wait for each client to client
        for (int i = 0; i < players.length; i++) {
            try{ //wait for connection, create Player, start runnable
                players[i] = new Player(server.accept(), i);    
                runGame.execute(players[i]); //execute player runnable
            }
            catch(IOException ioException){
                ioException.printStackTrace();
                System.exit(1);
            }
        }
        gameLock.lock(); //lock game to signal player X's thread
        
        try{ //Esto permite que en el run de Player (el Player (X) pueda inciar moviendo al avisar que otro Player (O) se ha conectado a jugar)
            players[PLAYER_X].setSuspended(false); //resume player X
            otherPlayerConnected.signal(); //wake up player X's thread
        }
        finally{
            gameLock.unlock(); //unlock game after signalling player X
        }
    }
    
    //display message in outputArea
    private void displayMessage(final String messageToDisplay){
        //display message from event-dispatch thread of execution
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() { //updates outputArea
                outputArea.append(messageToDisplay); //add message
            }
        });
    }
    
    //determine if move is valid
    //allows only one player at a time to move, thereby preventing them from modifying the state information of the game simultaneously
    public boolean validateAndMove(int location, int player){
        while(player != currentPlayer){ //if the player attempting to validate a move is not the current player, its placed in a wait state until its turn to move
            gameLock.lock(); //lock game to wait for other player to go
            try {
                otherPlayerTurn.await(); //wait for player's turn
            } 
            catch (InterruptedException exception) {
                exception.printStackTrace();
            }
            finally{
                gameLock.unlock(); //unlock game after waiting
            }
        }
        
        //if location not occupied, make move
        if(!isOccupied(location)){
            board[location] = MARKS[currentPlayer]; //set move on board
            currentPlayer = (currentPlayer + 1) % 2; //change player
            
            //let new current player know that move occurred
            players[currentPlayer].otherPlayerMoved(location);
            
            gameLock.lock(); //lock game to signal other player to go
            
            try{
                otherPlayerTurn.signal();//signal other player to continue
                //invokes method signal so that the waiting Player (if there is one) can validate a move and return true to indicate that the move is valid
            }
            finally{
                gameLock.unlock(); //unlock game after signaling
            }
            return true; //notify player that move was valid
        }
        else //move was not valid
            return false; //notify player that move was invalid
            
    }

    //determine whether location is occupied
    public boolean isOccupied(int location) {
        if(board[location].equals(MARKS[PLAYER_X]) || 
                board[location].equals(MARKS[PLAYER_O]))
            return true; //location is occupied
        else
            return false; //location is occupied
    }
    
    //place code in  this method to determine whether game over
    //the program mantains board locations as number from 0 to 8 (0 through 2 for the first row, 
    //3 through 5 for the second row and 6 through 8 for the third)
    public boolean isGameOver(){
       System.out.println("isGameOver");
       boolean isGameOver=false;
        
        if( (board[0].equals("X") && board[1].equals("X") && board[2].equals("X")) ||
                (board[0].equals("O") && board[1].equals("O") && board[2].equals("O")) )//fila 0 
            isGameOver = true;
        else if( (board[3].equals("X") && board[4].equals("X") && board[5].equals("X")) ||
                (board[3].equals("O") && board[4].equals("O") && board[5].equals("O")) )//fila 1
            isGameOver = true;
        else if( (board[6].equals("X") && board[7].equals("X") && board[8].equals("X")) ||
                (board[6].equals("O") && board[7].equals("O") && board[8].equals("O")) )//fila 2
            isGameOver = true;
        else if( (board[0].equals("X") && board[4].equals("X") && board[8].equals("X")) || 
                 (board[0].equals("O") && board[4].equals("O") && board[8].equals("O")) )//diagonal izquierda
            isGameOver = true;
        else if( (board[2].equals("X") && board[4].equals("X") && board[6].equals("X")) ||
                (board[2].equals("O") && board[4].equals("O") && board[6].equals("O")) )//diagonal derecha
            isGameOver = true;
        else if( (board[0].equals("X") && board[3].equals("X") && board[6].equals("X")) ||
                (board[0].equals("O") && board[3].equals("O") && board[6].equals("O")) )//columna 0 
            isGameOver = true;
        else if( (board[1].equals("X") && board[4].equals("X") && board[7].equals("X")) ||
                (board[1].equals("O") && board[4].equals("O") && board[7].equals("O")) )//columna 1
            isGameOver = true;
        else if( (board[2].equals("X") && board[5].equals("X") && board[8].equals("X")) ||
                (board[2].equals("O") && board[5].equals("O") && board[8].equals("O")) )//columna 2
            isGameOver = true;
        else if(!"".equals(board[0]) && !"".equals(board[1]) && !"".equals(board[2]) && !"".equals(board[3]) && !"".equals(board[4]) && !"".equals(board[5]) 
                && !"".equals(board[6]) && !"".equals(board[7]) && !"".equals(board[8])){ //sin más espacio dispobible en el tablero del Tic-Tac-Toe
            gameOver=true; //se usa para indica si el fuego concluyo en empate
            isGameOver = true;
        }
        
        
        return isGameOver;
    }
    
    //metodo agregado para ejercio 28.20 
    private void advertisingClients(int opcion, int playerNumber) {
        
        if(opcion == -1){ //avisa a jugadores el resultado final del juego
            for(Player player:players)
                player.gameResult(winnerClient); 
        }
        //aqui se ejecuta para notificar que volveran a jugar
        if (opcion == 9) { //avisa a jugadores que nuevo juego comenzará
           // playAgain=true;
            for (Player player : players) {
                player.playAgainButtonPressed();
            resetGame(); //resetea tablero y demas variables del servidor para poder volver a jugar
            }
        } //aqui se ejecuta para notificar que un jugador salio y estara en espera de otro para volver a jugar
        else if (opcion == 10) { //avisa al otrp jugador que un jugador se ha retirado
            //int gonePlayer = Arrays.asList(MARKS).indexOf(client); //obtiene jugador que ha salido del juego
            //players[(playerNumber+1)%2].terminateGameButtonPressed(playerNumber); //avisa al otro jugador que el otro jugador termino el juego
            for (Player player : players) 
                player.terminateGameButtonPressed(playerNumber);
    }
    }
    
    //metodo para ejecicio 28.20 punto B
    //resetea las variables del servidor necesarias para empezar una nueva partida del juego Tic-Tac-Toe
    private void resetGame(){
        currentPlayer = PLAYER_X;
        gameOver=false; //reseta variable de empate
        isGameOver=false; //resetea variable de final de juego
        playAgain=true;
        winnerClient="";
        //resetea tablero usado por servidor para saber avance del juego
        for(int i=0; i<9; i++)
            board[i] = new String(""); //create tic-tac-toe board
    }
    
    
    private void esperarTurno(int player){
        while(player != currentPlayer){ //if the player attempting to validate a move is not the current player, its placed in a wait state until its turn to move
            gameLock.lock(); //lock game to wait for other player to go
            try {
                otherPlayerTurn.await(); //wait for player's turn
            } 
            catch (InterruptedException exception) {
                exception.printStackTrace();
            }
            finally{
                gameLock.unlock(); //unlock game after waiting
            }
        }
    }
    
    private void asignarTurno(){ 
            gameLock.lock(); //lock game to signal other player to go
            
            try{
                otherPlayerTurn.signal(); //signal other player to continue
                //invokes method signal so that the waiting Player (if there is one) can validate a move and return true to indicate that the move is valid
            }
            finally{
                gameLock.unlock(); //unlock game after signaling
            }
    }
   

    //private inner class Player manages each Player as a runnable
    private class Player implements Runnable{

        private Socket connection; //connection to client
        private Scanner input; //input from client
        private Formatter output; //output to client
        private int playerNumber; //tracks which player this is
        private String mark; //mark for this player 
        private boolean suspended = true; //whether thread is suspended
        
        //set up Player thread
        public Player(Socket socket, int number) {
            playerNumber = number; //store this player's number
            mark = MARKS[playerNumber]; //specify player's mark
            connection = socket; //store socket for client
            
            try { //obtain streams from socket
                input = new Scanner(connection.getInputStream());
                output = new Formatter(connection.getOutputStream());
            } catch (IOException ioException) {
                ioException.printStackTrace();
                System.exit(1);
            }
        }
        
        //send message that other player moved
        public void otherPlayerMoved(int location){
            output.format("Opponent moved\n");
            output.format("%d\n", location); //send location of move
            output.flush();
            
        }

        //controlthread's execution
        //controls the information that send to and received from the client
        @Override
        public void run() {
            try { //send client its mark (X or O), process messages from client
                displayMessage("Player " + mark + " Connected\n");
                output.format("%s\n", mark); //send player's mark
                output.flush(); //flush output

                if (playerNumber == PLAYER_X) {
                    output.format("%s\n%s", "Player X Connected", "Waiting for another player\n");
                    output.flush(); //flush output

                    gameLock.lock(); //lock game to wait for second player

                    try {
                        while (suspended) { //suspendes player X thread as it starts executing, because player X can move only after player O connects

                            otherPlayerConnected.await(); //wait for player 0
                        }
                    } catch (InterruptedException exception) {
                        exception.printStackTrace();
                    } finally {
                        gameLock.unlock(); //unlock game after second player
                    }

                    //send message that other player connected
                    //when player O connects, the game can be played, and the run method begins executing its while statement
                    output.format("Other player connected. You move. \n");
                    output.flush(); //flush output
                } else {
                    output.format("Player 0 connected, please wait\n");
                    output.flush();
                }

            boolean salir=false; //agregado ejercicio 28.20
            while(true){    
                
                //parte ejemplo libro
                //while game not over
                //isGameOver=isGameOver();
                //while (!isGameOver()) {
                esperarTurno(playerNumber);
                
                while (!isGameOver) {
                    System.out.println("playing "+playerNumber);
                    int location = 0; //initialize move location
            
                    if (input.hasNext() && winnerClient.equals("") && gameOver==false) { //each iteration of this loop reads an integer, representing the location where the client wants to place mark
                        location = input.nextInt(); //get move location
                        if(location>8){
                            //String markPlayer = input.next();
                            advertisingClients(location, playerNumber); //alerta cliente y servidor que abandona juego
                            displayMessage("\nPlayer " + MARKS[playerNumber] + " has abandoned the game\n");
                            salir=true;
                            break;
                        }
                    
            
                    
                    //check for valid move
                    if (validateAndMove(location, playerNumber)) {
                        if(winnerClient.equals("")){ //si el juego aun no tiene ganador
                            displayMessage("\nlocation: " + location);
                            output.format("Valid move.\n"); //notify client
                            output.flush(); //flush output
                            asignarTurno();
                        }
                        else //si tiene ganador salga del ciclo
                            break;
            
                    } else //move was invalid
                    {
                        if(winnerClient.equals("")){ //si juego aun no tiene ganador
                            output.format("Invalid move, try again\n");
                            output.flush(); //flush output
                        }
                        else //si tiene ganador sale del ciclo
                            break;
                    }
                    //isGameOver=isGameOver();
                    
                }
                    else
                        break;
                    isGameOver=isGameOver();
                }

                System.out.println("salida ciclo "+playerNumber);
                if(salir){
                    System.out.println("salir break!!!");
                    break;
                }
                //verifica si el juego no quedo empatado, para asignar al ganador (primero en salir del ciclo isGameOver)
                if(gameOver==false && winnerClient.equals("")){
                    winnerClient = MARKS[playerNumber]; //guarda jugador con ultima movida, se usa para saber quien realizo el posible movimiento ganador
                    advertisingClients(-1, playerNumber); //muestra resultado del juego
                    playAgain=false;
                    //gameOver=false;
                }
                else if(gameOver){ //si juego ha quedado empatado
                    advertisingClients(-1, playerNumber);
                    playAgain=false;
                }
                
                int opcionUser=0;
                while(!playAgain){
                //do{System.out.println("ciclo botones "+playerNumber);
                  //  if(!playAgain)
                  System.out.println("ciclo botones "+playerNumber);
                        if (input.hasNext()){
                            opcionUser = input.nextInt();
                            //if(!playAgain){
                                advertisingClients(opcionUser, playerNumber);
                                //playAgain=true; 
                            }
                            //else
                              //  break;
                        //}
                        else
                            break;
                            //if(playAgain)
                             //   break;
                }//while(opcionUser<8);
                //if(playAgain)
                  
                //asignarTurno(); //libera turno para poder jugar sin problemas y empezar de cero
                //playAgain=true; //sive para indicar que ya uno de los clientes presiono boton de jugar otra vez
                System.out.println("salio ciclo botones "+playerNumber);
    
            }
                System.out.println("salida salida!!!");
            } finally {
                try {
                    connection.close(); //close connection to client
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                    System.exit(1);
                }
            }
        }
        
        //set whether or not thread is suspended
        public void setSuspended(boolean status){
            suspended = status;
        }
        
        //metodos agragados para implementar ejercicio 28.20 punto B
        //envia al client el mensaje para alertarlo que alguien presiono el boton Jugar otra véz
        public void playAgainButtonPressed(){
            output.format("Lets play again\n");
            output.flush();
        }
        
        //metodo para ejercicio 28.20 punto C
        //envia al client el mensaje para avisarle que el otro jugador se retiro del juego
        public void terminateGameButtonPressed(int playerNumber){
            output.format("Terminated game by player " + MARKS[playerNumber] + "\n");
            output.flush();
        }
        
        //Metodo para ejercion 28.20 punto A
        //envía mensaje al cliente para avisar el resultado del juego, ya sea elganador o empate del juego
        public void gameResult(String winner){
            if (gameOver){
                    output.format("neither won\n");
                    output.flush();
                }
                else{
                    output.format("Winner Player " + winner+"\n");
                    output.flush();
                }
        }
    }
}