package at.jku.isse.ecco.gui.view.graph;

import at.jku.isse.ecco.EccoException;
import at.jku.isse.ecco.EccoService;
import at.jku.isse.ecco.core.Commit;
import at.jku.isse.ecco.core.DependencyGraph;
import at.jku.isse.ecco.gui.ExceptionAlert;
import at.jku.isse.ecco.listener.RepositoryListener;
import at.jku.isse.ecco.plugin.artifact.ArtifactReader;
import at.jku.isse.ecco.plugin.artifact.ArtifactWriter;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Task;
import javafx.embed.swing.SwingNode;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ToolBar;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.BorderPane;
import javafx.stage.FileChooser;
import org.graphstream.graph.Edge;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.SingleGraph;
import org.graphstream.stream.file.FileSink;
import org.graphstream.stream.file.FileSinkFactory;
import org.graphstream.ui.layout.Layout;
import org.graphstream.ui.layout.springbox.implementations.SpringBox;
import org.graphstream.ui.swingViewer.ViewPanel;
import org.graphstream.ui.view.Viewer;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

public class DependencyGraphView extends BorderPane implements RepositoryListener {

	private EccoService service;

	private Graph graph;
	private Layout layout;
	private Viewer viewer;
	private ViewPanel view;

	private boolean showLabels = true;
	private boolean simplifyLabels = true;
	private boolean hideImpliedDependencies = true;


	private DependencyGraph dg = null;


	public DependencyGraphView(EccoService service) {
		this.service = service;


		ToolBar toolBar = new ToolBar();
		this.setTop(toolBar);

		Button refreshButton = new Button("Refresh");

		refreshButton.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent e) {
				toolBar.setDisable(true);
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						dg = new DependencyGraph(DependencyGraphView.this.service.getAssociations());
						DependencyGraphView.this.updateGraph();
					}
				});
				Task refreshTask = new Task<Void>() {
					@Override
					public Void call() throws EccoException {
						//ArtifactsGraphView.this.updateGraph();
						Platform.runLater(() -> {
							toolBar.setDisable(false);
						});
						return null;
					}
				};

				new Thread(refreshTask).start();
			}
		});


		toolBar.getItems().add(refreshButton);


		Button exportButton = new Button("Export");

		exportButton.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent ae) {
				toolBar.setDisable(true);

				FileChooser fileChooser = new FileChooser();
				File selectedFile = fileChooser.showSaveDialog(DependencyGraphView.this.getScene().getWindow());

				if (selectedFile != null) {
					FileSink out = FileSinkFactory.sinkFor(selectedFile.toString());
					try {
						out.writeAll(DependencyGraphView.this.graph, selectedFile.toString());
						out.flush();
					} catch (IOException e) {
						new ExceptionAlert(e).show();
					}
				}

				toolBar.setDisable(false);
			}
		});

		toolBar.getItems().add(exportButton);


		CheckBox showLabelsCheckbox = new CheckBox("Show Labels");
		showLabelsCheckbox.setSelected(this.hideImpliedDependencies);
		toolBar.getItems().add(showLabelsCheckbox);
		showLabelsCheckbox.selectedProperty().addListener(new ChangeListener<Boolean>() {
			public void changed(ObservableValue<? extends Boolean> ov, Boolean old_val, Boolean new_val) {
				DependencyGraphView.this.showLabels = new_val;
				DependencyGraphView.this.updateGraphStylehseet();
			}
		});


		CheckBox simplifyLabelsCheckbox = new CheckBox("Simplified Labels");
		simplifyLabelsCheckbox.setSelected(this.simplifyLabels);
		toolBar.getItems().add(simplifyLabelsCheckbox);
		simplifyLabelsCheckbox.selectedProperty().addListener(new ChangeListener<Boolean>() {
			public void changed(ObservableValue<? extends Boolean> ov, Boolean old_val, Boolean new_val) {
				DependencyGraphView.this.simplifyLabels = new_val;
				DependencyGraphView.this.updateGraph();
			}
		});


		CheckBox hideImpliedDependenciesCheckbox = new CheckBox("Hide Implied Dependencies");
		hideImpliedDependenciesCheckbox.setSelected(this.hideImpliedDependencies);
		toolBar.getItems().add(hideImpliedDependenciesCheckbox);
		hideImpliedDependenciesCheckbox.selectedProperty().addListener(new ChangeListener<Boolean>() {
			public void changed(ObservableValue<? extends Boolean> ov, Boolean old_val, Boolean new_val) {
				DependencyGraphView.this.hideImpliedDependencies = new_val;
				DependencyGraphView.this.updateGraph();
			}
		});


		//System.clearProperty("gs.ui.renderer");
		System.setProperty("gs.ui.renderer", "org.graphstream.ui.j2dviewer.J2DGraphRenderer");


		this.graph = new SingleGraph("DependencyGraph");

		this.layout = new SpringBox(false);
		this.graph.addSink(this.layout);
		this.layout.addAttributeSink(this.graph);

		this.viewer = new Viewer(this.graph, Viewer.ThreadingModel.GRAPH_IN_ANOTHER_THREAD);
		//this.viewer.enableAutoLayout(this.layout);
		this.view = this.viewer.addDefaultView(false); // false indicates "no JFrame"

		SwingNode swingNode = new SwingNode();

		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				swingNode.setContent(view);
			}
		});


		this.setOnScroll(new EventHandler<ScrollEvent>() {
			@Override
			public void handle(ScrollEvent event) {
				view.getCamera().setViewPercent(Math.max(0.1, Math.min(1.0, view.getCamera().getViewPercent() - 0.05 * event.getDeltaY() / event.getMultiplierY())));
			}
		});


		this.setCenter(swingNode);


		showLabelsCheckbox.setSelected(this.showLabels);


		service.addListener(this);

		if (!service.isInitialized())
			this.setDisable(true);
	}


	private void updateGraphStylehseet() {
		String textMode = "text-mode: normal; ";
		if (!this.showLabels)
			textMode = "text-mode: hidden; ";

		this.graph.addAttribute("ui.stylesheet",
				"edge { " + textMode + " size: 1px; shape: blob; arrow-shape: none; arrow-size: 3px, 3px; } " +
						"node { " + textMode + " text-background-mode: plain;  shape: circle; size: 10px; stroke-mode: plain; stroke-color: #000000; stroke-width: 1px; } ");
	}

	private void updateGraph() {
		this.viewer.disableAutoLayout();

		this.graph.removeSink(this.layout);
		this.layout.removeAttributeSink(this.graph);
		this.layout.clear();
		this.graph.clear();

		this.view.getCamera().resetView();


		//this.graph.setStrict(false);

		this.graph.addAttribute("ui.quality");
		this.graph.addAttribute("ui.antialias");

		this.updateGraphStylehseet();


		if (dg == null)
			dg = new DependencyGraph(this.service.getAssociations());

		for (DependencyGraph.Dependency dep : dg.getDependencies()) {
			if (!hideImpliedDependencies || !dep.getFrom().getPresenceCondition().implies(dep.getTo().getPresenceCondition())) {
//			boolean implied = Condition.implies(dep.getFrom().getPresenceCondition(), dep.getTo().getPresenceCondition());
				Node from = this.graph.getNode(String.valueOf(dep.getFrom().getId()));
				if (from == null) {
					from = this.graph.addNode(String.valueOf(dep.getFrom().getId()));
					if (simplifyLabels)
						from.setAttribute("label", "[" + dep.getFrom().getPresenceCondition().getSimpleLabel() + "]");
					else
						from.setAttribute("label", "[" + dep.getFrom().getPresenceCondition().getLabel() + "]");
//				from.setAttribute("implied", implied);
//				if (implied)
//					from.setAttribute("hide");
				}
//			if ((boolean) from.getAttribute("implied") && !implied) {
//				from.setAttribute("implied", false);
//				from.removeAttribute("hide");
//			}
				Node to = this.graph.getNode(String.valueOf(dep.getTo().getId()));
				if (to == null) {
					to = this.graph.addNode(String.valueOf(dep.getTo().getId()));
					if (simplifyLabels)
						to.setAttribute("label", "[" + dep.getTo().getPresenceCondition().getSimpleLabel() + "]");
					else
						to.setAttribute("label", "[" + dep.getTo().getPresenceCondition().getLabel() + "]");
//				to.setAttribute("implied", implied);
//				if (implied)
//					to.setAttribute("hide");
				}
//			if ((boolean) to.getAttribute("implied") && !implied) {
//				to.setAttribute("implied", false);
//				to.removeAttribute("hide");
//			}
				Edge edge = this.graph.addEdge(dep.getFrom().getId() + "-" + dep.getTo().getId(), from, to, true);
				edge.setAttribute("label", String.valueOf(dep.getWeight()));
//			if (implied)
//				edge.setAttribute("hide");
			}
		}


//		while (this.layout.getStabilization() < 0.9) {
//			this.layout.compute();
//		}


		this.graph.addSink(this.layout);
		this.layout.addAttributeSink(this.graph);

		this.viewer.enableAutoLayout(this.layout);
	}


	@Override
	public void statusChangedEvent(EccoService service) {
		if (service.isInitialized()) {
			Platform.runLater(() -> {
				//this.updateGraph();
				this.setDisable(false);
			});
		} else {
			Platform.runLater(() -> {
				this.setDisable(true);
			});
		}
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
