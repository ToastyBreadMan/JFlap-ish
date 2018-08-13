import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

public class Tester {

    public boolean runMachine(Machine m, int tapeLoc, int waitTime) throws Exception{
        State currentState;
        ArrayList<State> states = m.getStates();
        Tape tape = m.tape;

        // Fail if tapeLoc is out of scope
        if(!tape.setTapeHead(tapeLoc)){
            System.out.printf("Invalid TapeHead value: %d /t TapeHead must be between 0 and %d\n", tapeLoc, tape.getSize()-1);
            return false;
        }

        // Fail if there is no start state
        currentState = m.getStartState();
        if(currentState == null){
            System.out.println("Machine has no start state!");
            return false;
        }

        // Main body
        while(true) {
            Character curChar = tape.currentTapeVal();
            Transition curTransition = null;

            // Set the color of the selected State
            if(currentState.getCircle() != null){
                currentState.getCircle().setFill(Color.GREENYELLOW);
            }

            // Find the transition where the current tape char is
            // equal to the first transition's readChar
            for(Transition t : currentState.getPaths()){
                if(t.getReadChar() == curChar && t.getFromState() == currentState){
                    curTransition = t;
                    break;
                }
            }

            // If no Transition is found, search for a transition
            // with no read char. I.E. catchall transition
            if(curTransition == null){
                for(Transition t : currentState.getPaths()){
                    if(t.getFromState() == currentState && t.getReadChar() == '~'){
                        curTransition = t;
                        break;
                    }
                }
            }

            // If no transition is found still, exit the Machine
            if(curTransition == null){

                TimeUnit.MILLISECONDS.sleep(waitTime);
                if(currentState.getCircle() != null) {
                    currentState.getCircle().setFill(Color.LIGHTGOLDENRODYELLOW);
                }

                return currentState.isAccept();
            }

            // Set color of the selected Transition
            if(curTransition.getLine() != null){
                curTransition.getLine().setFill(Color.YELLOWGREEN);
            }

            // If the writeChar is the null character do not write anything
            if(curTransition.getWriteChar() != '~'){
                try{
                    tape.setTape(curTransition.getWriteChar());
                } catch (Exception e){
                    System.out.println(e.getMessage());
                    return false;
                }
            }

            switch(curTransition.getMoveDirection()){
                case LEFT:
                    tape.left();
                    break;
                case RIGHT:
                    tape.right();
                    break;
                case STAY:
                    break;
            }

            currentState = curTransition.getToState();

            System.out.print("|");
            for(Character c : tape.getTapeAsArray()){
                System.out.printf("%c|", c);
            }
            System.out.print("\n");

            TimeUnit.MILLISECONDS.sleep(waitTime);

            // Reset Colors
            if(currentState.getCircle() != null) {
                currentState.getCircle().setFill(Color.LIGHTGOLDENRODYELLOW);
            }

            if(curTransition.getLine() != null){
                curTransition.getLine().setFill(Color.BLACK);
            }

            // TODO: prompt user if loop goes over X iterations
        }
    }
}