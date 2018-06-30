package application;
	
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.stage.Stage;
import javafx.scene.Parent;
import javafx.scene.Scene;

public class Main extends Application {
	@Override
	public void start(Stage primaryStage) {
		try {
			//Read file fxml and draw interfere.
			FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("MyScene.fxml"));
			Parent root = fxmlLoader.load();
			primaryStage.setScene(new Scene(root));
			primaryStage.setTitle("人脸采集、训练及识别平台");
			primaryStage.show();
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) {
		launch(args);
	}
}
