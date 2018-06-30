package application;

import java.net.URL;
import java.util.ResourceBundle;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;

public class DlgTranslateImageController implements Initializable {

	public int nTx=0, nTy=0; //图像平移分量
	public boolean isOkClicked = false; //用户是否点击“确定”按钮
	@FXML
	private AnchorPane translatePane;
	@FXML
	private TextField tfTransX;
	@FXML
	private TextField tfTransY;
	@FXML
	private Button btnCancel;
	@FXML
	private Button btnOK;
	
	private Stage dialogStage;
	
	@Override
	public void initialize(URL location, ResourceBundle resources) {
		// TODO Auto-generated method stub		
	}
	
	public void setDialogStage(Stage dialogStage) {
		this.dialogStage = dialogStage;
	}

	@FXML
	private void onHandleOK(ActionEvent event) {
		try {
			nTx = Integer.parseInt(tfTransX.getText());
			nTy = Integer.parseInt(tfTransY.getText());
			isOkClicked = true;
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			dialogStage.close();
		}
	}
	
	@FXML
	private void onHandleCancel(ActionEvent event) {
		isOkClicked = false;
		dialogStage.close();
	}
}
