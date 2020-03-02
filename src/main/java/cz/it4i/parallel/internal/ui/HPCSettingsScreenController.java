
package cz.it4i.parallel.internal.ui;

import java.io.File;

import cz.it4i.cluster_job_launcher.HPCSchedulerType;
import cz.it4i.parallel.paradigm_managers.AuthenticationChoice;
import cz.it4i.parallel.paradigm_managers.HPCSettings;
import cz.it4i.swing_javafx_ui.JavaFXRoutines;
import cz.it4i.swing_javafx_ui.SimpleDialog;
import cz.vsb.vas0166.remotefilebrowser.RemoteFileChooserComponent;
import cz.vsb.vas0166.remotefilebrowser.RemoteFile.FileType;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import lombok.Getter;
import lombok.Setter;

public class HPCSettingsScreenController extends AnchorPane {

	@FXML
	private TextField hostTextField;

	@FXML
	private Spinner<Integer> portSpinner;

	@FXML
	private TextField userNameTextField;

	@FXML
	private ToggleGroup authenticationMethod;

	@FXML
	private RadioButton authenticationChoiceKeyRadioButton;

	@FXML
	private RadioButton authenticationChoicePasswordRadioButton;

	@FXML
	private TextField keyFileTextField;

	@FXML
	private PasswordField keyFilePasswordPasswordField;

	@FXML
	private PasswordField passwordPasswordField;

	@FXML
	private ComboBox<String> schedulerTypeComboBox;

	/*@FXML
	private TextField remoteDirectoryTextField;*/

	@FXML
	private RemoteFileChooserComponent remoteDirectory;

	@FXML
	private TextField commandTextField;

	@FXML
	private Spinner<Integer> nodesSpinner;

	@FXML
	private Spinner<Integer> ncpusSpinner;

	@FXML
	private CheckBox shutdownJobAfterCloseCheckBox;

	@FXML
	private CheckBox redirectStdOutErrCheckBox;

	@FXML
	private Button okButton;

	@FXML
	private Button browseButton;

	@Getter
	@Setter
	private HPCSettings settings;

	private static final String PBS_OPTION = "PBS";

	private static final String SLURM_OPTION = "Slurm";

	private static final Integer SPINER_INITIAL_VALUE = 1;

	public HPCSettingsScreenController() {
		JavaFXRoutines.initRootAndController("hpc-settings-screen.fxml", this);
	}

	@FXML
	public void initialize() {
		// RadioButtons:
		authenticationChoiceKeyRadioButton.setSelected(true);

		// ComboBoxes:
		schedulerTypeComboBox.getItems().removeAll(schedulerTypeComboBox
			.getItems());
		schedulerTypeComboBox.getItems().addAll(PBS_OPTION, SLURM_OPTION);
		schedulerTypeComboBox.getSelectionModel().select(PBS_OPTION);

		// Spinners:
		SpinnerValueFactory<Integer> portValueFactory =
			new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 65535,
				SPINER_INITIAL_VALUE);
		portSpinner.setValueFactory(portValueFactory);

		SpinnerValueFactory<Integer> nodesValueFactory =
			new SpinnerValueFactory.IntegerSpinnerValueFactory(1, Integer.MAX_VALUE,
				SPINER_INITIAL_VALUE);
		nodesSpinner.setValueFactory(nodesValueFactory);

		SpinnerValueFactory<Integer> ncpusValueFactory =
			new SpinnerValueFactory.IntegerSpinnerValueFactory(1, Integer.MAX_VALUE,
				SPINER_INITIAL_VALUE);
		ncpusSpinner.setValueFactory(ncpusValueFactory);

		// Disable fields that are not relevant to authentication method selection:
		authenticationChoiceKeyRadioButton.selectedProperty().addListener((
			ObservableValue<? extends Boolean> obs, Boolean wasPreviouslySelected,
			Boolean isNowSelected) -> disableIrrelevantFileds(isNowSelected));
		remoteDirectory.setOutput(FileType.FOLDER);
		remoteDirectory.setConnectionParameters(() -> "ssh://" + hostTextField
			.getText(), userNameTextField::getText, keyFileTextField::getText,
			this::getPassword);
	}

	public void disableIrrelevantFileds(Boolean isSelected) {
		if (isSelected) {
			passwordPasswordField.setDisable(true);
			keyFileTextField.setDisable(false);
			keyFilePasswordPasswordField.setDisable(false);
			browseButton.setDisable(false);
		}
		else {
			keyFileTextField.setDisable(true);
			keyFilePasswordPasswordField.setDisable(true);
			browseButton.setDisable(true);
			passwordPasswordField.setDisable(false);
		}
	}

	@FXML
	private void browseAction() {
		Stage stage = (Stage) browseButton.getScene().getWindow();
		File selectedFile = SimpleDialog.fileChooser(stage, "Open SSH Public Key file");
		if (selectedFile != null) {
			this.keyFileTextField.setText(selectedFile.getAbsolutePath());
		}
	}

	@FXML
	private void okAction() {
		this.settings = createSettings();
		((Stage) getScene().getWindow()).close();
	}

	private HPCSettings createSettings() {
		String host;
		int port;
		AuthenticationChoice authenticationChoice;
		String userName;
		String password;
		File keyFile;
		String keyFilePassword;
		String remoteDirectory;
		String command;
		int nodes;
		int ncpus;
		boolean shutdownJobAfterClose;
		boolean redirectStdOutErr;
		String schedulerType;

		host = hostTextField.getText();
		commitSpinnerValue(portSpinner);
		port = portSpinner.getValue();
		// authenticationChoice
		if (authenticationChoiceKeyRadioButton.isSelected()) {
			authenticationChoice = AuthenticationChoice.KEY_FILE;
		}
		else {
			authenticationChoice = AuthenticationChoice.PASSWORD;
		}
		userName = userNameTextField.getText();
		password = passwordPasswordField.getText();
		keyFile = new File(keyFileTextField.getText());
		keyFilePassword = keyFilePasswordPasswordField.getText();
		remoteDirectory = this.remoteDirectory.getPath();
		command = commandTextField.getText();
		commitSpinnerValue(nodesSpinner);
		nodes = nodesSpinner.getValue();
		commitSpinnerValue(ncpusSpinner);
		ncpus = ncpusSpinner.getValue();
		shutdownJobAfterClose = shutdownJobAfterCloseCheckBox.isSelected();
		redirectStdOutErr = redirectStdOutErrCheckBox.isSelected();
		schedulerType = schedulerTypeComboBox.getSelectionModel().getSelectedItem();

		return HPCSettings.builder().host(host).portNumber(port).userName(userName)
			.authenticationChoice(authenticationChoice).password(password).keyFile(
				keyFile).keyFilePassword(keyFilePassword).remoteDirectory(
					remoteDirectory).command(command).nodes(nodes).ncpus(ncpus)
			.shutdownOnClose(shutdownJobAfterClose).redirectStdInErr(
				redirectStdOutErr).adapterType(HPCSchedulerType.getByString(
					schedulerType)).build();
	}

	private <T> void commitSpinnerValue(Spinner<T> spinner) {
		if (!spinner.isEditable()) return;
		String text = spinner.getEditor().getText();
		SpinnerValueFactory<T> valueFactory = spinner.getValueFactory();
		if (valueFactory != null) {
			StringConverter<T> converter = valueFactory.getConverter();
			if (converter != null) {
				T value = converter.fromString(text);
				valueFactory.setValue(value);
			}
		}
	}

	private String getPassword() {
		if (authenticationChoiceKeyRadioButton.isSelected()) {
			return keyFilePasswordPasswordField.getText();
		}
		return passwordPasswordField.getText();
	}

	public void setInitialFormValues(HPCSettings oldSettings) {
		if (oldSettings == null) {
			hostTextField.setText("localhost");
			portSpinner.getValueFactory().setValue(22);
		}
		else {
			hostTextField.setText(oldSettings.getHost());
			portSpinner.getValueFactory().setValue(oldSettings.getPort());
			userNameTextField.setText(oldSettings.getUserName());
			// Get authentication choice:
			if (oldSettings
				.getAuthenticationChoice() == AuthenticationChoice.KEY_FILE)
			{
				authenticationChoiceKeyRadioButton.setSelected(true);
				authenticationChoicePasswordRadioButton.setSelected(false);
				disableIrrelevantFileds(true);
			}
			else {
				authenticationChoiceKeyRadioButton.setSelected(false);
				authenticationChoicePasswordRadioButton.setSelected(true);
				disableIrrelevantFileds(false);
			}
			keyFileTextField.setText(oldSettings.getKeyFile().getAbsolutePath());
			keyFilePasswordPasswordField.setText(oldSettings.getKeyFilePassword());
			passwordPasswordField.setText(oldSettings.getPassword());
			schedulerTypeComboBox.getSelectionModel().select(oldSettings
				.getAdapterType().toString());
			remoteDirectory.setPath(oldSettings.getRemoteDirectory());
			commandTextField.setText(oldSettings.getCommand());
			nodesSpinner.getValueFactory().setValue(oldSettings.getNodes());
			ncpusSpinner.getValueFactory().setValue(oldSettings.getNcpus());
			shutdownJobAfterCloseCheckBox.setSelected(oldSettings
				.isShutdownOnClose());
			redirectStdOutErrCheckBox.setSelected(oldSettings.isRedirectStdInErr());
		}
	}
}
