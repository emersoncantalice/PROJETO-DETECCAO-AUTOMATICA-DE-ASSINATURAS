package com.unifacisa.imageMark;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.DragEvent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

/**
 * The JavaFX controller bound to "main.fxml", the main UI window.
 */
public class Controller implements Initializable {

	@FXML
	private MenuItem fileOpen;
	@FXML
	private MenuItem fileExit;
	@FXML
	private CheckMenuItem optionsAutoplay;
	@FXML
	private CheckMenuItem optionsLoop;
	@FXML
	private MenuItem helpAbout;
	@FXML
	private Label status;
	@FXML
	private StackPane content;

	private String[] args;
	private Stage stage;
	private Gallery gallery = new Gallery();

	/**
	 * Called automatically by JavaFX when creating the UI.
	 *
	 * @param location
	 * @param resources
	 */
	@Override
	public void initialize(final URL location, final ResourceBundle resources) {
		initializeMenuBar();
		initializeStatusBar();
		initializeDragAndDrop();

		// TODO: Add event handlers (or a bidirectional property?) to
		// synchronize the slider position when the image changes size through
		// other means

		// Load the initially-selected file, if there was one
		if (args != null && args.length > 0) {
			loadFile(new File(args[0]));
		}
	}

	private void initializeStatusBar() {
		status.textProperty().bind(gallery.statusProperty());
	}

	/**
	 * <p>
	 * Processes key events, to scroll through the gallery items when arrow keys
	 * are pressed.
	 * </p>
	 *
	 * <p>
	 * It doesn't seem possible to register key event handlers for the main
	 * window from this controller class. So <code>Main</code> has to register
	 * the handler, and pass events here via this method. There could
	 * potentially be thread safety issues (i.e. new scrolling operations coming
	 * in before the previous operation finishes rendering), but I'm
	 * <i>somewhat</i> sure that the synchronous nature of this method and the
	 * single-threadedness of <code>Main</code> prevents that. Still, there's
	 * probably a better way to approach this.
	 * </p>
	 *
	 * @param event
	 */
	void keyPressedEvent(KeyEvent event) {
		if (!gallery.isEmpty()) {
			if (event.getCode().equals(KeyCode.RIGHT) || event.getCode().equals(KeyCode.DOWN)) {
				renderNext();
			} else if (event.getCode().equals(KeyCode.LEFT) || event.getCode().equals(KeyCode.UP)) {
				renderPrevious();
			}
		}
	}

	void markEvent(KeyEvent event) {
		if (!gallery.isEmpty()) {
			if (event.getCode().equals(KeyCode.M)) {
				System.out.println("Apertou M");
			}
		}
	}

	/**
	 * Registers event handlers for the menu bar actions.
	 */
	private void initializeMenuBar() {
		fileOpen.setOnAction(actionEvent -> {
			final FileChooser fileChooser = new FileChooser();
			fileChooser.setInitialDirectory(gallery.directory());
			fileChooser
					.getExtensionFilters().addAll(
							new FileChooser.ExtensionFilter("All supported files",
									GalleryItem.allExtensions.stream().map(ext -> "*" + ext)
											.collect(Collectors.toList())),
							new FileChooser.ExtensionFilter("Images",
									GalleryItem.imageExtensions.stream().map(ext -> "*" + ext)
											.collect(Collectors.toList())),
							new FileChooser.ExtensionFilter("Audio/Video", GalleryItem.videoExtensions.stream()
									.map(ext -> "*" + ext).collect(Collectors.toList())));
			final File file = fileChooser.showOpenDialog(null);
			loadFile(file);
		});
		fileExit.setOnAction(actionEvent -> Platform.exit());
		helpAbout.setOnAction(actionEvent -> {
			final Alert dialog = new Alert(Alert.AlertType.NONE,
					"Unifacisa - Image View\nby Projeto: detecção de assinaturas\n\n", ButtonType.CLOSE);
			dialog.setTitle("About");
			((Stage) dialog.getDialogPane().getScene().getWindow()).getIcons()
					.add(new Image(getClass().getResource("/icon.png").toString()));
			dialog.showAndWait();
		});
	}

	/**
	 * Initializes the controls and status label on the status bar.
	 */

	/**
	 * Registers event handlers for loading files by drag-n-dropping them onto
	 * the window's main content area.
	 */
	private void initializeDragAndDrop() {
		content.addEventHandler(DragEvent.DRAG_OVER, event -> {
			if (event.getDragboard().hasFiles() && GalleryItem.create(event.getDragboard().getFiles().get(0)) != null) {
				event.acceptTransferModes(TransferMode.LINK);
			} else {
				event.consume();
			}
		});
		content.addEventHandler(DragEvent.DRAG_DROPPED, event -> {
			if (event.getDragboard().hasFiles()) {
				loadFile(event.getDragboard().getFiles().get(0));
			}
		});
	}

	/**
	 * <p>
	 * Called when a file is explicitly selected by the user (i.e. passed as a
	 * command-line parameter, drag-n-dropped onto the executable icon,
	 * drag-n-dropped onto the application window after launch, or selected from
	 * the File->Open menu item.
	 * </p>
	 *
	 * <p>
	 * Populates the gallery with all supported files in the same directory as
	 * the explicitly-selected file, and renders that explicitly-selected file.
	 * Or else does nothing if the selected file isn't a supported media item.
	 * </p>
	 *
	 * @param file
	 */
	private void loadFile(final File file) {
		if (gallery != null) {
			gallery.clear();
		}
		final GalleryItem item = GalleryItem.create(file);
		if (item == null)
			return;

		gallery.add(item);
		gallery.addAll(findSiblingItems(item));
		render(item);
	}

	/**
	 * <p>
	 * Finds all files in the same directory as the parameter item. The
	 * parameter file will be positioned as the first element in the resulting
	 * list. If the parameter file is null, then a non-null empty list will be
	 * returned.
	 * </p>
	 *
	 * <p>
	 * Note that the returned list will contain <code>null</code> elements for
	 * the files that are not supported media items. The application relies upon
	 * the {@link Gallery} methods stripping out <code>null</code>'s.
	 * </p>
	 *
	 * @param item
	 * @return
	 */
	private List<GalleryItem> findSiblingItems(final GalleryItem item) {
		final List<GalleryItem> returnValue = new ArrayList<>(Arrays.asList(item));
		if (item == null)
			return returnValue;
		returnValue.addAll(Arrays.stream(item.getItem().getParentFile().listFiles())
				.filter(sibling -> item.getItem().isFile() && !item.getItem().equals(sibling)).map(GalleryItem::create)
				.collect(Collectors.toList()));
		return returnValue;
	}

	/**
	 * Renders the next item in the gallery.
	 */
	private void renderNext() {
		if (gallery == null)
			return;
		render(gallery.next());
	}

	/**
	 * Renders the previous item in the gallery.
	 */
	private void renderPrevious() {
		if (gallery == null)
			return;
		render(gallery.previous());
	}

	/**
	 * Functionality common to {@link Controller#renderNext()} and
	 * {@link Controller#renderPrevious()}.
	 *
	 * @param item
	 */
	private void render(final GalleryItem item) {
		if (item == null)
			return;
		stage.setTitle("MediaGallery - " + item.getItem().getName());

		if (content.getChildren().size() > 0 && content.getChildren().get(0) instanceof ImageView) {
			// Stop the status bar slider from resizing any previous image (this
			// will be a no-op if there is no
			// existing listener)

		} else if (content.getChildren().size() > 0 && content.getChildren().get(0) instanceof MediaControl) {
			// If the currently rendered item is a video, stop its player before
			// proceeding
			final MediaControl previousMediaControl = (MediaControl) content.getChildren().get(0);
			previousMediaControl.getMediaPlayer().dispose();
		}

		if (item.isImage()) {
			renderImage(item.getItem());
		} else if (item.isVideo()) {
			System.out.println("O arquivo não é uma imagem");
		}
	}

	/**
	 * Renders a given gallery item as an image.
	 *
	 * @param item
	 */
	private void renderImage(final File item) {
		try {
			final String imageURL = item.toURI().toURL().toExternalForm();
			final ImageView imageView = new ImageView(new Image(imageURL));
			imageView.setPreserveRatio(true);
			imageView.fitWidthProperty().bind(content.widthProperty());
			imageView.fitHeightProperty().bind(content.heightProperty());
			content.getChildren().clear();
			content.getChildren().add(imageView);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
	}

	/**
	 * <p>
	 * Called by event handlers (i.e. the "Actual Size" button and the size
	 * slider on the status bar) to shrink or grow the size of the displayed
	 * image.
	 * </p>
	 *
	 * <p>
	 * By default, images automatically shrink or grow to fit the content area.
	 * Once a resize operation has been explicitly called, this switches from
	 * automatic to manual (until the "Fit to Window Size" button toggles it
	 * back). This method resizes the image by effectively removing the previous
	 * <code>ImageView</code> altogether, and replacing it with a new instance
	 * having the appropriate dimensions.
	 * </p>
	 *
	 * @param ratio
	 */

	/**
	 * Allows {@link Main#start(Stage)} to inject the primary {@link Stage} for
	 * the JavaFX window, so that this controller can update the title bar when
	 * loading media items.
	 *
	 * @param stage
	 */
	public void setStage(final Stage stage) {
		this.stage = stage;
	}

	/**
	 * Allows {@link Main#start(Stage)} to inject the arguments originally
	 * passed at application invocation, so that {@link this#initialize(URL,
	 * ResourceBundle)} can determine if a filename to load was passed at
	 * startup.
	 *
	 * @param args
	 */
	public void setArgs(final String[] args) {
		this.args = args;
	}
}
