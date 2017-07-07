package com.unifacisa.imageMark;

import java.awt.event.ActionEvent;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.value.ChangeListener;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Bounds;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.DragEvent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
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
	@FXML
	private Button sizeButton;
	@FXML
	private Button resetMarcacao;
	@FXML
	private Slider sizeSlider;
	@FXML
	private Text coords;
	@FXML
	private Text nameImage;
	@FXML
	private Text pontoInicial;
	@FXML
	private Text alturaLinha;
	@FXML
	private Text statusMarcacao;
	@FXML
	private Text pontoFinal;
	@FXML
	private Text coord1;
	@FXML
	private Text coord2;
	@FXML
	private Text coord3;
	@FXML
	private RadioButton buttonFaltantes;
	@FXML
	private RadioButton buttonPresentes;

	private String[] args;
	private Stage stage;
	private Gallery gallery = new Gallery();
	private boolean fitsize = true;
	private ChangeListener<? super Number> sizeSliderListener;
	private ImageView imageView;
	private ScrollPane scrollPane;
	private ArrayList<String> coordenadas;
	private List<Node> listNodesRectangles;

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

		// Load the initially-selected file, if there was one
		if (args != null && args.length > 0) {
			loadFile(new File(args[0]));
		}
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
			if (event.getCode().equals(KeyCode.RIGHT)) {
				renderNext();
			} else if (event.getCode().equals(KeyCode.LEFT)) {
				renderPrevious();
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
			fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("Images",
					GalleryItem.imageExtensions.stream().map(ext -> "*" + ext).collect(Collectors.toList())));
			final File file = fileChooser.showOpenDialog(null);
			loadFile(file);
		});

		fileExit.setOnAction(actionEvent -> Platform.exit());

		helpAbout.setOnAction(actionEvent -> {
			final Alert dialog = new Alert(Alert.AlertType.NONE,
					"Unifacisa - Image Mark\n Projeto: detecção de assinaturas\n\n", ButtonType.CLOSE);
			dialog.setTitle("Sobre");

			((Stage) dialog.getDialogPane().getScene().getWindow()).getIcons()
					.add(new Image(getClass().getResource("/icon.png").toString()));
			dialog.showAndWait();
		});

		buttonPresentes.setOnAction(actionEvent -> {

		});

		buttonFaltantes.setOnAction(actionEvent -> {

		});

		resetMarcacao.setOnAction(ActionEvent -> {
			ScrollPane sp = (ScrollPane) content.getChildren().get(0);
			StackPane st = (StackPane) sp.getContent();
			AnchorPane ap = (AnchorPane) st.getChildren().get(0);
			ap.getChildren().removeAll(listNodesRectangles);
			coordenadas = new ArrayList<String>();
			statusMarcacao.setText("Marcação reiniciada, aguardando marcação inicial");
			coord1.setText("0,0");
			coord2.setText("0,0");
			coord3.setText("0,0");
		});

		buttonFaltantes.setDisable(true);
		buttonPresentes.setDisable(true);
		resetMarcacao.setDisable(true);
	}

	/**
	 * Initializes the controls and status label on the status bar.
	 */
	private void initializeStatusBar() {
		status.textProperty().bind(gallery.statusProperty());
		sizeButton.setOnAction(event -> {
			// Validate that the main content area contains an ImageView, either
			// directly or within scrollbars
			if (content.getChildren().size() < 1)
				return;
			final Node contentNode = content.getChildren().get(0);
			if (!(contentNode instanceof ImageView || contentNode instanceof ScrollPane))
				return;

			if (fitsize) {
				// Switch to actual size
				final ImageView sizeButtonImageView = new ImageView(
						new Image(getClass().getResourceAsStream("/fitsizebutton.png")));
				sizeButton.setGraphic(sizeButtonImageView);
				fitsize = false;
				resizeImage(1.0);
			} else {
				// Switch to window fit size
				final ImageView sizeButtonImageView = new ImageView(
						new Image(getClass().getResourceAsStream("/actualsizebutton.png")));
				sizeButton.setGraphic(sizeButtonImageView);
				fitsize = true;

				ImageView imageView;
				if (contentNode instanceof ImageView) {
					imageView = (ImageView) contentNode;
				} else {
					final ScrollPane scrollPane = (ScrollPane) contentNode;
					final StackPane stackPane = (StackPane) scrollPane.getContent();
					imageView = (ImageView) stackPane.getChildren().get(0);
				}
				content.getChildren().clear();
				imageView.fitWidthProperty().unbind();
				imageView.fitHeightProperty().unbind();
				imageView.fitWidthProperty().bind(content.widthProperty());
				imageView.fitHeightProperty().bind(content.heightProperty());
				imageView.setViewport(null);
				content.getChildren().add(imageView);
			}
			sizeSlider.setValue(0);
			content.requestFocus();
		});

		sizeSliderListener = (observable, oldValue, newValue) -> {
			fitsize = false;
			final ImageView actualSizeImageView = new ImageView(
					new Image(getClass().getResourceAsStream("/fitsizebutton.png")));
			sizeButton.setGraphic(actualSizeImageView);

			double ratio = 1 + (sizeSlider.getValue() / 100);
			ratio = ratio == 0 ? 0.01 : ratio;
			resizeImage(ratio);
		};
		sizeSlider.setOnMouseReleased(event -> content.requestFocus());
		sizeSlider.setOnKeyReleased(event -> content.requestFocus());
	}

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
	private void resizeImage(final double ratio) {
		// Validate that the main content area contains an ImageView, either
		// directly or nested within scrollbars
		if (content.getChildren().size() < 1)
			return;
		final Node contentNode = content.getChildren().get(0);
		if (!(contentNode instanceof ImageView) && !(contentNode instanceof ScrollPane))
			return;

		// Clear the current main content area and ImageView settings
		ImageView imageView;
		if (contentNode instanceof ImageView) {
			imageView = (ImageView) contentNode;
		} else {
			final ScrollPane scrollPane = (ScrollPane) contentNode;
			final StackPane stackPane = (StackPane) scrollPane.getContent();
			final AnchorPane anchorPane = (AnchorPane) stackPane.getChildren().get(0);
			imageView = (ImageView) anchorPane.getChildren().get(0);
		}
		imageView.fitWidthProperty().unbind();
		imageView.fitHeightProperty().unbind();
		imageView.setViewport(null);
		content.getChildren().clear();

		// Calculate size
		final double imageWidth = imageView.getImage().getWidth() * ratio;
		final double imageHeight = imageView.getImage().getHeight() * ratio;
		imageView.setFitWidth(imageWidth);
		imageView.setFitHeight(imageHeight);

		// Resize the image
		if (imageWidth > content.getWidth() || imageHeight > content.getHeight()) {
			// If the image is bigger than the main content area, add scroll
			// bars
			final AnchorPane anchor = new AnchorPane(imageView);
			anchor.setLayoutX(imageView.getFitHeight());
			anchor.setLayoutY(imageView.getFitWidth());
			final StackPane stackPane = new StackPane(anchor);
			stackPane.setLayoutX(imageView.getFitHeight());
			stackPane.setLayoutY(imageView.getFitWidth());
			stackPane.minWidthProperty().bind(Bindings.createDoubleBinding(
					() -> scrollPane.getViewportBounds().getWidth(), scrollPane.viewportBoundsProperty()));
			scrollPane = new ScrollPane(stackPane);
			content.getChildren().add(scrollPane);
		}
		// else {
		// // If the image fits within the main content area, re-add it as-is
		// content.getChildren().add(imageView);
		// }
		fitsize = false;
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
		stage.setTitle("Image Mark - " + item.getItem().getName());

		if (content.getChildren().size() > 0 && content.getChildren().get(0) instanceof ImageView) {
			// Stop the status bar slider from resizing any previous image (this
			// will be a no-op if there is no
			// existing listener)
			sizeSlider.valueProperty().removeListener(sizeSliderListener);

		} else if (content.getChildren().size() > 0 && content.getChildren().get(0) instanceof MediaControl) {
			// If the currently rendered item is a video, stop its player before
			// proceeding
			final MediaControl previousMediaControl = (MediaControl) content.getChildren().get(0);
			previousMediaControl.getMediaPlayer().dispose();
		}

		if (item.isImage()) {
			renderImage(item.getItem());
		} else {
			System.out.println("O arquivo não é uma imagem");
		}
	}

	/**
	 * Renders a given gallery item as an image.
	 *
	 * @param item
	 */
	public void renderImage(final File item) {

		try {
			final String imageURL = item.toURI().toURL().toExternalForm();
			imageView = new ImageView(new Image(imageURL));
			StackPane stackPane = new StackPane();
			AnchorPane anchor = new AnchorPane();
			statusMarcacao.setText("Aguardando marcação inicial");
			coord1.setText("0,0");
			coord2.setText("0,0");
			coord3.setText("0,0");
			nameImage.setText(item.getName());
			coordenadas = new ArrayList<String>();

			imageView.addEventFilter(MouseEvent.MOUSE_CLICKED, new EventHandler<MouseEvent>() {

				private double x;
				private double y;
				private double x2;
				private double y2;
				private double x3;
				private double y3;
				private List<RetanguloAssinatura> retangulos;
				private RetanguloAssinatura rOut;
				

				@Override
				public void handle(MouseEvent mouseEvent) {

					if (coordenadas.size() == 0) {
						coordenadas.add((int) mouseEvent.getX() + "," + (int) mouseEvent.getY() + "px");
						coord1.setText((int) mouseEvent.getX() + "," + (int) mouseEvent.getY());
						statusMarcacao.setText("Aguardando marcação de altura de linha");
						resetMarcacao.setDisable(false);
						x = mouseEvent.getX();
						y = mouseEvent.getY();
					} else if (coordenadas.size() == 1) {
						coordenadas.add((int) mouseEvent.getX() + "," + (int) mouseEvent.getY() + "px");
						coord2.setText((int) mouseEvent.getX() + "," + (int) mouseEvent.getY());
						statusMarcacao.setText("Aguardando marcação final");
						x2 = mouseEvent.getX();
						y2 = mouseEvent.getY();
					} else if (coordenadas.size() == 2) {
						coordenadas.add((int) mouseEvent.getX() + "," + (int) mouseEvent.getY() + "px");
						coord3.setText((int) mouseEvent.getX() + "," + (int) mouseEvent.getY());
						statusMarcacao.setText("Marcação finalizada!");
						x3 = mouseEvent.getX();
						y3 = mouseEvent.getY();

						// anchor.minWidthProperty().bind(Bindings.createDoubleBinding(()
						// -> scrollPane.getViewportBounds().getWidth(),
						// scrollPane.viewportBoundsProperty()));
						anchor.getChildren().clear();
						anchor.setLayoutX(imageView.getFitHeight());
						anchor.setLayoutY(imageView.getFitWidth());
						anchor.opacityProperty().set(1);

						stackPane.getChildren().clear();
						stackPane.setLayoutX(imageView.getFitHeight());
						stackPane.setLayoutY(imageView.getFitWidth());
						stackPane.opacityProperty().set(1);

						int quantidadeRetangulos = (int) Math.round((((y3 - 2) - (y - 2)) / ((y2 - 2) - (y - 2))));
						System.out.println("Quantidade de retangulos = " + quantidadeRetangulos + " - Real = "
								+ ((y3 - y) / (y2 - y)));

						double pontoInicialX = x;
						double pontoInicialY = y;
						double largura = x3 - x2;
						double altura = y2 - y;
						retangulos = calculaRetangulos(quantidadeRetangulos, largura, altura, pontoInicialY,
								pontoInicialX);

						Rectangle rectangle = null;
						listNodesRectangles = new ArrayList<Node>();
						for (RetanguloAssinatura retangulo : retangulos) {
							rectangle = new Rectangle(retangulo.getPontoInicialX(), retangulo.getPontoInicialY(),
									retangulo.getLargura(), retangulo.getAltura());
							rectangle.setStroke(Color.RED);
							rectangle.setStrokeWidth(5);
							rectangle.opacityProperty().set(0.5);
							rectangle.setCursor(Cursor.HAND);
							rectangle.addEventFilter(MouseEvent.MOUSE_CLICKED, new EventHandler<MouseEvent>() {
								@Override
								public void handle(MouseEvent mouseEvent) {
									Rectangle rectangleSelect = (Rectangle) mouseEvent.getSource();
									rectangleSelect.setStroke(Color.GREEN);
									System.out.println("Clique no retangulo " + rectangleSelect);
								}
							});
							listNodesRectangles.add(rectangle);
						}

						anchor.getChildren().add(imageView);
						anchor.getChildren().addAll(listNodesRectangles);
						stackPane.getChildren().add(anchor);
						ScrollPane sp = (ScrollPane) content.getChildren().get(0);
						content.getChildren().clear();
						sp.setContent(stackPane);
						content.getChildren().add(sp);
					} else {
						System.out.println("Marcação para essa imagem finalizada.");
					}

				}

				private List<RetanguloAssinatura> calculaRetangulos(int quantidadeRetangulos, double largura,
						double altura, double pontoInicialY, double pontoInicialX) {

					double pontoInicialInc = pontoInicialY;

					List<RetanguloAssinatura> retorno = new ArrayList<RetanguloAssinatura>();
					RetanguloAssinatura r;
					for (int i = 1; i <= quantidadeRetangulos; i++) {
						r = new RetanguloAssinatura();
						r.setId((int) i);
						r.setPontoInicialY(pontoInicialInc);
						r.setPontoInicialX(pontoInicialX);
						r.setAltura(altura);
						r.setLargura(largura);
						pontoInicialInc += altura;
						retorno.add(r);
					}
					return retorno;
				}
			});

			imageView.addEventFilter(MouseEvent.MOUSE_MOVED, new EventHandler<MouseEvent>() {
				@Override
				public void handle(MouseEvent mouseEvent) {
					coords.setText((int) mouseEvent.getX() + "," + (int) mouseEvent.getY() + "px");
				}
			});

			imageView.setPreserveRatio(true);
			imageView.fitWidthProperty().bind(content.widthProperty());
			imageView.fitHeightProperty().bind(content.heightProperty());
			imageView.setCursor(Cursor.CROSSHAIR);
			imageView.fitWidthProperty().unbind();
			imageView.fitHeightProperty().unbind();
			imageView.setViewport(null);
			final double imageWidth = imageView.getImage().getWidth() * 1;
			final double imageHeight = imageView.getImage().getHeight() * 1;
			imageView.setFitWidth(imageWidth);
			imageView.setFitHeight(imageHeight);

			content.getChildren().clear();

			anchor.setLayoutX(imageView.getFitHeight());
			anchor.setLayoutY(imageView.getFitWidth());
			anchor.opacityProperty().set(1);

			stackPane.setLayoutX(imageView.getFitHeight());
			stackPane.setLayoutY(imageView.getFitWidth());
			stackPane.opacityProperty().set(1);

			anchor.getChildren().add(imageView);
			stackPane.getChildren().add(anchor);
			scrollPane = new ScrollPane(stackPane);
			// content.getChildren().clear();
			content.getChildren().add(scrollPane);
			sizeButton.setDisable(false);
			sizeSlider.setDisable(false);
			sizeSlider.valueProperty().addListener(sizeSliderListener);
			// resizeImage(1.0);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
	}

	@SuppressWarnings("deprecation")
	void markEvent(KeyEvent event) {
		if (!gallery.isEmpty()) {
			if (event.getCode().equals(KeyCode.M)) {
				System.out.println("Apertou M");
				System.out.println(imageView.getImage().impl_getUrl());
				Bounds boundsInScreen = imageView.localToScreen(imageView.getBoundsInLocal());
				System.out.println(boundsInScreen);
			}
		}
	}

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