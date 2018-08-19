import javafx.scene.shape.Circle;
import javafx.scene.text.Text;

import java.util.ArrayList;

class State {
	private String name;
	private Text label;
	private double x;
	private double y;
	private boolean start;
	private boolean accept;
	private Circle circle;
	private Circle acceptCircle;
	private ArrayList<Transition> transition;

	public State(){
		transition = new ArrayList<>();
	}

	public State(String name, double x, double y){
		this.name = name;
		this.x = x;
		this.y = y;
		transition = new ArrayList<>();
	}

	public State(String name, double x, double y, Text label, Circle circle){
		this.name = name;
		this.x = x;
		this.y = y;
		this.label = label;
		this.circle = circle;
		transition = new ArrayList<>();
	}

	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public Text getLabel() {
		return label;
	}
	
	public void setLabel(Text label) {
		this.label = label;
	}
	
	public double getX(){
		return x;
	}
	
	public void setX(double x){
		this.x = x;
	}
	
	public double getY(){
		return y;
	}
	
	public void setY(double y){
		this.y = y;
	}
	
	public boolean isStart() {
		return start;
	}
	
	public void setStart(boolean start) {
		this.start = start;
	}
	
	public boolean isAccept() {
		return accept;
	}
	
	public void setAccept(boolean accept) {
		this.accept = accept;
	}
	
	public Circle getCircle(){
		return circle;
	}
	
	public void setCircle(Circle circle){
		this.circle = circle;
	}

	public Circle getAcceptCircle() {
		return acceptCircle;
	}

	public void setAcceptCircle(Circle acceptCircle) {
		this.acceptCircle = acceptCircle;
	}

	public ArrayList<Transition> getTransition() {
		return transition;
	}
	
	public void setTransition(ArrayList<Transition> transition) {
		this.transition = transition;
	}
}
