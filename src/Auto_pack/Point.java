package Auto_pack;
import java.text.DecimalFormat;

/*
 * A simple point class- the interested points the drone left behind
 */

public class Point {
	private double x;
	private double y;
	
	public Point(double x,double y) {
		this.x = x;
		this.y = y;
	}
	
	public Point(Point p) {
		this.x = p.x;
		this.y = p.y;
	}
	
	public Point() {
		x = 0;
		y = 0;
	}
	
	public double getX() {
		return this.x;
	}
	
	public void setX(double x) {
		this.x = x;
	}
	
	public double getY() {
		return this.y;
	}
	
	public void setY(double y) {
		this.y = y;
	}
	
	public double distance(Point other) {
		return Math.sqrt(Math.pow(this.x-other.x,2) + Math.pow(this.y-other.y,2));
	}
	
	@Override
	public String toString() {
		DecimalFormat df = new DecimalFormat("#.###");
		
		return "(" + df.format(x) + "," + df.format(y) + ")";
	}

}