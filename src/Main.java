import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class Main extends Application {
	private Stage window;
	private Scene menu;
	private CloseButton closebutton;
	private HelpMenu help;
	private Editor editor;
	
	/* Launch the app */
	public static void main(String[] args) {
		launch(args);
	}
	
	@Override
	public void start(Stage primaryStage) throws Exception{
		window = primaryStage;
		window.setMinWidth(200);
		window.setMinHeight(200);
		initMenu();
		window.setScene(menu);
		window.setTitle("Definitely NOT JFlap");
		window.show();
	}
	
	private void initMenu(){
		/* File menu option. */
		Menu fileMenu = new Menu("File");
		fileMenu.setStyle("-fx-font-size: 10px;");
		fileMenu.getItems().add(new MenuItem("Close"));
		
		/* Help menu option */
		Menu helpMenu = new Menu("Help");
		helpMenu.setStyle("-fx-font-size: 10px;");
		helpMenu.getItems().add(new MenuItem("About"));
		MenuBar menuBar = new MenuBar();
		
		/* Add menu options */
		menuBar.getMenus().addAll(fileMenu, helpMenu);
		menuBar.setStyle("-fx-background-color: #dae4e3");
		
		/* Contents of page. */
		Label label = new Label("Welcome to umm... JFLAP?");
//		label.setStyle("-fx-text-fill: #839496");
		Button newMachineButton = new Button("New Machine");
		newMachineButton.requestFocus();
		Button helpButton = new Button("Help");
		closebutton = new CloseButton();
		closebutton.setCloseButton(window);
		
		/* Set layout. */
		BorderPane menuLayout = new BorderPane(); 				//outer Borderpane to hold menubar
		menuLayout.setTop(menuBar);
		//menuLayout.setStyle("-fx-background-color: #002b36");
		VBox buttonLayout = new VBox(20); 				//inner VBox to hold buttons
		buttonLayout.setPadding(new Insets(0, 0, 0, 50));
		buttonLayout.getChildren().addAll(label, newMachineButton, helpButton, closebutton.getCloseButton());
		//buttonLayout.setStyle("-fx-background-color: #002b36");
		menuLayout.setCenter(buttonLayout);
		menu = new Scene(menuLayout, 300, 300);
		
		/* After menu is set up, create other scenes. */
		help = new HelpMenu(window, menu);
		helpButton.setOnAction(e-> help.setMenu(window));
		
		newMachineButton.setOnAction(e-> {
			editor = new Editor(window, menu);
			editor.setMenu(window);
			editor.newMachine(window, menu);
			editor = null;
		});
		
	}
}
