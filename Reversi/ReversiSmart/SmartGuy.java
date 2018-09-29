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
    try {    
       FileWriter writer = new FileWriter("output.txt", false);
        
        while (true) {
            System.out.println("Read");
            readMessage();
            
            
            if (turn == me) {

                long startMillis = System.currentTimeMillis();

                System.out.println("Move");
                getValidMoves(round, state);
                
                myValueAndMove = move();
                int myMove = myValueAndMove[1];
                int value = myValueAndMove[0];
                //myMove = generator.nextInt(numValidMoves);        // select a move randomly
                
                String sel = validMoves[myMove] / 8 + "\n" + validMoves[myMove] % 8;

                long endMillis = System.currentTimeMillis();

                writer.append((endMillis - startMillis) + ", ");
                
                System.out.println("Selection took " + (endMillis - startMillis) + " milliseconds: (" + validMoves[myMove] / 8 + ", " + validMoves[myMove] % 8 + ") -> " + value);
                
                sout.println(sel);
            }
        //while (turn == me) {
        //    System.out.println("My turn");
            
            //readMessage();
        //}
   writer.flush();
    }
         } catch (IOException e) {
           System.out.println("Failed to open output.txt file");
         }
    }
    
    // You should modify this function
    // validMoves is a list of valid locations that you could place your "stone" on this turn
    // Note that "state" is a global variable 2D list that shows the state of the game
    private int[] move() {
        // just move randomly for now
        int[] myValueAndMove = getBestMove(true, numValidMoves, validMoves, Integer.MIN_VALUE, Integer.MAX_VALUE, 9);

        return myValueAndMove;
    }

    private void updateIfBetterValue(boolean isMaximizer, int[] bestMoveResponse, int expectedValue, int moveNumber) {
      if (isMaximizer) {
        if (expectedValue > bestMoveResponse[0]) {
          bestMoveResponse[0] = expectedValue;
          bestMoveResponse[1] = moveNumber;

          // Update alpha for maximizer
          bestMoveResponse[2] = expectedValue;
        }
      } else { 
        if (expectedValue < bestMoveResponse[0]) {
          bestMoveResponse[0] = expectedValue;
          bestMoveResponse[1] = moveNumber;

          // Update beta for minimizer
          bestMoveResponse[3] = expectedValue;
        }
      }
    }

    private int[] getExpectedValueFromSubtree(boolean opponentIsMaximizer, int alphaValue, int betaValue, int numLevelsLeft) {

      // Then just evaulate the moves for the opponent and get thier expectedValue
      int[] opponentMoves = new int[64];
      int numOpponentMoves = 0;

      //Evaluate moves for opponent, num moves, and decrement levels left
      for (int i = 0; i < 8; i++) {
          for (int j = 0; j < 8; j++) {
              if (state[i][j] == 0) {
                  if (couldBe(opponentIsMaximizer, state, i, j) > 0) { // Check opponents possible moves
                      opponentMoves[numOpponentMoves] = i*8 + j;
                      numOpponentMoves ++;
                      //System.out.println(i + ", " + j);
                  }
              }
          }
      }
      
      int[] expectedMoveResponse = getBestMove(opponentIsMaximizer, numOpponentMoves, opponentMoves, alphaValue, betaValue, numLevelsLeft - 1);
      
      return expectedMoveResponse;
    }

    // Return int[0] -> the best value, and int[1] -> the best move, int[2] -> alphaValue, int[3] -> betaValue
    private int[] getBestMove(boolean isMaximizer, int numMovesForPlayer, int[] movesForPlayer, int previousAlpha, int previousBeta, int numLevelsLeft) {

      int bestMoveResponse[] = new int[4];

      // Maximizers return the highest value
      // Minimizers return the smallest value
      
      if (isMaximizer) {
        bestMoveResponse[0] = Integer.MIN_VALUE;
      } else {
        bestMoveResponse[0] = Integer.MAX_VALUE;
      }

      bestMoveResponse[1] = 0;
      bestMoveResponse[2] = previousAlpha;
      bestMoveResponse[3] = previousBeta;

      // If we want to stop at this level
      if (numLevelsLeft == 1) {
          for (int a = 0; a < numMovesForPlayer; ++a) {

            // Alpha-beta pruning before entering possible choices
            if (bestMoveResponse[2] >= bestMoveResponse[3]) {
               return bestMoveResponse;
            }

            int currentMove = movesForPlayer[a];
            int expectedValue = evaluateMove(isMaximizer, state, numMovesForPlayer, currentMove);

            updateIfBetterValue(isMaximizer, bestMoveResponse, expectedValue, a);
          }

          //System.out.println("BASE(" + isMaximizer + "): " + bestMoveResponse[0]);

          return bestMoveResponse;
      }

      // If player cannot make a move
      if (numMovesForPlayer == 0) {
        
        // Alpha-beta pruning before entering possible choices
        if (bestMoveResponse[2] >= bestMoveResponse[3]) {
           return bestMoveResponse;
        }

        // Then this value is worth nothing
        int currentMoveValue = 0;

        if (isMaximizer) {
          currentMoveValue = -300;
        } else {
          currentMoveValue = 100;
        }

        int[] expectedMoveResponse = getExpectedValueFromSubtree(!isMaximizer, bestMoveResponse[2], bestMoveResponse[3], numLevelsLeft);

        // TODO: What to return on no possible move? 0th move?
        updateIfBetterValue(isMaximizer, bestMoveResponse, expectedMoveResponse[0] + currentMoveValue, 0);

          //System.out.println("LEVEL(" + isMaximizer + ") -> " + numLevelsLeft + ": " + bestMoveResponse[0]);
        return bestMoveResponse;
          
      } else {
        for (int a = 0; a < numMovesForPlayer; ++a) {
          
          // Alpha-beta pruning
          if (bestMoveResponse[2] >= bestMoveResponse[3]) {
            return bestMoveResponse;
          }

          int currentMove = movesForPlayer[a];
          int currentMoveValue = evaluateMove(isMaximizer, state, numMovesForPlayer, currentMove);

          // Update the game board if this move were to be done
          if (isMaximizer) {
            state[currentMove / 8][currentMove % 8] = me;
          } else {
            if (me == 1) {
              state[currentMove / 8][currentMove % 8] = 2;
            } else {
              state[currentMove / 8][currentMove % 8] = 1;
            }
          }

          int[] expectedMoveResponse = getExpectedValueFromSubtree(!isMaximizer, bestMoveResponse[2], bestMoveResponse[3], numLevelsLeft);

          updateIfBetterValue(isMaximizer, bestMoveResponse, expectedMoveResponse[0] + currentMoveValue, a);
            
          // Remove the move that would be done down this path
          state[currentMove / 8][currentMove % 8] = 0;

        }

          
        //System.out.println("LEVEL(" + isMaximizer + ") -> " + numLevelsLeft + ": " + bestMoveResponse[0]);
        return bestMoveResponse;
      }
    }

    private int evaluateMove(boolean isMaximizer, int[][] state, int numMovesForPlayer, int desiredMove) {

      int row = desiredMove / 8;
      int column = desiredMove % 8;

      int totalUtility = 0;

      // Corners are the best
      if (desiredMove == 0 || desiredMove == 7 || desiredMove == 56 || desiredMove == 63) {
        if (isMaximizer) {
          totalUtility += 300;
        } else {
          totalUtility -= 400;
        }
      }

      //Edges are great value
      if (row == 0 || row == 7 || column == 0 || column == 7) {
        if (isMaximizer) {
          totalUtility += 50;
          // If exactly one neighboring peice is there, that is bad
          if (hasOneSideNeighbor(isMaximizer, state, row, column)) {
            totalUtility -= 200;
          }
        } else {
          totalUtility -= 10;
          if (hasOneSideNeighbor(isMaximizer, state, row, column)) {
            totalUtility += 200;
          }
        }
      }

      //Avoid the spaces right next to the side
      if (row == 1 || row == 6 || column == 1 || column == 6) {
        if (isMaximizer) {
          totalUtility -= 70;
        } else {
          totalUtility += 70;
        }
      }

      // The more peices captured the better
      int totalCaptured = couldBe(isMaximizer, state, row, column);
      if (isMaximizer) {
        totalUtility += totalCaptured;
      } else {
        totalCaptured -= totalCaptured;
      }

      return totalUtility;
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
                        if (couldBe(true, state, i, j) > 0) {
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
    
    private int checkDirection(boolean checkMyMoves, int state[][], int row, int col, int incx, int incy) {
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
                          return count;
                      break;
                  }
              }
              else {
                  if (sequence[i] == 1)
                      count ++;
                  else {
                      if ((sequence[i] == 2) && (count > 0))
                          return count;
                      break;
                  }
              }
         }
        
          return 0;
        } else { // Checking the opponents possible moves
          for (i = 0; i < seqLen; i++) {
              if (me == 1) {
                  if (sequence[i] == 1) // Counting the number of my peices to capture
                      count ++;
                  else {
                      if ((sequence[i] == 2) && (count > 0))
                          return count;
                      break;
                  }
              }
              else {
                  if (sequence[i] == 2)
                      count ++;
                  else {
                      if ((sequence[i] == 1) && (count > 0))
                          return count;
                      break;
                  }
              }
         }

         return 0;
        }
    }

    private boolean hasOneSideNeighbor(boolean checkMyMoves, int state[][], int row, int col) {
      if (row == 0 || row == 7) {

        int left;
        if (col - 1 < 0) {
          left = 0;
        } else {
          left = state[row][col-1];
        }

        int right;
        if (col + 1 > 7) {
          right = 0;
        } else {
          right = state[row][col+1];
        }

        if (checkMyMoves) {
          if (me == 1) {
            if (left == 2 ^ right == 2) {
              return true;
            } else {
              return false;
            }
          } else {
            if (left == 1 ^ right == 1) {
              return true;
            } else {
              return false;
            }
          }
        } else {
          if (me == 1) {
            if (left == 1 ^ right == 1) {
              return true;
            } else {
              return false;
            }
          } else {
            if (left == 2 ^ right == 2) {
              return true;
            } else {
              return false;
            }
          }
        }
      }

      if (col == 0 || col == 7) {
        
        int up;
        if (row - 1 < 0) {
          up = 0;
        } else {
          up = state[row-1][col];
        }

        int down;
        if (row + 1 > 7) {
          down = 0;
        } else {
          down = state[row+1][col];
        }

        if (checkMyMoves) {
          if (me == 1) {
            if (up == 2 ^ down == 2) {
              return true;
            } else {
              return false;
            }
          } else {
            if (up == 1 ^ down == 1) {
              return true;
            } else {
              return false;
            }
          }
        } else {
          if (me == 1) {
            if (up == 1 ^ down == 1) {
              return true;
            } else {
              return false;
            }
          } else {
            if (up == 2 ^ down == 2) {
              return true;
            } else {
              return false;
            }
          }
        }
      }

      return false;
    }
    
    private int couldBe(boolean checkMyMoves, int state[][], int row, int col) {
        int incx, incy;

        int totalCaptured = 0;
        
        for (incx = -1; incx < 2; incx++) {
            for (incy = -1; incy < 2; incy++) {
                if ((incx == 0) && (incy == 0))
                    continue;

                totalCaptured += checkDirection(checkMyMoves, state, row, col, incx, incy);
            
                //if (checkDirection(checkMyMoves, state, row, col, incx, incy))
                //    return true;
            }
        }
        
        return totalCaptured;
        //return false;
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
