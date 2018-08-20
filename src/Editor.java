import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.concurrent.Task;
import javafx.event.EventHandler;
import javafx.geometry.Orientation;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Pair;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class Editor {
	private Stage window;
	private Scene editor;
	private ToolBar bar;
	private Pane editorSpace;
	private ToggleGroup toggleGroup;
	private Machine currentMachine;
	private EventHandler<MouseEvent> currentHandler;
	private ArrayList<Integer> deletedValues = new ArrayList<>();
	private State transitionFromState;
	private int stateNextVal = 0;
	private int circleRadius;
	private Polygon startTriangle;
	private ContextMenu contextMenu = initContextMenu();

	private static String ourHeader = "// Save File for JFLAP-ISH";
	private static String oldHeader = "xTuringMachine File Format 1.0";

	Editor(Stage window, Scene prev){
		this.window = window;

		BorderPane pane = new BorderPane();

		editorSpace = new Pane();
		
		pane.setTop(initMenuBar(window, prev));
		pane.setCenter(editorSpace);

		editor = new Scene(pane, 500, 500);

		pane.prefHeightProperty().bind(editor.heightProperty());
		pane.prefWidthProperty().bind(editor.widthProperty());

		circleRadius = 20;
		startTriangle = new Polygon();
	}

	void setCircleRadius(int size){
		circleRadius = size;
	}
	
	void setMenu(Stage window){
		window.setTitle("Editor");
		window.setMinWidth(500);
		window.setMinHeight(500);
		window.setScene(editor);
	}
	
	/* This sets up the menu bar, but does NOT set the button actions */
	private ToolBar initMenuBar(Stage window, Scene prev){
		bar = new ToolBar();
		toggleGroup = new ToggleGroup();

		ToggleButton addState = new ToggleButton("Add State");
		ObjectProperty<Font> addStateTrack = new SimpleObjectProperty<>(Font.getDefault());
		addState.fontProperty().bind(addStateTrack);
		addState.setUserData("Add State");
		addState.setToggleGroup(toggleGroup);
		
		ToggleButton deleteState = new ToggleButton("Delete Value");
		ObjectProperty<Font> deleteStateTrack = new SimpleObjectProperty<>(Font.getDefault());
		deleteState.fontProperty().bind(deleteStateTrack);
		deleteState.setUserData("Delete Value");
		deleteState.setToggleGroup(toggleGroup);
		
		ToggleButton addTransition = new ToggleButton("Add Transition");
		ObjectProperty<Font> addTransitionTrack = new SimpleObjectProperty<>(Font.getDefault());
		addTransition.fontProperty().bind(addTransitionTrack);
		addTransition.setUserData("Add Transition");
		addTransition.setToggleGroup(toggleGroup);

		// END TOGGLE BUTTONS

		Separator separator = new Separator();
		separator.setOrientation(Orientation.VERTICAL);

		// Begin NON-Toggle buttons

		/* Open the tester dialog. */
		Button tapeButton = new Button("Edit Tape");
		ObjectProperty<Font> tapeButtonTack = new SimpleObjectProperty<>(Font.getDefault());
		tapeButton.fontProperty().bind(tapeButtonTack);
		tapeButton.setOnAction(e->editTape(window, currentMachine));

		Button testButton = new Button("Test Machine");
		ObjectProperty<Font> testButtonTrack = new SimpleObjectProperty<>(Font.getDefault());
		testButton.fontProperty().bind(testButtonTrack);
		testButton.setOnAction(e-> testMachine(testButton, addState, deleteState, addTransition, tapeButton));

		Button backButton = new Button("Back");
		ObjectProperty<Font> backButtonTrack = new SimpleObjectProperty<>(Font.getDefault());
		backButton.fontProperty().bind(backButtonTrack);
		backButton.setOnAction(e->deleteEditor(window, prev, currentMachine));

		// Add toggle buttons
		bar.getItems().addAll(addState, addTransition, deleteState);

		// Add separator
		bar.getItems().add(separator);

		// Add non-toggle buttons
		bar.getItems().addAll(tapeButton, testButton, backButton);

		bar.setStyle("-fx-background-color: #dae4e3");

		// Cursor when over the bar will always be default cursor
		bar.addEventFilter(MouseEvent.MOUSE_MOVED, event -> editor.setCursor(Cursor.DEFAULT));

		bar.widthProperty().addListener((observable, oldWidth, newWidth) -> {
			addStateTrack.set(Font.font(Math.min(newWidth.doubleValue() / 45, 20)));
			deleteStateTrack.set(Font.font(Math.min(newWidth.doubleValue() / 45, 20)));
			addTransitionTrack.set(Font.font(Math.min(newWidth.doubleValue() / 45, 20)));
			testButtonTrack.set(Font.font(Math.min(newWidth.doubleValue() / 45, 20)));
			tapeButtonTack.set(Font.font(Math.min(newWidth.doubleValue() / 45, 20)));
			backButtonTrack.set(Font.font(Math.min(newWidth.doubleValue() / 45, 20)));
		});

		return bar;
	}
	
	public void newMachine(Stage window, Scene prev){
		currentMachine = new Machine();
		startMachine(window, prev);
	}

	public boolean saveMachine(Stage window, Machine m) {
		// Save the file here
		FileChooser chooser = new FileChooser();
		FileChooser.ExtensionFilter filter = new FileChooser.ExtensionFilter("Text Files", "*.txt");
		//FileNameExtensionFilter filter = new FileNameExtensionFilter(".txt Files", "txt");
		chooser.getExtensionFilters().add(filter);
		File file = chooser.showSaveDialog(window);
		if(file != null) {
			try {
				BufferedWriter bw = new BufferedWriter(new FileWriter(file));
				bw.write(currentMachine.toString());
				bw.close();
				return true;
			}
			catch (IOException e) {
				Alert saveError = new Alert(Alert.AlertType.ERROR);
				saveError.initOwner(window);
				saveError.initModality(Modality.APPLICATION_MODAL);
				saveError.setTitle("Error Saving");
				saveError.setHeaderText("There was an error trying to save this machine.");
				saveError.showAndWait();
				return false;
			}
		}
		else {
			System.out.println("File was null? Maybe create file?");
		}
		return false;

		//System.out.print(currentMachine.toString());
	}

	public void loadMachine(Stage window, Scene prev){
		Machine loadedMachine = new Machine();

		FileChooser chooser = new FileChooser();
		FileChooser.ExtensionFilter filter = new FileChooser.ExtensionFilter("Text Files", "*.txt");
		chooser.getExtensionFilters().add(filter);
		//FileNameExtensionFilter filter = new FileNameExtensionFilter(".txt Files", "txt");
		chooser.setTitle("Pick a Turing Machine file");
		File file = chooser.showOpenDialog(window);
		if(file != null) {
			/* Reads in the file */
			BufferedReader br;
			try {
				br = new BufferedReader(new FileReader(file));
				String curLine = br.readLine(); // Header (Compare to find out if imported from old or current)
				String[] splitLine;
				if (curLine.equals(ourHeader)) {
					Hashtable<String, State> totalStates = new Hashtable<>();
					//ArrayList<Transition> totalTransitions = new ArrayList<>();
					Pattern statePattern = Pattern.compile("\t(-?\\d+) (\\d+.\\d+) (\\d+.\\d+) (\\w+) (\\w+)");
					Pattern transitionPattern = Pattern.compile("\t(-?\\d+) (-?\\d+) (\\p{ASCII}) (\\p{ASCII}) (\\w+)");

					// Read until beginning of states
					while(!curLine.equals("STATES:")) {
						curLine = br.readLine();
					}
					// Process all states
					while(!curLine.startsWith("//")) {
						curLine = br.readLine();
						Matcher stateMatcher = statePattern.matcher(curLine);
						if (stateMatcher.matches()) {
							State newState = new State();
							newState.setLabel(new Text(stateMatcher.group(1)));
							newState.setName(stateMatcher.group(1));
							newState.setX(Double.parseDouble(stateMatcher.group(2)));
							newState.setY(Double.parseDouble(stateMatcher.group(3)));
							if (Boolean.parseBoolean(stateMatcher.group(4))) loadedMachine.setStartState(newState);
							newState.setAccept(Boolean.parseBoolean(stateMatcher.group(5)));
							loadedMachine.addState(newState);
							totalStates.put(stateMatcher.group(1), newState);

						}
					}
					// Read until beginning of transitions
					while(!curLine.equals("TRANSITION:")) {
						curLine = br.readLine();
					}
					// Process all transitions
					while(!curLine.startsWith("//")) {
						curLine = br.readLine();
						Matcher transitionMatcher = transitionPattern.matcher(curLine);
						if (transitionMatcher.matches()) {
							Transition newTransition = new Transition();
							newTransition.setFromState(totalStates.get(transitionMatcher.group(1)));
							newTransition.setToState(totalStates.get(transitionMatcher.group(2)));
							newTransition.setReadChar(transitionMatcher.group(3).charAt(0));
							newTransition.setWriteChar(transitionMatcher.group(4).charAt(0));
							if(transitionMatcher.group(5).equals("RIGHT")) newTransition.setMoveDirection(Transition.Direction.RIGHT);
							else if(transitionMatcher.group(5).equals("LEFT")) newTransition.setMoveDirection(Transition.Direction.LEFT);
							else throw new IOException("Bad Transition");
							loadedMachine.getTransitions().add(newTransition);
						}
					}
					// Read until beginning of tape
					while(!curLine.equals("TAPE:")) {
						curLine = br.readLine();
					}
					// Read tape
					curLine = br.readLine();
					curLine = curLine.substring(1, curLine.length());
					int tapeHead = Integer.parseInt(curLine);
					curLine = br.readLine();
					curLine = curLine.substring(1, curLine.length()); // Remove the tab
					ArrayList<Character> newTape = new ArrayList<>();
					for(char c : curLine.toCharArray()) newTape.add(c);
					loadedMachine.tape.initTape(newTape);
					loadedMachine.tape.setTapeHead(tapeHead);

					int highestState = Integer.MIN_VALUE;
					for (Map.Entry<String, State> pair : totalStates.entrySet()) {

						int cur = Integer.parseInt(pair.getKey());
						if (cur > highestState) highestState = cur;
					}
					stateNextVal = highestState + 1;

				} else if (curLine.equals(oldHeader)) {
					br.readLine(); // TransitionCharacters MaxStates (Not needed for our machine)


					curLine = br.readLine(); // TapeMinIndex TapeMaxIndex TapeHeadIndex; (Grab the head index from the end)

					Pattern tapeInfo = Pattern.compile("(-?\\d+) (-?\\d+) (-?\\d+)\\p{Punct}");
					Matcher tapeMatch = tapeInfo.matcher(curLine);
					int tapeHead;
					if (tapeMatch.matches()) tapeHead = Integer.parseInt(tapeMatch.group(3));
					else throw new IOException("Bad TapeHead");

					curLine = br.readLine(); // Tape (Grab this and copy it)
					ArrayList<Character> tapeChars = new ArrayList<>();
					for (char c : curLine.toCharArray()) tapeChars.add(c);

					// Tape is loaded here.
					loadedMachine.tape.initTape(tapeChars);
					loadedMachine.tape.setTapeHead(tapeHead);

					curLine = br.readLine(); // NumTransitions (This tells how many more lines to read for transitions)
					Pattern transitionNum = Pattern.compile("(\\d+)\\p{Punct}");
					Matcher transitionNumMatch = transitionNum.matcher(curLine);
					int numTransitions;
					if (transitionNumMatch.matches()) numTransitions = Integer.parseInt(curLine.substring(0, curLine.length() - 1));
					else throw new IOException("Bad NumTransitions");
					System.out.println(numTransitions);
					Hashtable<String, State> statesNeeded = new Hashtable<>();
					ArrayList<Transition> totalTransitions = new ArrayList<>();

					// Pattern match to read a transition line.
					Pattern transitionLinePattern = Pattern.compile("(\\d+) (\\p{ASCII}) (\\p{ASCII}) (\\p{ASCII}) (-?\\d+)\\p{Punct}"); //
					Matcher transitionMatch;

					for(int i = 0; i < numTransitions;) {
						// Next transition, and if reached the end of file before reading all transitions, throw exception
						// curState read write move nextState;
						if ((curLine = br.readLine()) == null) throw new EOFException();
						//System.out.println(curLine);
						transitionMatch = transitionLinePattern.matcher(curLine);

						// If the line is a valid transition
						if (transitionMatch.matches()) {
							//System.out.println("Found a matching transition line with number of elements: " + transitionMatch.groupCount());
							//System.out.println(transitionMatch.group(1) + " " + transitionMatch.group(2) + " " + transitionMatch.group(3) + " " + transitionMatch.group(4) + " " + transitionMatch.group(5));
							// We have a state to put the transition in
							State startState;
							Transition newTransition = new Transition();
							if(statesNeeded.containsKey(transitionMatch.group(1))) {
								startState = statesNeeded.get(transitionMatch.group(1));
							}
							// Otherwise make a state
							else {
								startState = new State();
								statesNeeded.put(transitionMatch.group(1), startState);
							}


							// Setup the transition
							newTransition.setFromState(startState);
							newTransition.setReadChar(transitionMatch.group(2).charAt(0));
							newTransition.setWriteChar(transitionMatch.group(3).charAt(0));

							if (transitionMatch.group(4).charAt(0) == 'L') newTransition.setMoveDirection(Transition.Direction.LEFT);
							else if (transitionMatch.group(4).charAt(0) == 'R') newTransition.setMoveDirection(Transition.Direction.RIGHT);
							else throw new IOException("Bad Direction"); // Old machine only accepted left and right directions.
							if(statesNeeded.containsKey(transitionMatch.group(5))) {
								newTransition.setToState(statesNeeded.get(transitionMatch.group(5)));
							}
							else {
								State toState = new State();
								statesNeeded.put(transitionMatch.group(5), toState);

								if(transitionMatch.group(5).equals("-1")) {
									toState.setAccept(true); // Old machine only had one accept state.
								}
								newTransition.setToState(toState);
							}
							totalTransitions.add(newTransition);
							startState.getTransition().add(newTransition);
							i++;

						}
						// Skip the line if it isn't valid
					}
					Integer counter = 0;
					int highestState = Integer.MIN_VALUE;
					for(Map.Entry<String, State> s : statesNeeded.entrySet()) {
						State curState = s.getValue();
						curState.setName(s.getKey());

						curState.setX(100 + 150 * (counter % 5) + 30 * (counter / 5));
						curState.setY(30 + 150 * (counter / 5) + 30 * (counter % 2));
						curState.setLabel(new Text(s.getKey()));

						loadedMachine.addState(curState);
						counter++;
						int curVal = Integer.parseInt(curState.getName());
						if (curVal > highestState) highestState = curVal;
					}
					stateNextVal = highestState + 1;
					loadedMachine.setTransitions(totalTransitions);



					// Create our own state locations
				} else {
					// Not a valid header format, display a message and return false
					Alert invalidFileType = new Alert(Alert.AlertType.INFORMATION);
					invalidFileType.setTitle("Invalid File");
					invalidFileType.setHeaderText("Incorrect File header.");
					invalidFileType.initOwner(window);
					invalidFileType.initModality(Modality.APPLICATION_MODAL);
					invalidFileType.showAndWait();

				}

			}
			catch (IOException e) {
				Alert fileError = new Alert(Alert.AlertType.ERROR);
				fileError.setTitle("File Error");
				fileError.setHeaderText("Ran into a problem loading that file!");
				fileError.setContentText(e.getMessage());
				fileError.initOwner(window);
				fileError.initModality(Modality.APPLICATION_MODAL);
				fileError.showAndWait();

			}
		}


		currentMachine = loadedMachine;
		redrawAllStates();
		redrawAllPaths();
		startMachine(window, prev);


	}
	
	/* Called whenever a new machine is setup */
	private void startMachine(Stage window, Scene prev){

		Circle circle = new Circle(circleRadius, null);
		circle.setStroke(Color.BLACK);

		SnapshotParameters sp = new SnapshotParameters();
		sp.setFill(Color.TRANSPARENT);

		Image img = circle.snapshot(sp, null);
		ImageCursor cursor = new ImageCursor(img, img.getHeight() / 2, img.getWidth() / 2);

		EventHandler<MouseEvent> MoveEvent = event -> {
			if(event.getY() > circleRadius)
				editor.setCursor(cursor);
			else
				editor.setCursor(Cursor.DEFAULT);
		};

		toggleGroup.selectedToggleProperty().addListener((ov, toggle, new_toggle) -> {

			//   ____       _
			//  / ___|  ___| |_ _   _ _ __
			//  \___ \ / _ \ __| | | | '_ \
			//   ___) |  __/ |_| |_| | |_) |
			//  |____/ \___|\__|\__,_| .__/
			//                       |_|
			editorSpace.removeEventFilter(MouseEvent.MOUSE_MOVED, MoveEvent);
			if(currentHandler != null)
				editorSpace.removeEventHandler(MouseEvent.MOUSE_CLICKED, currentHandler);
			if(transitionFromState != null){
				transitionFromState.getCircle().setFill(Color.LIGHTGOLDENRODYELLOW);
				transitionFromState = null;
			}
			for (Path p : currentMachine.getPaths())
				p.setTextFillColor(Color.BLACK);

			//   _   _
			//  | \ | | ___  _ __   ___
			//  |  \| |/ _ \| '_ \ / _ \
			//  | |\  | (_) | | | |  __/
			//  |_| \_|\___/|_| |_|\___|
			//
			if(new_toggle == null){
				System.out.println("No toggle selected");
			}

			//      _       _     _   ____  _        _
			//     / \   __| | __| | / ___|| |_ __ _| |_ ___
			//    / _ \ / _` |/ _` | \___ \| __/ _` | __/ _ \
			//   / ___ \ (_| | (_| |  ___) | |_ (_| | |_  __/
			//  /_/   \_\__,_|\__,_| |____/ \__\__,_|\__\___|
			//
			else if (new_toggle.getUserData() == "Add State"){
				System.out.println(new_toggle.getUserData());

				editorSpace.addEventFilter(MouseEvent.MOUSE_MOVED, MoveEvent);

				// Define our new click handler
				currentHandler = event -> {

				    // If click is left click and not too close to another circle
					double minDist = calcDist(event, currentMachine);
					if (event.getButton() == MouseButton.PRIMARY
							&& !(event.getTarget() instanceof Circle)
							&& !(event.getTarget() instanceof Text)
							&& (minDist / 3) >= circleRadius
							&& event.getY() > circleRadius) {

						// Figure out what the name of the state should be;
						String name;
						if (deletedValues.isEmpty()) {
							name = Integer.toString(stateNextVal);
							System.out.println(stateNextVal);
							stateNextVal++;
						} else {
							int minIndex = deletedValues.indexOf(Collections.min(deletedValues));
							int savedVal = deletedValues.get(minIndex);
							deletedValues.remove(minIndex);
							System.out.println(savedVal);
							name = Integer.toString(savedVal);
						}

						Circle c = new Circle(event.getX(), event.getY(), circleRadius, Color.LIGHTGOLDENRODYELLOW);
						c.setId(name);
						c.setStrokeWidth(2);
						c.setStroke(Color.BLACK);

						Text t = new Text(name);
						t.setId(name);
						t.setX(c.getCenterX() - (t.getLayoutBounds().getWidth() / 2));
						t.setY(c.getCenterY() + (t.getLayoutBounds().getHeight() / 4));

						// Set Create State and add it to the Node's user data
						// so it is easy to find if clicked on
						State s = new State(name, event.getX(), event.getY(), t, c);
						c.setUserData(s);
						t.setUserData(s);

						c.setOnContextMenuRequested(event1 -> {
							contextMenu.show(c,event1.getScreenX(), event1.getScreenY());
						});
						t.setOnContextMenuRequested(event2 -> {
							contextMenu.show(t,event2.getScreenX(),event2.getScreenY());
						});

						currentMachine.addState(s);
						editorSpace.getChildren().addAll(s.getCircle(), s.getLabel());
					}
				};

				// Add the new event handler to the editorSpace
				editorSpace.addEventHandler(MouseEvent.MOUSE_CLICKED, currentHandler);
			}

			//   ____       _      _         ____  _        _
			//  |  _ \  ___| | ___| |_ ___  / ___|| |_ __ _| |_ ___
			//  | | | |/ _ \ |/ _ \ __/ _ \ \___ \| __/ _` | __/ _ \
			//  | |_| |  __/ |  __/ |_  __/  ___) | |_ (_| | |_  __/
			//  |____/ \___|_|\___|\__\___| |____/ \__\__,_|\__\___|
			//
			else if (new_toggle.getUserData() == "Delete Value"){
				System.out.println(new_toggle.getUserData());

				for (Path p : currentMachine.getPaths())
					p.setTextFillColor(Color.DARKRED);

				currentHandler = event -> {
					if(event.getButton() == MouseButton.PRIMARY
							&& (event.getTarget() instanceof Circle
							|| event.getTarget() instanceof Text)){

						Object Target = ((Node) event.getTarget()).getUserData();

						if(Target instanceof State) {
							State targetState;
							ArrayList<Transition> deleteTransitions = new ArrayList<>();
							ArrayList<Path> deletePaths = new ArrayList<>();

							targetState = (State) Target;

							for (Transition t : currentMachine.getTransitions()) {
								if (t.getToState() == targetState) {

									ArrayList<Node> nodes = t.getPath().getAllNodes();
									if (!nodes.isEmpty())
										editorSpace.getChildren().removeAll(t.getPath().getAllNodes());
									t.getFromState().getTransition().remove(t);

									deletePaths.add(t.getPath());
									deleteTransitions.add(t);
								}
							}
							currentMachine.getPaths().removeAll(deletePaths);
							currentMachine.getTransitions().removeAll(deleteTransitions);
							deleteTransitions.clear();
							deletePaths.clear();

							deleteState(targetState);
						}
						// TODO: edge case causes transitions not to delete properly
						else if(Target instanceof Transition){
							ArrayList<Node> nodes;
							Transition targetTransition;

							targetTransition = (Transition) Target;
							nodes = targetTransition.getPath().removeTransition(targetTransition);

							if(!nodes.isEmpty())
								editorSpace.getChildren().removeAll(nodes);

							if(targetTransition.getPath().getAllNodes().isEmpty())
								currentMachine.getPaths().remove(targetTransition.getPath());

							targetTransition.getFromState().getTransition().remove(targetTransition);
							currentMachine.getTransitions().remove(targetTransition);
						}

						for(Transition t : currentMachine.getTransitions())
							System.out.printf("%c ; %c ; %c\n", t.getReadChar(), t.getWriteChar(), t.getMoveDirection().toString().charAt(0));

						for(Path p : currentMachine.getPaths())
							System.out.println(p.toString());
					}
				};
				editorSpace.addEventHandler(MouseEvent.MOUSE_CLICKED, currentHandler);
			}

			//      _       _     _   _____                    _ _   _
			//     / \   __| | __| | |_   _| __ __ _ _ __  ___(_) |_(_) ___  _ __
			//    / _ \ / _` |/ _` |   | || '__/ _` | '_ \/ __| | __| |/ _ \| '_ \
			//   / ___ \ (_| | (_| |   | || | | (_| | | | \__ \ | |_| | (_) | | | |
			//  /_/   \_\__,_|\__,_|   |_||_|  \__,_|_| |_|___/_|\__|_|\___/|_| |_|
			//
			else if (new_toggle.getUserData() == "Add Transition"){
				System.out.println(new_toggle.getUserData());

				currentHandler = event -> {
					if(event.getButton() == MouseButton.PRIMARY){
						if(event.getTarget() instanceof Circle || event.getTarget() instanceof Text){
							Node Target = (Node) event.getTarget();

							if(Target.getUserData() instanceof State){
								State s = (State) Target.getUserData();
								System.out.printf("State: %s\n", s.getName());

								if(transitionFromState == null){
									transitionFromState = s;
									transitionFromState.getCircle().setFill(Color.AQUA);
								}
								else{
									System.out.printf("Create Transition from %s to %s\n", transitionFromState.getName(), s.getName());

									s.getCircle().setFill(Color.AQUA);
									Transition t = addTransition(transitionFromState, s);

									if(t == null){
										transitionFromState.getCircle().setFill(Color.LIGHTGOLDENRODYELLOW);
										s.getCircle().setFill(Color.LIGHTGOLDENRODYELLOW);
										transitionFromState = null;

										return;
									}

									for (Transition temp : currentMachine.getTransitions()){
										if(temp.compareTo(t))
											return;
									}

									currentMachine.getTransitions().add(t);
									transitionFromState.getTransition().add(t);

									Path path = null;
									for(Path p : currentMachine.getPaths()){
										if(p.compareTo(transitionFromState, s)) {
										    path = p;
										    System.out.println("Found Path");
										    break;
										}
									}

									if (path == null){
										path = new Path(transitionFromState, s);
										System.out.println("New Path");
										currentMachine.getPaths().add(path);
									}

									t.setPath(path);
									ArrayList<Node> nodes = path.addTransition(t);
									editorSpace.getChildren().addAll(nodes);

									for(Node n : nodes)
										if(n instanceof Line)
											n.toBack();

									s.getCircle().setFill(Color.LIGHTGOLDENRODYELLOW);
									transitionFromState.getCircle().setFill(Color.LIGHTGOLDENRODYELLOW);
									transitionFromState = null;
								}
							}
						}
						else{
							if(transitionFromState != null)
								transitionFromState.getCircle().setFill(Color.LIGHTGOLDENRODYELLOW);
							transitionFromState = null;
						}

					}

				};
				editorSpace.addEventHandler(MouseEvent.MOUSE_CLICKED, currentHandler);
			}
		});
	}

	private ContextMenu initContextMenu(){
		ContextMenu contextMenu = new ContextMenu();

		MenuItem setStart = new MenuItem("Set Start");
		setStart.setOnAction(event -> {
			State s = (State) contextMenu.getOwnerNode().getUserData();

			editorSpace.getChildren().remove(startTriangle);
			startTriangle.getPoints().clear();

			startTriangle.getPoints().addAll(
					s.getCircle().getCenterX()-circleRadius - 1, s.getCircle().getCenterY(),
					s.getCircle().getCenterX()-2*circleRadius, s.getCircle().getCenterY()-circleRadius,
					s.getCircle().getCenterX()-2*circleRadius, s.getCircle().getCenterY()+circleRadius
			);

			startTriangle.setFill(null);
			startTriangle.setStroke(Color.BLACK);

			editorSpace.getChildren().addAll(startTriangle);
			currentMachine.setStartState(s);
			System.out.printf("State %s is now start\n", currentMachine.getStartState().getName());
		});

		MenuItem toggleAccept = new MenuItem("Toggle Accept");
		toggleAccept.setOnAction(event -> {
			State s = (State) contextMenu.getOwnerNode().getUserData();

			if(s.getAcceptCircle() == null){
				s.setAccept(true);

				Circle c = new Circle(s.getCircle().getCenterX(), s.getCircle().getCenterY()
						, circleRadius * 1.25, null);
				c.setStrokeWidth(2);
				c.setStroke(Color.BLACK);

				s.setAcceptCircle(c);
				editorSpace.getChildren().add(c);
				c.toBack();

				System.out.printf("State %s is accept = %s\n", s.getName(), s.isAccept());
			}
			else {
				s.setAccept(false);

				editorSpace.getChildren().remove(s.getAcceptCircle());

				s.setAcceptCircle(null);
				System.out.printf("State %s is accept = %s\n", s.getName(), s.isAccept());
			}
		});

		contextMenu.getItems().addAll(setStart, toggleAccept);

		return contextMenu;
	}

	// Function to delete state
	private void deleteState(State state){
		editorSpace.getChildren().removeAll(state.getCircle(), state.getLabel());

		currentMachine.getTransitions().removeAll(state.getTransition());

		for(Transition t : state.getTransition()){
			editorSpace.getChildren().removeAll(t.getPath().getAllNodes());
			currentMachine.getPaths().remove(t.getPath());
			t.setPath(null);
		}
		state.getTransition().clear();

		state.setCircle(null);
		state.setLabel(null);

		if (currentMachine.getStartState() == state){
		    System.out.printf("State %s is start removing...", state.getName());
			currentMachine.setStartState(null);
			editorSpace.getChildren().remove(startTriangle);
		}

		if (state.isAccept())
			editorSpace.getChildren().remove(state.getAcceptCircle());

		currentMachine.deleteState(state);
		deletedValues.add(Integer.parseInt(state.getName()));
		state = null;
	}

	private void deleteEditor(Stage window, Scene prev, Machine m){
		System.out.println("If you see this you should be saving your machine");
		saveMachine(window, currentMachine);
		window.setScene(prev);

		//TODO: Save machine before this

		deletedValues.clear();

		window.setMinWidth(300);
		window.setMinHeight(400);
		window.setHeight(400);
		window.setWidth(300);

		/* garbage collection to the rescue */
		editor = null;
		bar = null;
		editorSpace = null;

		currentMachine = null;
		if(currentHandler != null) {
			window.removeEventHandler(MouseEvent.MOUSE_CLICKED, currentHandler);
		}
		currentHandler = null;
	}
	

	private Transition addTransition(State from, State to) {
		// This window suspends until Transition editor is done.
		TransitionEditor t = new TransitionEditor(window ,from, to);

		// Check if transition is valid is done.
		if(t.createdTransition == null)
			System.out.println("null");
		else
			System.out.printf("Transition: %s -> %s %c %c %s\n", t.createdTransition.getFromState().getName(), t.createdTransition.getToState().getName(),
					t.createdTransition.getReadChar(), t.createdTransition.getWriteChar(), t.createdTransition.getMoveDirection().toString());

		return t.createdTransition;
	}
	
	private double calcDist(MouseEvent event, Machine currentMachine){
		double min = Double.MAX_VALUE;
		if(!(currentMachine.getStates().isEmpty())) {
			State minState = null;
			for (State state : currentMachine.getStates()) {
				double dist = distForm(event.getX(), state.getCircle().getCenterX(),
						event.getY() , state.getCircle().getCenterY());
				if(min > dist){
					min = dist;
					minState = state;
				}
			}
		}
		return min;
	}
	
	private Pair calcCloseState(MouseEvent event, Machine currentMachine){
		double min = Double.MAX_VALUE;
		State minState = null;
		Pair returnVal = null;
		if(!(currentMachine.getStates().isEmpty())) {
			minState = null;
			for (State state : currentMachine.getStates()) {
				//System.out.printf("Using (%f, %f) and (%f, %f)", event.getX(), event.getY(), state.getCircle().getCenterX(), state.getCircle().getCenterY());
				double dist = distForm(event.getX(), state.getCircle().getCenterX(),
						event.getY(), state.getCircle().getCenterY());
				if(min > dist){
					min = dist;
					minState = state;
					returnVal = new Pair(min, minState);
				}
			}
		}
		return returnVal;
	}

	private void redrawAllStates(){
		for(State s : currentMachine.getStates()){
			editorSpace.getChildren().removeAll(s.getCircle(), s.getLabel());


			Circle c = new Circle(s.getX(), s.getY(), circleRadius, Color.LIGHTGOLDENRODYELLOW);
			c.setId(s.getName());
			c.setStrokeWidth(2);
			c.setStroke(Color.BLACK);
			s.setCircle(c);
			Text t = new Text(s.getName());
			t.setId(s.getName());
			t.setX(c.getCenterX() - (t.getLayoutBounds().getWidth() / 2));
			t.setY(c.getCenterY() + (t.getLayoutBounds().getHeight() / 4));
			s.setLabel(t);
			// Set Create State and add it to the Node's user data
			// so it is easy to find if clicked on
			c.setUserData(s);
			t.setUserData(s);

			c.setOnContextMenuRequested(event1 -> {
				contextMenu.show(c,event1.getScreenX(), event1.getScreenY());
			});
			t.setOnContextMenuRequested(event2 -> {
				contextMenu.show(t,event2.getScreenX(),event2.getScreenY());
			});


			editorSpace.getChildren().addAll(s.getCircle(), s.getLabel());
		}
	}

	private void redrawAllPaths(){
		for(Path p : currentMachine.getPaths())
			editorSpace.getChildren().removeAll(p.getAllNodes());

		currentMachine.getPaths().clear();

		for (Transition t : currentMachine.getTransitions()){
			Path path = null;
			for(Path p : currentMachine.getPaths()){
				if(p.compareTo(t.getFromState(), t.getToState())) {
					path = p;
					System.out.println("Found Path");
					break;
				}
			}

			if (path == null){
				path = new Path(t.getFromState(), t.getToState());
				System.out.println("New Path");
				currentMachine.getPaths().add(path);
			}

			t.setPath(path);
			ArrayList<Node> nodes = path.addTransition(t);
			editorSpace.getChildren().addAll(nodes);

			for(Node n : nodes)
				if(n instanceof Line)
					n.toBack();
		}
		try {
			for (Path p : currentMachine.getPaths())
				editorSpace.getChildren().addAll(p.getAllNodes());
		}
		catch (Exception e) {

		}
	}

	private double distForm(double x1, double x2, double y1, double y2){
		return Math.hypot(x2-x1, y2-y1);
	}
	
	private void testMachine(Button thisButton ,Node... args){
		toggleGroup.selectToggle(null);
		for(Node b : args)
			b.setDisable(true);

		Tester tester = new Tester();

		Task<Void> task = new Task<Void>() {
			@Override
			public Void call(){
				try {
					tester.runMachine(currentMachine, 500);
				} catch (Exception e) {
					System.out.println(e);
					Alert alert = new Alert(Alert.AlertType.ERROR);
					alert.initOwner(window);
					alert.initModality(Modality.APPLICATION_MODAL);
					alert.setTitle("An Exception has Occurred!");
					alert.setHeaderText(e.toString());
					// TODO: Better send us message
					alert.setContentText("Oops...");

					StringWriter stringWriter = new StringWriter();
					PrintWriter printWriter = new PrintWriter(stringWriter);
					e.printStackTrace(printWriter);
					String exceptionText = stringWriter.toString();

					Label label = new Label("The exception stacktrace was:");

					TextArea textArea = new TextArea(exceptionText);
					textArea.setEditable(false);
					textArea.setWrapText(true);
					textArea.setMaxWidth(Double.MAX_VALUE);
					textArea.setMaxHeight(Double.MAX_VALUE);
					GridPane.setVgrow(textArea, Priority.ALWAYS);
					GridPane.setHgrow(textArea, Priority.ALWAYS);

					GridPane expContent = new GridPane();
					expContent.setMaxWidth(Double.MAX_VALUE);
					expContent.add(label, 0, 0);
					expContent.add(textArea, 0, 1);

					alert.getDialogPane().setExpandableContent(expContent);

					alert.showAndWait();
				}

				return null;
			}
		};
		task.setOnSucceeded(event -> {
			Alert alert = new Alert(Alert.AlertType.ERROR);
			alert.initOwner(window);
			alert.initModality(Modality.APPLICATION_MODAL);
			alert.setTitle("The machine has finished");

			if(tester.didSucceed()){
				alert.setGraphic(new ImageView(this.getClass().getResource("checkmark.png").toString()));
				alert.setHeaderText("The machine has finished successfully");
			}
			else {
				alert.setHeaderText("The machine has finished unsuccessfully");
				alert.setContentText(tester.getFailReason());
			}

			alert.showAndWait();

			thisButton.setText("Test Machine");
			thisButton.setOnAction(event1 ->  testMachine(thisButton, args));

			for(Node b : args)
				b.setDisable(false);
		});
		task.setOnCancelled(event -> {
			for(State s : currentMachine.getStates())
				s.getCircle().setFill(Color.LIGHTGOLDENRODYELLOW);

		    for(Node b : args)
		    	b.setDisable(false);

			thisButton.setText("Test Machine");
			thisButton.setOnAction(event1 ->  testMachine(thisButton, args));
		});

		thisButton.setText("Stop Machine");
		thisButton.setOnAction(event ->  task.cancel());

		new Thread(task).start();
	}

	private void editTape(Stage window, Machine currentMachine) {
		TextInputDialog tapeEdit = new TextInputDialog();
		tapeEdit.setTitle("Edit Tape");
		tapeEdit.setHeaderText("Valid characters are Ascii values 32-125\nThis includes all alpha-numeric values.");

		tapeEdit.setContentText("Enter a string for the tape (spaces for blank):");
		tapeEdit.initOwner(window);
		tapeEdit.initModality(Modality.APPLICATION_MODAL);

		Optional<String> result = tapeEdit.showAndWait();
		result.ifPresent(tapeString -> {
			ArrayList<Character> characters = new ArrayList<>();
			for(Character c: tapeString.toCharArray()) {
				if (c >= 32 && c < 126) {
					characters.add(c);
				}
				else {
					Alert alert = new Alert(Alert.AlertType.WARNING);
					alert.setTitle("Invalid character(s)");
					alert.setContentText("You input invalid character(s) in your tape.");
					alert.initOwner(window);
					alert.initModality(Modality.APPLICATION_MODAL);
					alert.showAndWait();
					editTape(window, currentMachine);
					return;
				}
			}
			System.out.print("Old tape: ");
			for(Character c: currentMachine.tape.getTapeAsArray()) {
				System.out.print(c);
			}
			System.out.println();

			currentMachine.tape.initTape(characters);
			System.out.print("New tape: ");
			for(Character c: currentMachine.tape.getTapeAsArray()) {
				System.out.print(c);
			}
			System.out.println();

		});
	}
}
