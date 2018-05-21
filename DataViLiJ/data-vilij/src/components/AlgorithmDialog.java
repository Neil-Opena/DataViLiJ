/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package components;

import java.util.Arrays;
import java.util.List;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import vilij.components.Dialog;

/**
 * This class is responsible for displaying the dialog that occurs when an
 * algorithm is currently running
 *
 * 
 * Most of this code has been copied from the ConfirmationDialog class in the vilij framework
 * @author Neil Opena
 */
public class AlgorithmDialog extends Stage implements Dialog {

	public enum Option {

		YES("Yes"), NO("No");

		@SuppressWarnings("unused")
		private String option;

		Option(String option) {
			this.option = option;
		}
	}

	private static AlgorithmDialog dialog;

	private Label message = new Label();
	private Option selectedOption;

	private AlgorithmDialog() {
		/* empty constructor */ }

	public static AlgorithmDialog getDialog() {
		if (dialog == null) {
			dialog = new AlgorithmDialog();
		}
		return dialog;
	}

	private void setMessage(String message) {
		this.message.setText(message);
	}

	/**
	 * Completely initializes the error dialog to be used.
	 *
	 * @param owner the window on top of which the error dialog window will
	 * be displayed
	 */
	@Override
	public void init(Stage owner) {
		initModality(Modality.WINDOW_MODAL); // modal => messages are blocked from reaching other windows
		initOwner(owner);

		List<Button> buttons = Arrays.asList(new Button(Option.YES.name()),
			new Button(Option.NO.name()));

		buttons.forEach(button -> button.setOnAction((ActionEvent event) -> {
			this.selectedOption = Option.valueOf(((Button) event.getSource()).getText());
			this.hide();
		}));

		HBox buttonBox = new HBox(5);
		buttonBox.getChildren().addAll(buttons);

		VBox messagePane = new VBox(message, buttonBox);
		messagePane.setAlignment(Pos.CENTER);
		messagePane.setPadding(new Insets(10, 20, 20, 20));
		messagePane.setSpacing(10);

		this.setScene(new Scene(messagePane));
	}

	/**
	 * Loads the specified title and message into the error dialog and then
	 * displays the dialog.
	 *
	 * @param dialogTitle the specified dialog title
	 * @param message the specified message
	 */
	@Override
	public void show(String dialogTitle, String message) {
		setTitle(dialogTitle);           // set the title of the dialog
		setMessage(message); // set the main error message
		showAndWait();                   // open the dialog and wait for the user to click the close button
	}

	public Option getSelectedOption() {
		return selectedOption;
	}

}
