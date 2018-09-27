import java.awt.*;
import java.util.*;
import java.awt.event.*;
import java.lang.*;
import java.io.*;
import java.net.*;
import javax.swing.*;
import java.math.*;
import java.text.*;

class SmartGuy {

    public Socket s;
	public BufferedReader sin;
	public PrintWriter sout;
    Random generator = new Random();

    double t1, t2;
    int me;
    int boardState;
    int state[][] = new int[8][8]; // state[0][0] is the bottom left corner of the board (on the GUI)
    int turn = -1;
    int round;
    
    int validMoves[] = new int[64];
    int numValidMoves;
    
    
    // main function that (1) establishes a connection with the server, and then plays whenever it is this player's turn
    public SmartGuy(int _me, String host) {
        me = _me;
        initClient(host);

        int[] myValueAndMove = new int[2];

        System.out.println("SMARRRRRRTTTTTYYYYYY I S PLAYING");
        
        while (true) {
            System.out.println("Read");
            readMessage();
            
            if (turn == me) {
                System.out.println("Move");
                getValidMoves(round, state);
                
                myValueAndMove = move();
                int myMove = myValueAndMove[1];
                int value = myValueAndMove[0];
                //myMove = generator.nextInt(numValidMoves);        // select a move randomly
                
                String sel = validMoves[myMove] / 8 + "\n" + validMoves[myMove] % 8;
                
                System.out.println("Selection: " + validMoves[myMove] / 8 + ", " + validMoves[myMove] % 8 + " -> " + value);
                
                sout.println(sel);
            }
        }
        //while (turn == me) {
        //    System.out.println("My turn");
            
            //readMessage();
        //}
    }
    
    // You should modify this function
    // validMoves is a list of valid locations that you could place your "stone" on this turn
    // Note that "state" is a global variable 2D list that shows the state of the game
    private int[] move() {
        // just move randomly for now
        int[] myValueAndMove = getBestMove(true, numValidMoves, validMoves, 1);

        return myValueAndMove;
    }

    // Return int[0] -> the best value, and int[1] -> the best move
    private int[] getBestMove(boolean isMaximizer, int numMovesForPlayer, int[] movesForPlayer, int numLevelsLeft) {

      // If we want to stop at this level
      if (numLevelsLeft == 1) {
        if (isMaximizer) {

          int bestValueAndMove[] = new int[2];
          bestValueAndMove[0] = Integer.MIN_VALUE;
          bestValueAndMove[1] = 0;

          for (int a = 0; a < numMovesForPlayer; ++a) {
            int currentMove = movesForPlayer[a];
            int expectedValue = evaluateMove(isMaximizer, numMovesForPlayer, currentMove);

            if (expectedValue > bestValueAndMove[0]) {
              bestValueAndMove[0] = expectedValue;
              bestValueAndMove[1] = a;
            }
          }

          return bestValueAndMove;

        } else {

          int worstValueAndMove[] = new int[2];
          worstValueAndMove[0] = Integer.MAX_VALUE;
          worstValueAndMove[1] = 0;

          for (int a = 0; a < numMovesForPlayer; ++a) {
            int currentMove = movesForPlayer[a];
            int expectedValue = evaluateMove(isMaximizer, numMovesForPlayer, currentMove);

            if (expectedValue < worstValueAndMove[0]) {
              worstValueAndMove[0] = expectedValue;
              worstValueAndMove[1] = a;
            }
          }

          return worstValueAndMove;
        }
      }

      int alpha = 0;
      int beta = 0;

      // If this is a maximizer player (the computer)
      if (isMaximizer) {

        int bestValueAndMove[] = new int[2];
        bestValueAndMove[0] = Integer.MIN_VALUE;
        bestValueAndMove[1] = 0;

        // If player cannot make a move
        if (numMovesForPlayer == 0) {

          // Then this value is worth nothing
          int currentMoveValue = 0;

          // Then just evaulate the moves for the opponent and get thier expectedValue
          int[] opponentMoves = new int[64];
          int numOpponentMoves = 0;

          //Evaluate moves for opponent, num moves, and decrement levels left
          for (int i = 0; i < 8; i++) {
              for (int j = 0; j < 8; j++) {
                  if (state[i][j] == 0) {
                      if (couldBe(false, state, i, j)) { // Check opponents possible moves
                          validMoves[numValidMoves] = i*8 + j;
                          numValidMoves ++;
                          System.out.println(i + ", " + j);
                      }
                  }
              }
          }

          int[] expectedValueAndMove = getBestMove(!isMaximizer, numOpponentMoves, opponentMoves, numLevelsLeft - 1);

          if (expectedValueAndMove[0] + currentMoveValue > bestValueAndMove[0]) {
            bestValueAndMove[0] = expectedValueAndMove[0] + currentMoveValue;
            bestValueAndMove[1] = 0;
          }

          // TODO: What to return on no possible move? 0th move?
          return bestValueAndMove;
          
        } else {
          for (int a = 0; a < numMovesForPlayer; ++a) {

            // Update the game board if this move were to be done
            int currentMove = movesForPlayer[a];
            state[currentMove / 8][currentMove % 8] = me;

            int currentMoveValue = evaluateMove(isMaximizer, numMovesForPlayer, currentMove);

            int[] opponentMoves = new int[64];
            int numOpponentMoves = 0;

            //Evaluate moves for opponent, num moves, and decrement levels left
            for (int i = 0; i < 8; i++) {
                for (int j = 0; j < 8; j++) {
                    if (state[i][j] == 0) {
                        if (couldBe(false, state, i, j)) { // Check opponents possible moves
                            opponentMoves[numOpponentMoves] = i*8 + j;
                            numOpponentMoves ++;
                            System.out.println(i + ", " + j);
                        }
                    }
                }
            }

            int[] expectedValueAndMove = getBestMove(!isMaximizer, numOpponentMoves, opponentMoves, numLevelsLeft - 1);

            if (expectedValueAndMove[0] + currentMoveValue > bestValueAndMove[0]) {
              bestValueAndMove[0] = expectedValueAndMove[0] + currentMoveValue;
              bestValueAndMove[1] = a;
            }
          
            // Remove the move that would be done down this path
            state[currentMove / 8][currentMove % 8] = 0;

          }

          return bestValueAndMove;
        }
      } else { // If this is a minimizer player (the human player)
        
        int worstValueAndMove[] = new int[2];
        worstValueAndMove[0] = Integer.MAX_VALUE;
        worstValueAndMove[1] = 0;

        for (int a = 0; a < numMovesForPlayer; ++a) {
          int currentMove = movesForPlayer[a];

          if (me == 1) {
            state[currentMove / 8][currentMove % 8] = 2;
          } else {
            state[currentMove / 8][currentMove % 8] = 1;
          }

          int currentMoveValue = evaluateMove(isMaximizer, numMovesForPlayer, currentMove);

          int[] computerMoves = new int[64];
          int numComputerMoves = 0;

          //Evaluate moves for opponent, num moves, and decrement levels left
          for (int i = 0; i < 8; i++) {
              for (int j = 0; j < 8; j++) {
                  if (state[i][j] == 0) {
                      if (couldBe(true, state, i, j)) { // Check computer's next possible moves
                          computerMoves[numComputerMoves] = i*8 + j;
                          numComputerMoves ++;
                          System.out.println(i + ", " + j);
                      }
                  }
              }
          }

          int[] expectedValueAndMove = getBestMove(!isMaximizer, numComputerMoves, computerMoves, numLevelsLeft - 1);

          if (expectedValueAndMove[0] + currentMoveValue < worstValueAndMove[0]) {
            worstValueAndMove[0] = expectedValueAndMove[0] + currentMoveValue;
            worstValueAndMove[1] = a;
          }

          // Remove the move that would be done down this path
          state[currentMove / 8][currentMove % 8] = 0;
        }

        return worstValueAndMove;
      }
    }

    private int evaluateMove(boolean isMaximizer, int numMovesForPlayer, int desiredMove) {

      int row = desiredMove / 8;
      int column = desiredMove % 8;

      // Corners are the best
      if (desiredMove == 0 || desiredMove == 7 || desiredMove == 56 || desiredMove == 63) {
        if (isMaximizer) {
          return 10;
        } else {
          return -10;
        }
      }

      //Edges are great value
      if (row == 0 || row == 7 || column == 0 || column == 7) {
        if (isMaximizer) {
          return 8;
        } else {
          return -8;
        }
      }

      // If none of these positions are the next move, yield 1
      return 0;
    }
    
    // generates the set of valid moves for the player; returns a list of valid moves (validMoves)
    private void getValidMoves(int round, int state[][]) {
        int i, j;
        
        numValidMoves = 0;
        if (round < 4) {
            if (state[3][3] == 0) {
                validMoves[numValidMoves] = 3*8 + 3;
                numValidMoves ++;
            }
            if (state[3][4] == 0) {
                validMoves[numValidMoves] = 3*8 + 4;
                numValidMoves ++;
            }
            if (state[4][3] == 0) {
                validMoves[numValidMoves] = 4*8 + 3;
                numValidMoves ++;
            }
            if (state[4][4] == 0) {
                validMoves[numValidMoves] = 4*8 + 4;
                numValidMoves ++;
            }
            System.out.println("Valid Moves:");
            for (i = 0; i < numValidMoves; i++) {
                System.out.println(validMoves[i] / 8 + ", " + validMoves[i] % 8);
            }
        }
        else {
            System.out.println("Valid Moves:");
            for (i = 0; i < 8; i++) {
                for (j = 0; j < 8; j++) {
                    if (state[i][j] == 0) {
                        if (couldBe(true, state, i, j)) {
                            validMoves[numValidMoves] = i*8 + j;
                            numValidMoves ++;
                            System.out.println(i + ", " + j);
                        }
                    }
                }
            }
        }
        
        
        //if (round > 3) {
        //    System.out.println("checking out");
        //    System.exit(1);
        //}
    }
    
    private boolean checkDirection(boolean checkMyMoves, int state[][], int row, int col, int incx, int incy) {
        int sequence[] = new int[7];
        int seqLen;
        int i, r, c;
        
        seqLen = 0;
        for (i = 1; i < 8; i++) {
            r = row+incy*i;
            c = col+incx*i;
        
            if ((r < 0) || (r > 7) || (c < 0) || (c > 7))
                break;
        
            sequence[seqLen] = state[r][c];
            seqLen++;
        }
        
        int count = 0;
        if (checkMyMoves) { // Checking my (the computer player's) possible moves
          for (i = 0; i < seqLen; i++) {

            // Calculate everything based on which player number I am
              if (me == 1) {
                  if (sequence[i] == 2) // Counting the number of opponents peices to capture
                      count ++;
                  else {
                      if ((sequence[i] == 1) && (count > 0))
                          return true;
                      break;
                  }
              }
              else {
                  if (sequence[i] == 1)
                      count ++;
                  else {
                      if ((sequence[i] == 2) && (count > 0))
                          return true;
                      break;
                  }
              }
         }
        
          return false;
        } else { // Checking the opponents possible moves
          for (i = 0; i < seqLen; i++) {
              if (me == 1) {
                  if (sequence[i] == 1) // Counting the number of my peices to capture
                      count ++;
                  else {
                      if ((sequence[i] == 2) && (count > 0))
                          return true;
                      break;
                  }
              }
              else {
                  if (sequence[i] == 2)
                      count ++;
                  else {
                      if ((sequence[i] == 1) && (count > 0))
                          return true;
                      break;
                  }
              }
         }

         return false;
        }
    }
    
    private boolean couldBe(boolean checkMyMoves, int state[][], int row, int col) {
        int incx, incy;
        
        for (incx = -1; incx < 2; incx++) {
            for (incy = -1; incy < 2; incy++) {
                if ((incx == 0) && (incy == 0))
                    continue;
            
                if (checkDirection(checkMyMoves, state, row, col, incx, incy))
                    return true;
            }
        }
        
        return false;
    }
    
    public void readMessage() {
        int i, j;
        String status;
        try {
            //System.out.println("Ready to read again");
            turn = Integer.parseInt(sin.readLine());
            
            if (turn == -999) {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    System.out.println(e);
                }
                
                System.exit(1);
            }
            
            //System.out.println("Turn: " + turn);
            round = Integer.parseInt(sin.readLine());
            t1 = Double.parseDouble(sin.readLine());
            System.out.println(t1);
            t2 = Double.parseDouble(sin.readLine());
            System.out.println(t2);
            for (i = 0; i < 8; i++) {
                for (j = 0; j < 8; j++) {
                    state[i][j] = Integer.parseInt(sin.readLine());
                }
            }
            sin.readLine();
        } catch (IOException e) {
            System.err.println("Caught IOException: " + e.getMessage());
        }
        
        System.out.println("Turn: " + turn);
        System.out.println("Round: " + round);
        for (i = 7; i >= 0; i--) {
            for (j = 0; j < 8; j++) {
                System.out.print(state[i][j]);
            }
            System.out.println();
        }
        System.out.println();
    }
    
    public void initClient(String host) {
        int portNumber = 3333+me;
        
        try {
			s = new Socket(host, portNumber);
            sout = new PrintWriter(s.getOutputStream(), true);
			sin = new BufferedReader(new InputStreamReader(s.getInputStream()));
            
            String info = sin.readLine();
            System.out.println(info);
        } catch (IOException e) {
            System.err.println("Caught IOException: " + e.getMessage());
        }
    }

    
    // compile on your machine: javac *.java
    // call: java RandomGuy [ipaddress] [player_number]
    //   ipaddress is the ipaddress on the computer the server was launched on.  Enter "localhost" if it is on the same computer
    //   player_number is 1 (for the black player) and 2 (for the white player)
    public static void main(String args[]) {
        new SmartGuy(Integer.parseInt(args[1]), args[0]);
    }
    
}
