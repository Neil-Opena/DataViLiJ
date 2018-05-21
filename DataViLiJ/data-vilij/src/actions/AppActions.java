package actions;

import components.AlgorithmDialog;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.SnapshotParameters;
import javafx.scene.chart.Chart;
import javafx.scene.image.WritableImage;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javax.imageio.ImageIO;

import dataprocessors.AppData;
import javafx.application.Platform;
import vilij.components.ActionComponent;
import vilij.components.ConfirmationDialog;
import vilij.components.ConfirmationDialog.Option;
import vilij.components.Dialog;
import vilij.propertymanager.PropertyManager;
import vilij.templates.ApplicationTemplate;
import ui.AppUI;
import static settings.AppPropertyTypes.*;
import ui.DataVisualizer;

/**
 * This is the concrete implementation of the action handlers required by the
 * application.
 *
 * @author Ritwik Banerjee
 */
public final class AppActions implements ActionComponent {

	/**
	 * The application to which this class of actions belongs.
	 */
	private ApplicationTemplate applicationTemplate;
	private AppUI appUI;
	private PropertyManager manager;

	private FileChooser tsdFileChooser; // FileChooser to select the tsd file
	private FileChooser screenShotChooser; // FileChooser to select the file to save the image
	/**
	 * Path to the data file currently active.
	 */
	Path dataFilePath;
	private boolean canContinue;

	public AppActions(ApplicationTemplate applicationTemplate) {
		this.applicationTemplate = applicationTemplate;
		manager = applicationTemplate.manager;

		//set up file chooser for tsd files
		this.tsdFileChooser = new FileChooser();
		tsdFileChooser.getExtensionFilters().add(new ExtensionFilter(manager.getPropertyValue(DATA_FILE_EXT_DESC.name()), manager.getPropertyValue(DATA_FILE_EXT.name())));
		Path current = Paths.get(".").toAbsolutePath();
		Path dataDirectory = current.resolve(manager.getPropertyValue(DATA_RESOURCE_PATH.name()));
		tsdFileChooser.setInitialDirectory(new File(dataDirectory.toString()));

		//set up file chooser for screenshots
		this.screenShotChooser = new FileChooser();
		screenShotChooser.getExtensionFilters().add(new ExtensionFilter(manager.getPropertyValue(PNG_EXT_DESC.name()), manager.getPropertyValue(PNG_EXT.name())));
		Path screenshotDirectory = current.resolve(manager.getPropertyValue(SCREENSHOT_RESOURCE_PATH.name()));
		screenShotChooser.setInitialDirectory(new File(screenshotDirectory.toString()));

		appUI = (AppUI) applicationTemplate.getUIComponent();
	}

	@Override
	public void handleNewRequest() {
		AppData appData = ((AppData) applicationTemplate.getDataComponent());

		if(appData.isFromFile() || !appUI.textAreaShown() || !appUI.isDifferentFromSaved()){
			setUpNewFile();
		}else{
			try {
				if(promptToSave() && canContinue){ // user pressed yes or no 
					setUpNewFile();
					/*
					what would happen if user said yes and then pressed cancel
					*/
				}
			} catch (IOException ex) {
				showErrorDialog(manager.getPropertyValue(IO_ERROR_TITLE.name()),manager.getPropertyValue(IO_SAVE_ERROR_MESSAGE.name()));
			}
		}

		/*
		if data is from a file --> set up automatically
		if no text area is shown --> set up automatically
		if the data is the same as the saved data --> set up automatically
		*/
	}

	/*
	When displaying the file, the processor actually checks the data
	*/
	private void setUpNewFile(){
		AppData appData = (AppData) applicationTemplate.getDataComponent();
		if(checkIsRunning()){
			try{
				appData.stopAlgorithm();
			}catch(NullPointerException e){
				//do nothing
			}
			((AppData) applicationTemplate.getDataComponent()).clear();
			dataFilePath = null;
			appUI.clear();
			appUI.showTextArea();
			appUI.showEditToggle();
		}
	}

	@Override
	public void handleSaveRequest() {
		AppData appData = (AppData) applicationTemplate.getDataComponent();
		String result = appData.validateText(appUI.getTextAreaText());
		/*
		Check if the data in the text area is valid first
		*/
		if(result != null){
			showErrorDialog(manager.getPropertyValue(LOAD_ERROR_TITLE.name()), manager.getPropertyValue(LOAD_ERROR_MESSAGE.name()) + result);
			return;
		}

		if(dataFilePath == null){ //no save file yet
			try{
				if(showSaveDialog()){ // save successfully
					saveFile();
				}else{
					canContinue = false;;
				}
			}catch(IOException e){
				showErrorDialog(manager.getPropertyValue(IO_ERROR_TITLE.name()),manager.getPropertyValue(IO_SAVE_ERROR_MESSAGE.name()));
			}
		}else{
			saveFile();
		}
	}

	private void saveFile(){
		canContinue = true;
		appUI.setSavedText();
		((AppData) applicationTemplate.getDataComponent()).saveData(dataFilePath);
		appUI.disableSaveButton();
	}

	@Override
	public void handleLoadRequest() {
		AppData appData = (AppData) applicationTemplate.getDataComponent();
		if(appData.isFromFile() || !appUI.textAreaShown() || !appUI.isDifferentFromSaved()){
			loadFile();
		}else{
			try {
				if(promptToSave() && canContinue){ // user pressed yes or no 
					loadFile();
				}
			} catch (IOException ex) {
				showErrorDialog(manager.getPropertyValue(IO_ERROR_TITLE.name()),manager.getPropertyValue(IO_SAVE_ERROR_MESSAGE.name()));
			}
		}

		/*
		if data is from a file --> load automatically
		if no text area is shown --> load automatically
		if the data is the same as the saved data --> load automatically
		*/

		//ask if user can save while algorithm is running
		//ask if user can edit configuration while algorithm is running
	}

	private void loadFile(){
		AppData appData = (AppData) applicationTemplate.getDataComponent();
		if(checkIsRunning()){
			try{
				appData.stopAlgorithm();
			}catch(NullPointerException e){
				//do nothing
			}
			File file = tsdFileChooser.showOpenDialog(applicationTemplate.getUIComponent().getPrimaryWindow());
			try {
				String result = appData.validateText(file);
				if(result == null){
					applicationTemplate.getUIComponent().clear();
					appData.loadData(file.toPath());
					dataFilePath = file.toPath();
					appUI.disableSaveButton(); 
				} else {
					showErrorDialog(manager.getPropertyValue(LOAD_ERROR_TITLE.name()), manager.getPropertyValue(LOAD_ERROR_MESSAGE.name()) + result);
				}
			} catch (NullPointerException e) {
				//load cancelled
			}
		}
	}

	@Override
	public void handleExitRequest() {
		AppData appData = (AppData) applicationTemplate.getDataComponent();

		if(appData.isFromFile() || !appUI.textAreaShown() || !appUI.isDifferentFromSaved()){
			exit();
		}else{
			try {
				if(promptToSave() && canContinue){ // user pressed yes or no 
					exit();
				}
			} catch (IOException ex) {
				showErrorDialog(manager.getPropertyValue(IO_ERROR_TITLE.name()),manager.getPropertyValue(IO_SAVE_ERROR_MESSAGE.name()));
			}
		}
	}

	private boolean checkIsRunning(){
		AppData appData = (AppData) applicationTemplate.getDataComponent();
		return ((appData.isRunning() && showTerminateDialog()) || (!appData.isRunning()));
	}

	private void exit(){
		AppData appData = (AppData) applicationTemplate.getDataComponent();
		if(checkIsRunning()){
			try{
				appData.stopAlgorithm();
			}catch(NullPointerException e){
				//do nothing
			}
			Platform.exit();
		}
	}

	@Override
	public void handlePrintRequest() {
		// TODO: NOT A PART OF HW 1
	}

	/**
	 * Actions taken to save a screen shot of the chart
	 * @throws IOException when IO error occurs while saving
	 */
	public void handleScreenshotRequest() throws IOException {
		Chart chart = ((AppUI) applicationTemplate.getUIComponent()).getChart();
		File file = screenShotChooser.showSaveDialog(applicationTemplate.getUIComponent().getPrimaryWindow());
		try{
			WritableImage image = new WritableImage((int) chart.getWidth(), (int) chart.getHeight());
			WritableImage screenshot = chart.snapshot(new SnapshotParameters(), image);
			ImageIO.write(SwingFXUtils.fromFXImage(screenshot, null), manager.getPropertyValue(SCREENSHOT_TYPE.name()), file);

		}catch(IllegalArgumentException e){
			//save cancelled
		}
	}

	/**
	 * Show an error dialog to the user based on parameters
	 * @param title Title of the dialog
	 * @param message Message inside the dialog
	 */
	public void showErrorDialog(String title, String message) {
		Dialog errorDialog = applicationTemplate.getDialog(Dialog.DialogType.ERROR);
		errorDialog.show(title, message);
	}

	/**
	 * This helper method verifies that the user really wants to save their
	 * unsaved work, which they might not want to do. The user will be
	 * presented with three options:
	 * <ol>
	 * <li><code>yes</code>, indicating that the user wants to save the work
	 * and continue with the action,</li>
	 * <li><code>no</code>, indicating that the user wants to continue with
	 * the action without saving the work, and</li>
	 * <li><code>cancel</code>, to indicate that the user does not want to
	 * continue with the action, but also does not want to save the work at
	 * this point.</li>
	 * </ol>
	 *
	 * @return <code>false</code> if the user presses the <i>cancel</i>, and
	 * <code>true</code> otherwise.
	 */
	private boolean promptToSave() throws IOException {
		ConfirmationDialog confirmDialog = (ConfirmationDialog) applicationTemplate.getDialog(Dialog.DialogType.CONFIRMATION);
		confirmDialog.show(manager.getPropertyValue(SAVE_UNSAVED_WORK_TITLE.name()), manager.getPropertyValue(SAVE_UNSAVED_WORK.name()));

		Option option = confirmDialog.getSelectedOption();

		if (option == Option.CANCEL) {
			return false;
		} else {
			if (option == Option.YES) {
				handleSaveRequest();
			}
			if (option == Option.NO){
				canContinue = true;
			}
			return true;
		}
	}
	
	/**
	 * Shows the save dialog to the user
	 * @return true if save was completed, false if save was canceled
	 * @throws IOException when IO error occurs while saving
	 */
	private boolean showSaveDialog() throws IOException {
		File saveFile = tsdFileChooser.showSaveDialog(applicationTemplate.getUIComponent().getPrimaryWindow());
		try {
			saveFile.createNewFile();
			dataFilePath = saveFile.toPath();
		} catch (NullPointerException e) {
			return false; //save cancelled
		}
		return true;
	}

	/**
	 * Shows a dialog to notify the user that an algorithm is running
	 * @return true if user does not want to complete algorithm
	 */
	private boolean showTerminateDialog(){
		/*
		If there is an algorithm running, the user is shown a dialog with a two way choice:
		(a) terminate the algorithm right away and close the application
		(b) return to the application
		*/
		AppData appData = (AppData) applicationTemplate.getDataComponent();
		AlgorithmDialog algorithmDialog = ((DataVisualizer) applicationTemplate).getAlgorithmDialog();
		algorithmDialog.show("Algorithm is running", manager.getPropertyValue(EXIT_WHILE_RUNNING_WARNING.name()));
		AlgorithmDialog.Option option = algorithmDialog.getSelectedOption();

		return option == AlgorithmDialog.Option.YES;
	}
}
