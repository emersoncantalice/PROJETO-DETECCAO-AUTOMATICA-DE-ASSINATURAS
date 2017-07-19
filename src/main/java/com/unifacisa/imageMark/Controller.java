package com.unifacisa.imageMark;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Scanner;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;

import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Rectangle2D;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ScrollPane;
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
import javafx.scene.shape.StrokeType;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

/**
 * The JavaFX controller bound to "main.fxml", the main UI window.
 */
public class Controller implements Initializable {

	private static final String SELECIONE_A_ALTURA_DIREITA_MAXIMA_NA_TABELA = "Selecione a altura direita M�XIMA na tabela";
	private static final String SELECIONE_A_ALTURA_DIREITA_MINIMA_NA_TABELA = "Selecione a altura direita M�NIMA na tabela";
	private static final double CONSTANTE_DE_CRESCIMENTO_DE_ROTACAO = -0.0478260869565217;
	private static final String VAZIO = "";
	private static final String TXT_EXTENSION = ".txt";
	private static final String PONTO = ".";
	private static final String FALTANTES_SELECIONADO = "F";
	private static final String PRESENTES_SELECIONADO = "P";
	private static final String MARCACAO_REINICIADA_AGUARDANDO_MARCACAO_INICIAL = "Marca��o reiniciada, aguardando marca��o inicial";
	private static final byte[] SEPARADOR_FILE_LOG = "#@".getBytes();
	private static final String SEPARADOR_FILE = "#@";
	private static final String MARCACAO_PARA_ESSA_IMAGEM_FINALIZADA = "Marca��o para essa imagem finalizada";
	private static final String PX = "px";
	private static final String VIRGULA = ",";
	private static final String ZERO_STRING = "0,0";
	private static final String AGUARDANDO_MARCACAO_INICIAL = "Aguardando marca��o inicial";
	private static final String AGUARDANDO_MARCACAO_DE_ALTURA_DE_LINHA = "Aguardando marca��o de altura de linha";
	private static final String AGUARDANDO_MARCACAO_FINAL = "Aguardando marca��o final";
	private static final String MARCACAO_FINALIZADA = "Marca��o finalizada!\nTecle 'S' para salvar.";
	private static final String SEPARADOR = System.getProperty("file.separator");
	protected static final String IMAGEM_ROTACIONADA_AGUARDANDO_MARCACAO = "Imagem rotacionada, aguardando marca��o";

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
	private MenuItem tutorial;
	@FXML
	private Text status;
	@FXML
	private StackPane content;
	@FXML
	private StackPane contentRight;
	@FXML
	private AnchorPane imageZoom;
	@FXML
	private Button resetMarcacao;
	@FXML
	private Button rotate;
	@FXML
	private Button outFocus;
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
	private ImageView imageView;
	private ScrollPane scrollPane;
	private ArrayList<String> coordinates;
	private ArrayList<String> coordinatesRotacionar;
	private List<Node> listNodesRectangles;
	private GalleryItem itemCurrent;
	private double x0;
	private double y0;
	private double x1;
	private double y1;
	private double x2;
	private double y2;
	private List<RetanguloAssinatura> retangulos;
	private boolean refazer = false;
	private boolean rotacionar = false;
	private ImageView imgZoom;

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
			} else if (event.getCode().equals(KeyCode.S)) {
				if (statusMarcacao.getText().equals(MARCACAO_FINALIZADA)) {
					salvarLog(coordinates, listNodesRectangles);
					reset();
				} else {
					final Alert dialog = new Alert(Alert.AlertType.NONE,
							"Marca��o ainda n�o foi finalizada.\n\nPara salvar � necessario chegar ao menos no passo 4 da marca��o.\nQuaquer d�vida clique em 'Ajuda -> Tutorial', no menu superior. ",
							ButtonType.CLOSE);
					dialog.setTitle("Observe.");

					((Stage) dialog.getDialogPane().getScene().getWindow()).getIcons()
							.add(new Image(getClass().getResource("/icon.png").toString()));
					dialog.showAndWait();
				}
				renderNext();
			}
		}
	}

	private void salvarLog(ArrayList<String> coordenadas, List<Node> listNodesRectangles2) {
		String fileNameFull = itemCurrent.getItem().getName();
		String fileNameOutExtension = fileNameFull.substring(0, fileNameFull.lastIndexOf(PONTO));
		if (new File(itemCurrent.getItem().getParentFile() + SEPARADOR + fileNameOutExtension + TXT_EXTENSION)
				.exists()) {
			final Alert dialog = new Alert(Alert.AlertType.WARNING, "Arquivo j� existe! Deseja sobrecrever?\n\n",
					ButtonType.NO, ButtonType.YES);
			dialog.setTitle("Image View - Alerta!");

			((Stage) dialog.getDialogPane().getScene().getWindow()).getIcons()
					.add(new Image(getClass().getResource("/icon.png").toString()));
			dialog.showAndWait();

			if (dialog.getResult().equals(ButtonType.YES)) {
				File arquivo = new File(
						itemCurrent.getItem().getParentFile() + SEPARADOR + fileNameOutExtension + TXT_EXTENSION);
				saveLog(arquivo);
			} else {
				renderNext();
			}

		} else {
			File arquivo = new File(
					itemCurrent.getItem().getParentFile() + SEPARADOR + fileNameOutExtension + TXT_EXTENSION);
			saveLog(arquivo);

		}
	}

	private void saveLog(File arquivo) {
		try (FileOutputStream fo = new FileOutputStream(arquivo)) {
			BufferedOutputStream bos = new BufferedOutputStream(fo);

			String[] coord0 = coord1.getText().split(VIRGULA);
			String[] coord1 = coord2.getText().split(VIRGULA);
			String[] coord2 = coord3.getText().split(VIRGULA);

			bos.write((coord0[0].getBytes()));
			bos.write(SEPARADOR_FILE_LOG);
			bos.write((coord0[1].getBytes()));
			bos.write(SEPARADOR_FILE_LOG);

			bos.write((coord1[0].getBytes()));
			bos.write(SEPARADOR_FILE_LOG);
			bos.write((coord1[1].getBytes()));
			bos.write(SEPARADOR_FILE_LOG);

			bos.write((coord2[0].getBytes()));
			bos.write(SEPARADOR_FILE_LOG);
			bos.write((coord2[1].getBytes()));
			bos.write(SEPARADOR_FILE_LOG);

			if (buttonFaltantes.isSelected()) {
				bos.write((FALTANTES_SELECIONADO.getBytes()));
				bos.write(SEPARADOR_FILE_LOG);
				for (Node node : listNodesRectangles) {
					Rectangle r = (Rectangle) node;
					if (r.getStroke().equals(Color.DARKRED)) {
						bos.write((r.getX() + VAZIO).getBytes());
						bos.write(SEPARADOR_FILE_LOG);
						bos.write((r.getY() + VAZIO).getBytes());
						bos.write(SEPARADOR_FILE_LOG);
						bos.write((r.getWidth() + VAZIO).getBytes());
						bos.write(SEPARADOR_FILE_LOG);
						bos.write((r.getHeight() + VAZIO).getBytes());
						bos.write(SEPARADOR_FILE_LOG);
					}
				}
			} else {
				bos.write((PRESENTES_SELECIONADO.getBytes()));
				bos.write(SEPARADOR_FILE_LOG);
				for (Node node : listNodesRectangles) {
					Rectangle r = (Rectangle) node;
					if (r.getStroke().equals(Color.DARKGREEN)) {
						bos.write((r.getX() + VAZIO).getBytes());
						bos.write(SEPARADOR_FILE_LOG);
						bos.write((r.getY() + VAZIO).getBytes());
						bos.write(SEPARADOR_FILE_LOG);
						bos.write((r.getWidth() + VAZIO).getBytes());
						bos.write(SEPARADOR_FILE_LOG);
						bos.write((r.getHeight() + VAZIO).getBytes());
						bos.write(SEPARADOR_FILE_LOG);
					}

				}
			}

			bos.flush();

		} catch (IOException ex) {
			ex.printStackTrace();
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
					"Unifacisa - Image Mark\nProjeto: detec��o de assinaturas\n\nVers�o 1.0.0", ButtonType.CLOSE);
			dialog.setTitle("Sobre");

			((Stage) dialog.getDialogPane().getScene().getWindow()).getIcons()
					.add(new Image(getClass().getResource("/icon.png").toString()));
			dialog.showAndWait();
		});

		tutorial.setOnAction(actionEvent -> {
			final Alert dialog = new Alert(Alert.AlertType.NONE,
					"Atalhos:\n\n'S'" + " - Ap�s finalizar a marca��o, para salvar o arquivo de log.\n'<- e ->' Para "
							+ "navegar pelas imagens da pasta selecionada\n'Clique' Para selecionar os pontos"
							+ " da imagem\nObs:. Para refazer uma marca��o basta clicar sobre a imagem de uma marca��o"
							+ " j� conclu�da.\n\nComo usar:\nPasso 1 - Carregue uma imagem de um diret�rio, todas"
							+ " as demais desse diret�tio ser�o carregadas. (.JPG)\nPasso 2 - Clique no primeiro"
							+ " ponto da imagem, o primeiro ponto � o vertice do primeiro aluno da lista."
							+ "\nPasso 3 - Clique no segundo ponto da lista, correponde a altura da linha"
							+ " e � o vertice inferior esquerdo do primeiro aluno da lista\nPasso 4 - Clique "
							+ "no vertice inferior direito do ultimo aluno da lista\n\n Os retangulos de "
							+ "marca��o ser�o automaticamente gerados\n\nPasso 5 - Selecione o check "
							+ "correspondente aos 'Presentes' ou 'Faltantes'\nPasso 6 - Tecle 'S' para "
							+ "salvar a marca��o\n\n� direcionado para a pr�xima imagem, repita esse processo"
							+ " para as outras listas.",
					ButtonType.CLOSE);
			dialog.setTitle("Sobre");

			((Stage) dialog.getDialogPane().getScene().getWindow()).getIcons()
					.add(new Image(getClass().getResource("/icon.png").toString()));
			dialog.showAndWait();
		});

		buttonPresentes.setOnAction(actionEvent -> {
			buttonPresentes.setSelected(true);
			buttonFaltantes.setSelected(false);
			for (Node node : listNodesRectangles) {
				Rectangle r = (Rectangle) node;
				r.setStroke(Color.DARKBLUE);
				r.setFill(Color.STEELBLUE);
			}

		});

		buttonFaltantes.setOnAction(actionEvent -> {
			buttonPresentes.setSelected(false);
			buttonFaltantes.setSelected(true);
			for (Node node : listNodesRectangles) {
				Rectangle r = (Rectangle) node;
				r.setStroke(Color.DARKBLUE);
				r.setFill(Color.STEELBLUE);
			}
		});

		resetMarcacao.setOnAction(ActionEvent -> {
			reset();
		});

		rotate.setOnAction(ActionEvent -> {
			reset();
			statusMarcacao.setText(SELECIONE_A_ALTURA_DIREITA_MAXIMA_NA_TABELA);
			rotacionar = true;
		});

		outFocus.setOnAction(ActionEvent -> {
		});

		buttonFaltantes.setDisable(true);
		buttonPresentes.setDisable(true);
		resetMarcacao.setDisable(true);
	}

	private void reset() {
		ScrollPane sp = (ScrollPane) content.getChildren().get(0);
		StackPane st = (StackPane) sp.getContent();
		AnchorPane ap = (AnchorPane) st.getChildren().get(0);
		if (listNodesRectangles != null) {
			ap.getChildren().removeAll(listNodesRectangles);
		}
		coordinates = new ArrayList<String>();
		coordinatesRotacionar = new ArrayList<String>();
		statusMarcacao.setText(MARCACAO_REINICIADA_AGUARDANDO_MARCACAO_INICIAL);
		coord1.setText(ZERO_STRING);
		coord2.setText(ZERO_STRING);
		coord3.setText(ZERO_STRING);
		rotacionar = false;
	}

	/**
	 * Initializes the controls and status label on the status bar.
	 */
	private void initializeStatusBar() {
		status.textProperty().bind(gallery.statusProperty());
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
		itemCurrent = item;

		if (content.getChildren().size() > 0 && content.getChildren().get(0) instanceof ImageView) {

		} else if (content.getChildren().size() > 0 && content.getChildren().get(0) instanceof MediaControl) {
			final MediaControl previousMediaControl = (MediaControl) content.getChildren().get(0);
			previousMediaControl.getMediaPlayer().dispose();
		}

		if (item.isImage()) {
			renderImage(item.getItem());
		} else {
			System.out.println("O arquivo n�o � uma imagem");
		}
	}

	/**
	 * Renders a given gallery item as an image.
	 *
	 * @param item
	 */
	@SuppressWarnings("resource")
	public void renderImage(final File item) {

		String fileNameFull = itemCurrent.getItem().getName();
		String fileNameOutExtension = fileNameFull.substring(0, fileNameFull.lastIndexOf(PONTO));
		String imageURL = null;
		try {
			imageURL = item.toURI().toURL().toExternalForm();
		} catch (MalformedURLException e1) {
			e1.printStackTrace();
			return;
		}
		imageView = new ImageView(new Image(imageURL));
		imgZoom = new ImageView(new Image(imageURL));
		StackPane stackPane = new StackPane();
		AnchorPane anchor = new AnchorPane();
		nameImage.setText(item.getName());

		imageView.addEventFilter(MouseEvent.MOUSE_MOVED, new EventHandler<MouseEvent>() {

			@Override
			public void handle(MouseEvent mouseEvent) {
				drawZoom(mouseEvent);
			}

			private void drawZoom(MouseEvent mouseEvent) {
				coords.setText((int) mouseEvent.getX() + VIRGULA + (int) mouseEvent.getY() + PX);
				AnchorPane an = (AnchorPane) contentRight.getChildren().get(0);
				AnchorPane image = (AnchorPane) an.getChildren().get(24);
				ImageView img = imgZoom;
				Rectangle2D viewportRect3 = new Rectangle2D(mouseEvent.getX() - 20, mouseEvent.getY() - 20, 40, 40);
				img.setViewport(viewportRect3);
				img.setSmooth(true);
				img.setPreserveRatio(false);
				img.fitWidthProperty().bind(image.widthProperty());
				img.fitHeightProperty().bind(image.heightProperty());
				img.opacityProperty().set(0.4);
				if (image.getChildren().size() > 1) {
					image.getChildren().remove(1);
				}
				image.getChildren().add(img);
			}
		});

		if (new File(itemCurrent.getItem().getParentFile() + SEPARADOR + fileNameOutExtension + TXT_EXTENSION).exists()
				&& !refazer) {
			refazer = false;
			try {
				File log = new File(
						itemCurrent.getItem().getParentFile() + SEPARADOR + fileNameOutExtension + TXT_EXTENSION);
				Scanner leitor = new Scanner(log);
				String linhasDoArquivo = new String();
				linhasDoArquivo = leitor.nextLine();
				String[] valores = linhasDoArquivo.split(SEPARADOR_FILE);

				x0 = Double.parseDouble(valores[0]);
				y0 = Double.parseDouble(valores[1]);
				x1 = Double.parseDouble(valores[2]);
				y1 = Double.parseDouble(valores[3]);
				x2 = Double.parseDouble(valores[4]);
				y2 = Double.parseDouble(valores[5]);
				coord1.setText(valores[0] + VIRGULA + valores[1]);
				coord2.setText(valores[2] + VIRGULA + valores[3]);
				coord3.setText(valores[4] + VIRGULA + valores[5]);
				resetMarcacao.setDisable(false);

				coordinates = new ArrayList<String>();
				coordinatesRotacionar = new ArrayList<String>();
				coordinates.add(coord1.getText());
				coordinates.add(coord2.getText());
				coordinates.add(coord3.getText());
				statusMarcacao.setText(MARCACAO_FINALIZADA);

				int quantidadeRetangulos = (int) Math.round(((y2 - y0) / (y1 - y0)));
				System.out.println(
						"Quantidade de retangulos = " + quantidadeRetangulos + " - Real = " + ((y2 - y0) / (y1 - y0)));

				double pontoInicialX = x0;
				double pontoInicialY = y0;
				double largura = x2 - x1;
				double altura = y1 - y0;

				imageView.addEventFilter(MouseEvent.MOUSE_CLICKED, new EventHandler<MouseEvent>() {
					@Override
					public void handle(MouseEvent mouseEvent) {
						final Alert dialog = new Alert(Alert.AlertType.WARNING, "Deseja refazer essa marca��o?\n\n",
								ButtonType.NO, ButtonType.YES);
						dialog.setTitle("Image View - Alerta!");

						((Stage) dialog.getDialogPane().getScene().getWindow()).getIcons()
								.add(new Image(getClass().getResource("/icon.png").toString()));
						dialog.showAndWait();

						if (dialog.getResult().equals(ButtonType.YES)) {
							refazer = true;
							reset();
							renderImage(item);
						} else {
						}
					}
				});

				retangulos = calculaRetangulos(quantidadeRetangulos, largura, altura, pontoInicialY, pontoInicialX);

				Rectangle rectangle = null;
				listNodesRectangles = new ArrayList<Node>();
				for (RetanguloAssinatura retangulo : retangulos) {
					rectangle = new Rectangle(retangulo.getPontoInicialX(), retangulo.getPontoInicialY(),
							retangulo.getLargura(), retangulo.getAltura());

					rectangle.setStroke(Color.DARKBLUE);
					rectangle.setFill(Color.STEELBLUE);

					for (int i = 7; i < valores.length; i += 4) {
						if (rectangle.getX() == Double.parseDouble(valores[i])
								&& rectangle.getY() == Double.parseDouble(valores[i + 1])) {
							if (valores[6].equals(FALTANTES_SELECIONADO)) {
								rectangle.setStroke(Color.DARKRED);
								rectangle.setFill(Color.RED);

							} else {
								rectangle.setStroke(Color.DARKGREEN);
								rectangle.setFill(Color.GREENYELLOW);
							}
						}
					}
					rectangle.setStrokeWidth(5);
					rectangle.setStrokeType(StrokeType.INSIDE);
					rectangle.opacityProperty().set(0.4);
					rectangle.setCursor(Cursor.HAND);
					rectangle.addEventFilter(MouseEvent.MOUSE_CLICKED, new EventHandler<MouseEvent>() {
						@Override
						public void handle(MouseEvent mouseEvent) {
							Rectangle rectangleSelect = (Rectangle) mouseEvent.getSource();
							if (rectangleSelect.getStroke().equals(Color.DARKBLUE)) {
								if (buttonFaltantes.isSelected()) {
									rectangleSelect.setStroke(Color.DARKRED);
									rectangleSelect.setFill(Color.RED);
								} else {
									rectangleSelect.setFill(Color.GREENYELLOW);
									rectangleSelect.setStroke(Color.DARKGREEN);
								}
							} else {
								rectangleSelect.setStroke(Color.DARKBLUE);
								rectangleSelect.setFill(Color.STEELBLUE);
							}
							System.out.println("Clique no retangulo " + rectangleSelect);
						}
					});
					listNodesRectangles.add(rectangle);
				}

				if (valores[6].equals(FALTANTES_SELECIONADO)) {
					buttonFaltantes.setDisable(false);
					buttonFaltantes.setSelected(true);
					buttonPresentes.setDisable(false);
					buttonPresentes.setSelected(false);

				} else {
					buttonFaltantes.setDisable(false);
					buttonFaltantes.setSelected(false);
					buttonPresentes.setDisable(false);
					buttonPresentes.setSelected(true);
				}

				buttonFaltantes.setSelected(true);
				buttonPresentes.setSelected(false);
				anchor.getChildren().add(imageView);
				anchor.getChildren().addAll(listNodesRectangles);
				stackPane.getChildren().add(anchor);
				scrollPane = new ScrollPane(stackPane);
				scrollPane.setHvalue(1);
				scrollPane.setVvalue(0.23);
				content.getChildren().clear();
				content.getChildren().add(scrollPane);

			} catch (Exception e) {
			}
		} else {
			rotate.setDisable(false);
			refazer = false;
			statusMarcacao.setText(AGUARDANDO_MARCACAO_INICIAL);
			coord1.setText(ZERO_STRING);
			coord2.setText(ZERO_STRING);
			coord3.setText(ZERO_STRING);
			nameImage.setText(item.getName());
			resetMarcacao.setDisable(true);
			buttonPresentes.setDisable(true);
			buttonFaltantes.setDisable(true);
			coordinates = new ArrayList<String>();
			coordinatesRotacionar = new ArrayList<String>();

			imageView.addEventFilter(MouseEvent.MOUSE_CLICKED, new EventHandler<MouseEvent>() {

				private List<RetanguloAssinatura> retangulos;

				@Override
				public void handle(MouseEvent mouseEvent) {
					if (rotacionar) {
						resetMarcacao.setDisable(false);
						if (coordinatesRotacionar.size() == 0) {
							coordinatesRotacionar.add((int) mouseEvent.getX() + VIRGULA + (int) mouseEvent.getY() + PX);
							statusMarcacao.setText(SELECIONE_A_ALTURA_DIREITA_MINIMA_NA_TABELA);
							x0 = mouseEvent.getX();
						} else {
							coordinatesRotacionar.add((int) mouseEvent.getX() + VIRGULA + (int) mouseEvent.getY() + PX);
							statusMarcacao.setText(IMAGEM_ROTACIONADA_AGUARDANDO_MARCACAO);
							x1 = mouseEvent.getX();

							try {
								BufferedImage image = SwingFXUtils.fromFXImage(imageView.getImage(), null);
								BufferedImage imageRGB = new BufferedImage(image.getWidth(), image.getHeight(),
										BufferedImage.OPAQUE);
								Graphics2D graphics = imageRGB.createGraphics();
								graphics.rotate(Math.toRadians(calculaRotacao(x0 - x1)));
								graphics.drawImage(image, 0, 0, null);
								ImageIO.write(imageRGB, "jpg", new File(itemCurrent.getItem().getParentFile()
										+ SEPARADOR + fileNameOutExtension + "rotate.jpg"));
								File imagemRotacionada = new File(itemCurrent.getItem().getParentFile() + SEPARADOR
										+ fileNameOutExtension + "rotate.jpg");
								graphics.dispose();
								loadFile(imagemRotacionada);
							} catch (IOException e1) {
								e1.printStackTrace();
							} finally {
								rotacionar = false;
							}

						}
					} else {

						if (coordinates.size() == 0) {
							coordinates.add((int) mouseEvent.getX() + VIRGULA + (int) mouseEvent.getY() + PX);
							coord1.setText(mouseEvent.getX() + VIRGULA + mouseEvent.getY());
							statusMarcacao.setText(AGUARDANDO_MARCACAO_DE_ALTURA_DE_LINHA);
							resetMarcacao.setDisable(false);
							buttonPresentes.setDisable(false);
							buttonFaltantes.setDisable(false);
							buttonFaltantes.setSelected(true);
							x0 = mouseEvent.getX();
							y0 = mouseEvent.getY();
						} else if (coordinates.size() == 1) {
							coordinates.add((int) mouseEvent.getX() + VIRGULA + (int) mouseEvent.getY() + PX);
							coord2.setText(mouseEvent.getX() + VIRGULA + mouseEvent.getY());
							statusMarcacao.setText(AGUARDANDO_MARCACAO_FINAL);
							x1 = mouseEvent.getX();
							y1 = mouseEvent.getY();
						} else if (coordinates.size() == 2) {
							coordinates.add((int) mouseEvent.getX() + VIRGULA + (int) mouseEvent.getY() + PX);
							coord3.setText(mouseEvent.getX() + VIRGULA + mouseEvent.getY());
							statusMarcacao.setText(MARCACAO_FINALIZADA);
							x2 = mouseEvent.getX();
							y2 = mouseEvent.getY();

							anchor.getChildren().clear();
							anchor.setLayoutX(imageView.getFitHeight());
							anchor.setLayoutY(imageView.getFitWidth());
							anchor.opacityProperty().set(1);

							stackPane.getChildren().clear();
							stackPane.setLayoutX(imageView.getFitHeight());
							stackPane.setLayoutY(imageView.getFitWidth());
							stackPane.opacityProperty().set(1);
							content.setFocusTraversable(true);

							int quantidadeRetangulos = (int) Math
									.round((((y2 - 2) - (y0 - 2)) / ((y1 - 2) - (y0 - 2))));
							System.out.println("Quantidade de retangulos = " + quantidadeRetangulos + " - Real = "
									+ ((y2 - y0) / (y1 - y0)));

							double pontoInicialX = x0;
							double pontoInicialY = y0;
							double largura = x2 - x1;
							double altura = y1 - y0;
							retangulos = calculaRetangulos(quantidadeRetangulos, largura, altura, pontoInicialY,
									pontoInicialX);

							Rectangle rectangle = null;
							listNodesRectangles = new ArrayList<Node>();
							for (RetanguloAssinatura retangulo : retangulos) {
								rectangle = new Rectangle(retangulo.getPontoInicialX(), retangulo.getPontoInicialY(),
										retangulo.getLargura(), retangulo.getAltura());
								rectangle.setStroke(Color.DARKBLUE);
								rectangle.setStrokeWidth(5);
								rectangle.setStrokeType(StrokeType.INSIDE);
								rectangle.setFill(Color.STEELBLUE);
								rectangle.opacityProperty().set(0.4);
								rectangle.setCursor(Cursor.HAND);
								rectangle.addEventFilter(MouseEvent.MOUSE_CLICKED, new EventHandler<MouseEvent>() {
									@Override
									public void handle(MouseEvent mouseEvent) {
										Rectangle rectangleSelect = (Rectangle) mouseEvent.getSource();
										if (rectangleSelect.getStroke().equals(Color.DARKBLUE)) {
											if (buttonFaltantes.isSelected()) {
												rectangleSelect.setStroke(Color.DARKRED);
												rectangleSelect.setFill(Color.RED);
											} else {
												rectangleSelect.setFill(Color.GREENYELLOW);
												rectangleSelect.setStroke(Color.DARKGREEN);
											}
										} else {
											rectangleSelect.setStroke(Color.DARKBLUE);
											rectangleSelect.setFill(Color.STEELBLUE);
										}
										System.out.println("Clique no retangulo " + rectangleSelect);
									}
								});
								listNodesRectangles.add(rectangle);
							}

							buttonFaltantes.setSelected(true);
							buttonPresentes.setSelected(false);
							anchor.getChildren().add(imageView);
							anchor.getChildren().addAll(listNodesRectangles);
							stackPane.getChildren().add(anchor);
							ScrollPane sp = (ScrollPane) content.getChildren().get(0);
							content.getChildren().clear();
							sp.setContent(stackPane);
							content.getChildren().add(sp);
						} else {
							System.out.println(MARCACAO_PARA_ESSA_IMAGEM_FINALIZADA);
						}

					}

				}

				private double calculaRotacao(double distancia) {
					if (distancia != 0) {
						return distancia * CONSTANTE_DE_CRESCIMENTO_DE_ROTACAO;
					}
					return 0;

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
			scrollPane.setHvalue(1);
			scrollPane.setVvalue(0.20);
			content.getChildren().add(scrollPane);
		}
	}

	public List<RetanguloAssinatura> calculaRetangulos(int quantidadeRetangulos, double largura, double altura,
			double pontoInicialY, double pontoInicialX) {
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

	void markEvent(KeyEvent event) {
		if (!gallery.isEmpty()) {
			if (event.getCode().equals(KeyCode.S)) {
				System.out.println("Apertou S");
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