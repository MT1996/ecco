package at.jku.isse.ecco.gui;


import at.jku.isse.ecco.EccoException;
import at.jku.isse.ecco.EccoService;
import at.jku.isse.ecco.core.Commit;
import at.jku.isse.ecco.listener.RepositoryListener;
import at.jku.isse.ecco.plugin.artifact.ArtifactReader;
import at.jku.isse.ecco.plugin.artifact.ArtifactWriter;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

public class InitView extends BorderPane implements RepositoryListener {

	private EccoService eccoService;

	private Label pathLabel;
	private Button initButton;
	private Label statusLabel;

	public InitView(EccoService eccoService) {
		this.eccoService = eccoService;

		VBox content = new VBox();
		content.setAlignment(Pos.CENTER);
		content.setPadding(new Insets(10, 10, 10, 10));


		//content.getChildren().add(new Label("Repository Directory: "));


		this.pathLabel = new Label();
		this.pathLabel.setWrapText(true);
		content.getChildren().add(this.pathLabel);
		content.setMargin(this.pathLabel, new Insets(10, 10, 10, 10));


		Button chooseDirButton = new Button();
		chooseDirButton.setText("Select Directory");
		content.getChildren().add(chooseDirButton);
		content.setMargin(chooseDirButton, new Insets(10, 10, 10, 10));


		this.statusLabel = new Label();
		content.getChildren().add(this.statusLabel);
		content.setMargin(this.statusLabel, new Insets(10, 10, 10, 10));


		this.initButton = new Button();
		content.getChildren().add(this.initButton);
		content.setMargin(this.initButton, new Insets(10, 10, 10, 10));


		chooseDirButton.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				DirectoryChooser directoryChooser = new DirectoryChooser();
				File selectedDirectory = directoryChooser.showDialog(InitView.this.getScene().getWindow());

				if (selectedDirectory != null) {
					InitView.this.updatePath(selectedDirectory.toPath());
				}
			}
		});


		this.initButton.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				InitView.this.setDisable(true);
				new Thread(new Task<Void>() {
					@Override
					public Void call() throws EccoException, IOException {
						if (InitView.this.eccoService.repositoryDirectoryExists()) {
							InitView.this.eccoService.init();
						} else {
							InitView.this.eccoService.createRepository();
						}
						Platform.runLater(() -> {
							InitView.this.setDisable(false);
						});
						return null;
					}

					@Override
					public void failed() {
						super.failed();

						ExceptionAlert alert = new ExceptionAlert(this.getException());
						alert.setTitle("Init Error");
						alert.setHeaderText("Init Error");

						alert.showAndWait();

						InitView.this.setDisable(false);
					}
				}).start();
			}
		});


		this.updatePath(this.eccoService.getRepositoryDir().getParent());


		this.setCenter(content);
	}


	private void updatePath(Path path) {
		Path repoDir = path.resolve(EccoService.REPOSITORY_DIR_NAME);
		Path baseDir = path;

		this.eccoService.setRepositoryDir(repoDir);
		this.eccoService.setBaseDir(baseDir);

		this.pathLabel.setText(this.eccoService.getRepositoryDir().toString());

		if (this.eccoService.repositoryDirectoryExists()) {
			this.statusLabel.setText("An existing repository was detected.");
			this.initButton.setText("Load Existing Repository");
		} else {
			this.statusLabel.setText("There is no repository at this location.");
			this.initButton.setText("Create New Repository");
		}
	}


	@Override
	public void statusChangedEvent(EccoService service) {
		Platform.runLater(() -> {
			this.updatePath(service.getRepositoryDir());
		});
	}

	@Override
	public void commitsChangedEvent(EccoService service, Commit commit) {

	}

	@Override
	public void fileReadEvent(Path file, ArtifactReader reader) {

	}

	@Override
	public void fileWriteEvent(Path file, ArtifactWriter writer) {

	}

}
