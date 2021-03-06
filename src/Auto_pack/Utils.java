package Auto_pack;

import java.text.DecimalFormat;
import java.util.Random;
import javax.swing.JLabel;
import javax.swing.JOptionPane;

/*
 * Storage the static functionality of the program with the main algorithm function
 */

public class Utils {
	static boolean onlyOnce=true;
	static double lastDistance;
	static int lastTime;
	
	static boolean fixonlyOnce=true;
	static double fixlastDistance;
	static int fixlastTime;
	
	
	public static void stopAllCPUS() {
		for (int i = 0; i < GameVariabales.all_cpus.size(); i++) {
			GameVariabales.all_cpus.get(i).setPlay();
		}
	}

	public static void resumeAllCPUS() {
		for (int i = 0; i < GameVariabales.all_cpus.size(); i++) {
			GameVariabales.all_cpus.get(i).resume();
		}
	}

	public static void updateInfo(int deltaTime, JLabel info_label, JLabel info_label2, JLabel graphInfo) {
		DecimalFormat dfff = new DecimalFormat("#.##");
		info_label.setText(GameVariabales.drone.getInfoHTML());
		info_label2.setText("<html>" + String.valueOf(GameVariabales.counter) + " <BR>isRisky:"
				+ String.valueOf(GameVariabales.is_risky) + "<BR>" + String.valueOf(GameVariabales.risky_dis)
				+ "<BR> Time : " + String.valueOf(Timer.getTimeBySeconds()) + "<BR> Battery : "
				+ String.valueOf(dfff.format(GameVariabales.drone.getBattery().getStamina())) + "</html>");
		if (GameVariabales.graph != null)
			graphInfo.setText(GameVariabales.graph.toHtmlString());
	}

	public static void stopCPUS() {
		Utils.stopAllCPUS();
	}

	public static void resumseCPUS() {
		Utils.resumeAllCPUS();
	}

	public static void dronePlay() {
		GameVariabales.drone.play();
	}

	// CM sign
	public static Point getPointByDistance(Point fromPoint, double rotation, double distance) {
		double radians = Math.PI * (rotation / 180);

		double i = distance / Config.CMPerPixel;
		double xi = fromPoint.getX() + Math.cos(radians) * i;
		double yi = fromPoint.getY() + Math.sin(radians) * i;

		return new Point(xi, yi);
	}

	public static double noiseBetween(double min, double max, boolean isNegative) {
		Random rand = new Random();
		double noiseToDistance = 1;
		double noise = (min + rand.nextFloat() * (max - min)) / 100;
		if (!isNegative) {
			return noiseToDistance + noise;
		}

		if (rand.nextBoolean()) {
			return noiseToDistance + noise;
		} else {
			return noiseToDistance - noise;
		}

	}

	public static void setPixel(double x, double y, GameVariabales.PixelState state,
			GameVariabales.PixelState[][] map) {
		int xi = (int) x;
		int yi = (int) y;

		if (state == GameVariabales.PixelState.visited) {
			map[xi][yi] = state;
			return;
		}

		if (map[xi][yi] == GameVariabales.PixelState.unexplored) {
			map[xi][yi] = state;
		}
	}

	public static double getRotationBetweenPoints(Point from, Point to) {
		double y1 = from.getY() - to.getY();
		double x1 = from.getX() - to.getX();
		double radians = Math.atan(y1 / x1);
		double rotation = radians * 180 / Math.PI;
		return rotation;
	}

	/*
	 * The main function of the movement of the drone
	 */
	public static void ai(int deltaTime) {
		if (!GameVariabales.toogleAI) {
			return;
		}
		if (GameVariabales.is_init) {
			Utils.speedUp();
			Point dronePoint = GameVariabales.drone.getOpticalSensorLocation();
			GameVariabales.init_point = new Point(dronePoint);
			GameVariabales.graph = new Graph(GameVariabales.drone.getPointOnMap());
			GameVariabales.points.add(dronePoint);
			GameVariabales.is_init = false;
		}

		Point dronePoint = GameVariabales.drone.getOpticalSensorLocation();

		GameVariabales.spin_by = Config.max_angle_risky;
		if (!GameVariabales.is_risky) {

			Lidar lidar0 = GameVariabales.drone.getLidars().get(0);
			if (lidar0.getCurrentDistance() <= Config.max_risky_distance) {
				GameVariabales.is_risky = true;
				GameVariabales.risky_dis = lidar0.getCurrentDistance();
			}

			Lidar lidar1 = GameVariabales.drone.getLidars().get(1);
			if (lidar1.getCurrentDistance() <= Config.max_risky_distance / 3) {
				GameVariabales.is_risky = true;
			}

			Lidar lidar2 = GameVariabales.drone.getLidars().get(2);
			if (lidar2.getCurrentDistance() <= Config.max_risky_distance / 3) {
				GameVariabales.is_risky = true;
			}

		} else {
			if (!GameVariabales.try_to_escape) {

				GameVariabales.try_to_escape = true;

				Lidar lidar1 = GameVariabales.drone.getLidars().get(1);
				double a = lidar1.getCurrentDistance();

				Lidar lidar2 = GameVariabales.drone.getLidars().get(2);
				double b = lidar2.getCurrentDistance();

				if (a > 270 && b > 270) {
					GameVariabales.is_lidars_max = true;
					GameVariabales.spin_by = 90;
					Point l1 = Utils.getPointByDistance(dronePoint,
							GameVariabales.drone.getLidars().get(1).getDegrees()
									+ GameVariabales.drone.getGyroRotation(),
							GameVariabales.drone.getLidars().get(1).getCurrentDistance());
					Point l2 = Utils.getPointByDistance(dronePoint,
							GameVariabales.drone.getLidars().get(2).getDegrees()
									+ GameVariabales.drone.getGyroRotation(),
							GameVariabales.drone.getLidars().get(2).getCurrentDistance());
					Point last_point = Utils.getAvgLastPoint();
					double dis_to_lidar1 = Utils.getDistanceBetweenPoints(last_point, l1);
					double dis_to_lidar2 = Utils.getDistanceBetweenPoints(last_point, l2);

					if (dis_to_lidar1 < dis_to_lidar2) {
						GameVariabales.spin_by *= -1;
					}

				} else {
					if (a < b || GameVariabales.risky_dis >= 100) {
						GameVariabales.spin_by *= (-1);
					}
				}


				Utils.spinBy(GameVariabales.spin_by, true, new Func() {
					@Override
					public void method() {
						GameVariabales.try_to_escape = false;
						GameVariabales.is_risky = false;
					}
				});
			}
			Utils.interestedPoints(dronePoint);
		}
		Utils.isReturnHome(dronePoint, deltaTime);
		Utils.isBlock();
	}

	/*
	 * This function check if the drone enters the wall
	 */
	public static void isBlock() {
		// Alarmed the drone entered the wall (should not happened)
		if (GameVariabales.drone.getBattery().getStamina() <= 0) {
			stopCPUS();
			GameVariabales.gameEnd = true;
			clientMessage("Game over");
			System.exit(0);
		}
		if (!GameVariabales.is_init && GameVariabales.return_home && Utils.getDistanceBetweenPoints(GameVariabales.drone.getOpticalSensorLocation(), GameVariabales.points.get(0)) <= 15) {
				stopCPUS();
				GameVariabales.gameEnd = true;
				clientMessage("Arrived");
				System.exit(0);
		}
	}

	/*
	 * The drone leaves points behind every 100 meter 
	 */
	public static void interestedPoints(Point dronePoint) {
		int minDistanceBetweenImportantPoints = 100;
		if (Utils.getDistanceBetweenPoints(dronePoint,GameVariabales.points.get(GameVariabales.points.size() - 1)) > minDistanceBetweenImportantPoints
				&& !GameVariabales.return_home) {
			GameVariabales.points.add(dronePoint);
			GameVariabales.graph.addVertex(GameVariabales.drone.getPointOnMap());
		}
	}

	public static double angle(Point a, Point b) {
		return 180.0 / Math.PI * Math.atan2(a.getX() - b.getX(), a.getY() - b.getY());
	}
	
	/*
	 * Fixes the drone movement if he get stuck
	 */
	public static boolean isFixDirection(Point dronePoint) {
		if(fixonlyOnce) {
			fixlastDistance = Utils.getDistanceBetweenPoints(dronePoint, GameVariabales.points.get(0));
			fixlastTime = Timer.getTimeBySeconds();
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			fixonlyOnce = false;
		}
		double d = Utils.getDistanceBetweenPoints(dronePoint, GameVariabales.points.get(0));
		if(fixlastDistance < d) {
			fixlastTime = Timer.getTimeBySeconds();
			fixlastDistance = d;//if the boundary get smaller, update the boundary
			return false;
		}else {
			fixlastTime = Timer.getTimeBySeconds();
			fixlastDistance = d;//if the boundary get smaller, update the boundary
			return true;
		}
	}

	/*
	 * check if the drone is at the right way home
	 */
	public static boolean isHomeDirection(Point dronePoint) {
		if(onlyOnce) {
			lastDistance = Utils.getDistanceBetweenPoints(dronePoint, GameVariabales.points.get(0));
			lastTime = Timer.getTimeBySeconds();
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			onlyOnce = false;
		}
		double d = Utils.getDistanceBetweenPoints(dronePoint, GameVariabales.points.get(0));
		if(lastDistance < d) {
			lastTime = Timer.getTimeBySeconds();
			lastDistance = d;//if the boundary get smaller, update the boundary
			return false;
		}else {
			lastTime = Timer.getTimeBySeconds();
			lastDistance = d;//if the boundary get smaller, update the boundary
			return true;
		}
	}
	
	/*
	 * Delete the points the drone left behind if it is near them
	 */
	public static void deletInterestedPoints(Point dronePoint, int deltaTime) {
		if (GameVariabales.points.size() >= 1) {
			for (int i = 1; i < GameVariabales.points.size(); i++) {
				if (Utils.getDistanceBetweenPoints(dronePoint, GameVariabales.points.get(i)) <= 15) {		
					Utils.removePoint(dronePoint, i);
				}
			}
		}
	}

	/*
	 * Check if we clicked the "return home" button or we got half a battery left
	 */
	public static boolean isReturnHome(Point dronePoint, int deltaTime) {
		if (GameVariabales.return_home || GameVariabales.drone.getBattery().getStamina() < 50) {
			deletInterestedPoints(dronePoint, deltaTime);
			return true;
		}
		return false;
	}

	public static void batteryUpdate(int deltaTime) {
		GameVariabales.drone.getBattery().setStamina();
		try {
			Thread.sleep(1000);
		} catch (Exception e) {
			System.err.print(e);
		}
	}

	public static void rotateUpdate(int deltaTime) {
		if (GameVariabales.isRotating != 0) {
			Utils.updateRotating(deltaTime);
		}
	}

	public static void gameUpdates(int deltaTime) {
		Utils.updateVisited();
		Utils.updateMapByLidars();
		Utils.ai(deltaTime);
		Utils.speedUpdate(deltaTime);
	}

	public static void initMap() {
		GameVariabales.map = new GameVariabales.PixelState[Config.map_size][Config.map_size];
		for (int i = 0; i < Config.map_size; i++) {
			for (int j = 0; j < Config.map_size; j++) {
				GameVariabales.map[i][j] = GameVariabales.PixelState.unexplored;
			}
		}
		GameVariabales.droneStartingPoint = new Point(Config.map_size / 2, Config.map_size / 2);
	}
	
	public static void speedUpdate(int deltaTime) {
		if (GameVariabales.isSpeedUp) {
			GameVariabales.drone.speedUp(deltaTime);
		} else {
			GameVariabales.drone.slowDown(deltaTime);
		}
	}

	public static void speedUp() {
		GameVariabales.isSpeedUp = true;
	}

	public static void speedDown() {
		GameVariabales.isSpeedUp = false;
	}

	public static void updateVisited() {
		Point dronePoint = GameVariabales.drone.getOpticalSensorLocation();
		Point fromPoint = new Point(dronePoint.getX() + GameVariabales.droneStartingPoint.getX(),
				dronePoint.getY() + GameVariabales.droneStartingPoint.getY());
		Utils.setPixel(fromPoint.getX(), fromPoint.getY(), GameVariabales.PixelState.visited, GameVariabales.map);
	}

	public static void updateInfo(int deltaTime) {
		Utils.updateInfo(deltaTime, Visualizator.info_label_drone, Visualizator.info_label_config,
				Visualizator.graphInfo);
	}

	public static void updateAi(int deltaTime) {
		Utils.gameUpdates(deltaTime);
	}

	public static void updateMapByLidars() {
		Point dronePoint = GameVariabales.drone.getOpticalSensorLocation();
		Point fromPoint = new Point(dronePoint.getX() + GameVariabales.droneStartingPoint.getX(),
				dronePoint.getY() + GameVariabales.droneStartingPoint.getY());

		for (int i = 0; i < GameVariabales.drone.getLidars().size(); i++) {
			Lidar lidar = GameVariabales.drone.getLidars().get(i);
			double rotation = GameVariabales.drone.getGyroRotation() + lidar.getDegrees();
			for (int distanceInCM = 0; distanceInCM < lidar.getCurrentDistance(); distanceInCM++) {
				Point p = Utils.getPointByDistance(fromPoint, rotation, distanceInCM);
				Utils.setPixel(p.getX(), p.getY(), GameVariabales.PixelState.explored, GameVariabales.map);
			}

			if (lidar.getCurrentDistance() > 0 && lidar.getCurrentDistance() < Config.lidarLimit - Config.lidarNoise) {
				Point p = Utils.getPointByDistance(fromPoint, rotation, lidar.getCurrentDistance());
				Utils.setPixel(p.getX(), p.getY(), GameVariabales.PixelState.blocked, GameVariabales.map);
			}
		}
	}

	public static void updateRotating(int deltaTime) {

		if (GameVariabales.degrees_left.size() != 0) {

			double degrees_left_to_rotate = GameVariabales.degrees_left.get(0);
			boolean isLeft = (degrees_left_to_rotate > 0) ? false : true;
			double curr = GameVariabales.drone.getGyroRotation();
			double just_rotated = 0;
			just_rotated = curr - GameVariabales.lastGyroRotation;
			if (isLeft) {
				if (just_rotated > 0) {
					just_rotated = -(360 - just_rotated);
				}
			} else {
				if (just_rotated < 0) {
					just_rotated = 360 + just_rotated;
				}
			}

			GameVariabales.lastGyroRotation = curr;
			degrees_left_to_rotate -= just_rotated;
			GameVariabales.degrees_left.remove(0);
			GameVariabales.degrees_left.add(0, degrees_left_to_rotate);

			if ((isLeft && degrees_left_to_rotate >= 0) || (!isLeft && degrees_left_to_rotate <= 0)) {
				GameVariabales.degrees_left.remove(0);

				Func func = GameVariabales.degrees_left_func.get(0);
				if (func != null) {
					func.method();
				}
				GameVariabales.degrees_left_func.remove(0);

				if (GameVariabales.degrees_left.size() == 0) {
					GameVariabales.isRotating = 0;
				}
				return;
			}

			int direction = (int) (degrees_left_to_rotate / Math.abs(degrees_left_to_rotate));
			GameVariabales.drone.rotateLeft(deltaTime * direction);
			
		} else
			return;
	}

	public static void spinBy(double degrees, boolean isFirst, Func func) {
		GameVariabales.lastGyroRotation = GameVariabales.drone.getGyroRotation();
		if (isFirst) {
			GameVariabales.degrees_left.add(0, degrees);
			GameVariabales.degrees_left_func.add(0, func);
		} else {
			GameVariabales.degrees_left.add(degrees);
			GameVariabales.degrees_left_func.add(func);
		}
		GameVariabales.isRotating = 1;
	}

	public static void spinBy(double degrees, boolean isFirst) {
		GameVariabales.lastGyroRotation = GameVariabales.drone.getGyroRotation();
		if (isFirst) {
			GameVariabales.degrees_left.add(0, degrees);
			GameVariabales.degrees_left_func.add(0, null);
		} else {
			GameVariabales.degrees_left.add(degrees);
			GameVariabales.degrees_left_func.add(null);
		}

		GameVariabales.isRotating = 1;
	}

	public static void spinBy(double degrees) {
		GameVariabales.lastGyroRotation = GameVariabales.drone.getGyroRotation();

		GameVariabales.degrees_left.add(degrees);
		GameVariabales.degrees_left_func.add(null);
		GameVariabales.isRotating = 1;
	}

	public static Point getLastPoint() {
		if (GameVariabales.points.size() == 0) {
			return GameVariabales.init_point;
		}

		Point p1 = GameVariabales.points.get(GameVariabales.points.size() - 1);
		return p1;
	}

	public static void removePoint(Point dronePoint, int index) {
		if (!GameVariabales.points.isEmpty())
			GameVariabales.points.remove(index);
	}

	public static Point getAvgLastPoint() {
		if (GameVariabales.points.size() < 2) {
			return GameVariabales.init_point;
		}
		Point p1 = GameVariabales.points.get(GameVariabales.points.size() - 1);
		Point p2 = GameVariabales.points.get(GameVariabales.points.size() - 2);
		return new Point((p1.getX() + p2.getX()) / 2, (p1.getY() + p2.getY()) / 2);
	}

	public static double getDistanceBetweenPoints(Point from, Point to) {
		double x1 = (from.getX() - to.getX()) * (from.getX() - to.getX());
		double y1 = (from.getY() - to.getY()) * (from.getY() - to.getY());
		return Math.sqrt(x1 + y1);
	}

	public static void clientMessage(String message) {
		JOptionPane.showMessageDialog(null, message);
	}

}