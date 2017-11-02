package etapas;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JScrollPane;

import boofcv.abst.feature.detect.line.DetectLineHoughFoot;
import boofcv.abst.feature.detect.line.DetectLineHoughFootSubimage;
import boofcv.alg.filter.blur.GBlurImageOps;
import boofcv.factory.feature.detect.line.ConfigHoughFoot;
import boofcv.factory.feature.detect.line.ConfigHoughFootSubimage;
import boofcv.factory.feature.detect.line.FactoryDetectLineAlgs;
import boofcv.gui.ListDisplayPanel;
import boofcv.gui.binary.VisualizeBinaryData;
import boofcv.gui.feature.ImageLinePanel;
import boofcv.gui.image.ImagePanel;
import boofcv.gui.image.ShowImages;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;
import filter.DetectarPresencaEmerson;
import filter.DetectorDeAssinaturas;
import georegression.metric.Intersection2D_F32;
import georegression.metric.UtilAngle;
import georegression.struct.line.LineParametric2D_F32;
import georegression.struct.point.Point2D_F32;
import georegression.struct.shapes.RectangleLength2D_F32;
import util.PointComparator;
import util.RectangleUtils;

/**
 * @author emersoncantalice
 *
 */
public class Main {

	private static boolean presentes;
	private static String imgURL;
	private static final String SEPARADOR_FILE_LOG = "#@";
	private static final String FALTANTES_SELECIONADO = "F";
	private static final String PRESENTES_SELECIONADO = "P";
	private static final String SEPARADOR = System.getProperty("file.separator");
	private static final String TXT_EXTENSION = ".txt";
	private static final String PONTO = ".";

	public static ImagePanel showImage(BufferedImage img, String title, boolean closeOnExit) {
		JFrame frame = new JFrame();
		ImagePanel panel = new ImagePanel(img);
		JScrollPane jScrollPane = new JScrollPane(panel);
		jScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		jScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		frame.getContentPane().add(jScrollPane);
		frame.add(jScrollPane, BorderLayout.CENTER);
		frame.pack();
		frame.setLocationByPlatform(true);
		frame.setVisible(true);
		if (closeOnExit) {
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		}
		return panel;
	}

	public static void main(String args[]) {

		presentes = false;

		// 1 - Pega quantidade de linhas horizontais

		// 2 - Carrega paramentos de maxLines, edgeT e Tipo do algoritmo conforme
		// a quantidade de linhas horizontais retornadas.

		// Imagens disponiveis no pack 1 no drive, copialas para a pasta data desse
		// projeto
		// Carrega a imagem da lista
		File f = new File("data/00001E.jpg");
		imgURL = f.getName();

		BufferedImage image1 = UtilImageIO.loadImage("data/00001E.jpg");

		// Redimenciona a imagem, assim todas as imagens serão anallizadas nas mesmas
		// condicoes de resolução
		BufferedImage image2 = resize(image1, 600, 800);

		// Recorta a imagem para ficar apenas a parte de interesse, a tabela de nomes
		int inicioYRetangulo = (int) (image2.getHeight() * 0.1690);
		int fimYRetangulo = (int) (image2.getHeight() * 0.7255);

		BufferedImage image = image2.getSubimage(0, inicioYRetangulo, 600, fimYRetangulo);

		// showImage(image, "Imagens", false);

		// Sensibilidade de detecção de linhas
		float edgeThreshold;
		// Número máximo de uma lista totalmente preenchida
		int maxLines;

		// Verifica a quantidade de linhas horizontais na imagem
		int quantLinhasVerticais = quantidadeLinhasVerticais(image);
		System.out.println(quantLinhasVerticais);

		// Conforme a quantidade de linhas são carregadas configurações diferentes para
		// sensibilidade na detecao de linhas e quantidade maxima de linhas a serem
		// encontradas
		if (quantLinhasVerticais < 15) {
			edgeThreshold = 18.8F;
			maxLines = 18;
		} else if (quantLinhasVerticais >= 20) {
			edgeThreshold = 25;
			maxLines = 50;
		} else {
			edgeThreshold = 20;
			maxLines = 50;
		}

		// Converte a imagem para uma de 8bits, é pre-requeisto para que a binarização
		// ocorra
		GrayU8 gray = ConvertBufferedImage.convertFrom(image, (GrayU8) null);

		// Cria uma nova imagem agora binarizada, isto é, formada apenas por pixels
		// brancos e pretos. Isso facilita o reconhecimento de linhas
		BufferedImage renderedBinary = VisualizeBinaryData.renderBinary(gray, false, null);

		// Carrega a imagem já binarizada em 32 bits, necessario pois eh o formato que o
		// algoritmo de hough requer
		GrayF32 input = ConvertBufferedImage.convertFromSingle(renderedBinary, null, GrayF32.class);

		// Cria uma imagem de saida
		GrayF32 out = new GrayF32(image.getWidth(), image.getHeight());

		// Aplica um filtro de blur para facilitar o reconhecimento das linhas, na
		// documentação da biblioteca ela indica esse passo
		GBlurImageOps.gaussian(input, out, -1, 2, null);

		List<LineParametric2D_F32> lines;

		// Conforme a quantidade de linhas são carregadas algoritmos diferentes de hough
		// são carregados, o de houghFootSub se comporta melhor quando a linha é pequena
		// dentro da imagem, para valores intermediaros houghFoot apresentou melhor
		// performace
		if (quantLinhasVerticais < 15) {
			DetectLineHoughFootSubimage<GrayF32, GrayF32> alg = FactoryDetectLineAlgs.houghFootSub(
					new ConfigHoughFootSubimage(3, 8, 5, edgeThreshold, maxLines, 2, 2), GrayF32.class, GrayF32.class);

			lines = alg.detect(out);
			List<LineParametric2D_F32> vlines = new LinkedList<LineParametric2D_F32>();

			for (LineParametric2D_F32 pline : lines) {
				long angle = Math.abs(Math.round(UtilAngle.radianToDegree(pline.getAngle())));
				if (angle == 0 || angle == 180) {
					vlines.add(pline);
				}
			}

		} else if (quantLinhasVerticais >= 20) {

			DetectLineHoughFootSubimage<GrayF32, GrayF32> alg = FactoryDetectLineAlgs.houghFootSub(
					new ConfigHoughFootSubimage(3, 14, 5, edgeThreshold, maxLines, 2, 2), GrayF32.class, GrayF32.class);

			lines = alg.detect(out);
			List<LineParametric2D_F32> vlines = new LinkedList<LineParametric2D_F32>();

			for (LineParametric2D_F32 pline : lines) {
				long angle = Math.abs(Math.round(UtilAngle.radianToDegree(pline.getAngle())));
				if (angle == 0 || angle == 180) {
					vlines.add(pline);
				}
			}

		} else {

			DetectLineHoughFoot<GrayF32, GrayF32> alg = FactoryDetectLineAlgs
					.houghFoot(new ConfigHoughFoot(6, 12, 5, edgeThreshold, maxLines), GrayF32.class, GrayF32.class);

			lines = alg.detect(out);
			List<LineParametric2D_F32> vlines = new LinkedList<LineParametric2D_F32>();

			for (LineParametric2D_F32 pline : lines) {
				long angle = Math.abs(Math.round(UtilAngle.radianToDegree(pline.getAngle())));
				if (angle == 0 || angle == 180) {
					vlines.add(pline);
				}
			}

		}

		// <ALGORITMOS DISPONIVEIS>
		// Alg 1
		// DetectLineHoughFoot<GrayF32, GrayF32> alg = FactoryDetectLineAlgs
		// .houghFoot(new ConfigHoughFoot(6, 12, 5, edgeThreshold, maxLines),
		// GrayF32.class, GrayF32.class);

		// Alg 2
		// DetectLineHoughPolar<GrayF32, GrayF32> alg =
		// FactoryDetectLineAlgs.houghPolar(
		// new ConfigHoughPolar(3, 30, 2, Math.PI / 180, edgeThreshold, maxLines),
		// GrayF32.class, GrayF32.class);

		// Alg 3
		// DetectLineHoughFootSubimage<GrayF32, GrayF32> alg =
		// FactoryDetectLineAlgs.houghFootSub(
		// new ConfigHoughFootSubimage(3, 8, 5, edgeThreshold, maxLines, 2, 2),
		// GrayF32.class, GrayF32.class);

		// Descomentar a linha abaixo para ver as intercessoes
		// intercessoes(image, lines);

		// Descomentar a linha abaixo para ver as retas identifiadas
		retas(image, lines);

		// separaAssinaturas(image, lines);

		detectaAssinaturas(image, lines);

	}

	/**
	 * @param img
	 *            imagem a ser redimencionada
	 * @param newW
	 *            novo width da imagem
	 * @param newH
	 *            novo higth da imagem
	 * @return a imagem redimencionada
	 */
	public static BufferedImage resize(BufferedImage img, int newW, int newH) {
		Image tmp = img.getScaledInstance(newW, newH, Image.SCALE_SMOOTH);
		BufferedImage dimg = new BufferedImage(newW, newH, BufferedImage.TYPE_INT_ARGB);

		Graphics2D g2d = dimg.createGraphics();
		g2d.drawImage(tmp, 0, 0, null);
		g2d.dispose();

		return dimg;
	}

	/**
	 * @param image
	 *            imagem que a quantidade de linhas horizontais é encontrada
	 * @return a quantidade de linhas horizontais na imagem
	 */
	public static int quantidadeLinhasVerticais(BufferedImage image) {

		float edgeThreshold = 25;
		int maxLines = 43;

		GrayU8 gray = ConvertBufferedImage.convertFrom(image, (GrayU8) null);

		BufferedImage renderedBinary = VisualizeBinaryData.renderBinary(gray, false, null);

		GrayF32 input = ConvertBufferedImage.convertFromSingle(renderedBinary, null, GrayF32.class);
		GrayF32 out = new GrayF32(image.getWidth(), image.getHeight());
		GBlurImageOps.gaussian(input, out, -1, 2, null);

		DetectLineHoughFoot<GrayF32, GrayF32> alg = FactoryDetectLineAlgs
				.houghFoot(new ConfigHoughFoot(6, 12, 5, edgeThreshold, maxLines), GrayF32.class, GrayF32.class);

		List<LineParametric2D_F32> lines = alg.detect(out);
		List<LineParametric2D_F32> vlines = new LinkedList<LineParametric2D_F32>();

		for (LineParametric2D_F32 pline : lines) {
			long angle = Math.abs(Math.round(UtilAngle.radianToDegree(pline.getAngle())));
			if (angle == 0 || angle == 180) {
				vlines.add(pline);
			}
		}

		return vlines.size();

	}

	private static void detectaAssinaturas(BufferedImage image, List<LineParametric2D_F32> lines) {
		List<LineParametric2D_F32> hlines = new LinkedList<LineParametric2D_F32>();
		List<LineParametric2D_F32> vlines = new LinkedList<LineParametric2D_F32>();

		for (LineParametric2D_F32 pline : lines) {
			long angulo = Math.abs(Math.round(UtilAngle.radianToDegree(pline.getAngle())));
			if (angulo == 0 || angulo == 180) {
				hlines.add(pline);
			} else if (angulo > 80 && angulo <= 100) {
				vlines.add(pline);
			}
		}

		Graphics2D g2 = (Graphics2D) image.getGraphics();
		g2.setColor(Color.RED);

		List<Point2D_F32> pontosIntercessao = new ArrayList<Point2D_F32>();
		for (LineParametric2D_F32 hline : hlines) {
			for (LineParametric2D_F32 vline : vlines) {
				Point2D_F32 intersection = Intersection2D_F32.intersection(hline, vline, null);
				if (intersection.x > 0) {
					pontosIntercessao.add(intersection);
					g2.fillRect((int) intersection.x, (int) intersection.y, 4, 4);
				}
			}
		}

		Collections.sort(pontosIntercessao, new PointComparator(2));

		List<RectangleLength2D_F32> cells = RectangleUtils.find(pontosIntercessao, vlines.size(), 3);

		int inicio = 7, intervalo = 4;
		List<RectangleLength2D_F32> assinaturasRetangulo = new LinkedList<RectangleLength2D_F32>();
		for (int i = inicio; i < cells.size(); i = i + intervalo) {
			assinaturasRetangulo.add(cells.get(i));
		}

		List<BufferedImage> assinaturas = RectangleUtils.splitImages(image, assinaturasRetangulo);
		System.out.println("Total de Imagens: " + assinaturas.size());

		ListDisplayPanel panel = new ListDisplayPanel();
		int count = 0;

		StringBuffer conteudoArquivoLog = new StringBuffer();

		cabecalhoArquivo(assinaturas, conteudoArquivoLog);

		if (presentes) {
			conteudoArquivoLog.append(SEPARADOR_FILE_LOG).append(PRESENTES_SELECIONADO).append(SEPARADOR_FILE_LOG);
		} else {
			conteudoArquivoLog.append(SEPARADOR_FILE_LOG).append(FALTANTES_SELECIONADO).append(SEPARADOR_FILE_LOG);
		}

		for (BufferedImage imagemAssinatura : assinaturas) {
			DetectorDeAssinaturas dp = new DetectarPresencaEmerson();
			boolean preenchido = dp.detectaAssinatura(imagemAssinatura);

			if (presentes) {
				if (preenchido) {
					selecionadosArquivo(conteudoArquivoLog, imagemAssinatura);
				}
			} else {
				if (!preenchido) {
					selecionadosArquivo(conteudoArquivoLog, imagemAssinatura);
				}
			}

			String presenteOuNao = preenchido ? "Presente" : "Faltou";
			System.out.println("Imagem " + count + " : " + presenteOuNao);
			panel.addImage(imagemAssinatura, count + " : " + presenteOuNao);
			count++;
		}

		saveLog(image, conteudoArquivoLog);
		ShowImages.showWindow(panel, "Imagens");
	}

	private static void selecionadosArquivo(StringBuffer conteudoArquivoLog, BufferedImage imagemAssinatura) {
		conteudoArquivoLog.append(Math.abs(imagemAssinatura.getRaster().getSampleModelTranslateX()))
				.append(SEPARADOR_FILE_LOG)
				.append(Math.abs(imagemAssinatura.getRaster().getSampleModelTranslateY()))
				.append(SEPARADOR_FILE_LOG)
				.append(Math.abs(imagemAssinatura.getRaster().getWidth()))
				.append(SEPARADOR_FILE_LOG)
				.append(Math.abs(imagemAssinatura.getRaster().getHeight()))
				.append(SEPARADOR_FILE_LOG);
	}

	private static void cabecalhoArquivo(List<BufferedImage> assinaturas, StringBuffer conteudoArquivoLog) {
		conteudoArquivoLog.append(Math.abs(assinaturas.get(0).getRaster().getSampleModelTranslateX()))
				.append(SEPARADOR_FILE_LOG)
				.append(Math.abs(assinaturas.get(0).getRaster().getSampleModelTranslateY()))
				.append(SEPARADOR_FILE_LOG)
				.append(Math.abs(assinaturas.get(0).getRaster().getSampleModelTranslateX()))
				.append(SEPARADOR_FILE_LOG)
				.append(Math.abs(assinaturas.get(0).getRaster().getSampleModelTranslateY() + assinaturas.get(0).getRaster().getHeight()))
				.append(SEPARADOR_FILE_LOG)
				.append(Math.abs(assinaturas.get(assinaturas.size() - 1).getRaster().getSampleModelTranslateX() + assinaturas.get(0).getRaster().getWidth()))
				.append(SEPARADOR_FILE_LOG)
				.append(Math.abs(assinaturas.get(assinaturas.size() - 1).getRaster().getSampleModelTranslateY()	+ assinaturas.get(0).getRaster().getHeight()));
	}

	private static void saveLog(BufferedImage image, StringBuffer conteudoArquivoLog) {
		try {

			String fileNameFull = imgURL;
			String fileNameOutExtension = fileNameFull.substring(0, fileNameFull.lastIndexOf(PONTO));

			File arquivo = new File("data" + SEPARADOR + fileNameOutExtension + "VIA-CV" + TXT_EXTENSION);

			FileOutputStream fo = new FileOutputStream(arquivo);

			@SuppressWarnings("resource")
			BufferedOutputStream bos = new BufferedOutputStream(fo);

			bos.write(conteudoArquivoLog.toString().getBytes());

			bos.flush();

		} catch (IOException ex) {
			ex.printStackTrace();
		}

	}

	/**
	 * @param image
	 *            que as intercessoes seram encontradas
	 * @param lines
	 *            retas identificadas na imagem
	 */
	@SuppressWarnings("unused")
	private static void intercessoes(BufferedImage image, List<LineParametric2D_F32> lines) {
		// Linhas horizontais e verticais
		List<LineParametric2D_F32> hlines = new LinkedList<LineParametric2D_F32>();
		List<LineParametric2D_F32> vlines = new LinkedList<LineParametric2D_F32>();

		// Carrega a lista de imagens
		for (LineParametric2D_F32 pline : lines) {
			long angle = Math.abs(Math.round(UtilAngle.radianToDegree(pline.getAngle())));
			if (angle == 0 || angle == 180) {
				hlines.add(pline);
			} else if (angle > 80 && angle <= 100) {
				vlines.add(pline);
			}
		}

		Graphics2D g2 = (Graphics2D) image.getGraphics();
		g2.setColor(Color.RED);

		// Encontra as intercessoes entre linhas
		List<Point2D_F32> intersectionPoints = new ArrayList<Point2D_F32>();
		for (LineParametric2D_F32 hline : hlines) {
			for (LineParametric2D_F32 vline : vlines) {
				Point2D_F32 intersection = Intersection2D_F32.intersection(hline, vline, null);
				if (intersection.x > 0) {
					intersectionPoints.add(intersection);
					g2.fillRect((int) intersection.x, (int) intersection.y, 4, 4);
				}
			}
		}

		// Ordena as intercessoes pelo posisão ponto encontrado na imagem
		Collections.sort(intersectionPoints, new PointComparator(2));

		// Mostrar contador na imagem
		g2.setColor(Color.BLUE);
		int cout = 1;
		for (Point2D_F32 point : intersectionPoints) {
			g2.drawString("" + cout++, (int) point.x, (int) point.y - 3);
			System.out.println(point.x + "," + point.y);
		}

		// descomentar para ver a imagem binarizada
		// ShowImages.showWindow(renderedBinary, "Detected Edges");

		ShowImages.showWindow(image, "Detected Lines");
	}

	private static void retas(BufferedImage image, List<LineParametric2D_F32> lines) {

		Graphics2D g2 = (Graphics2D) image.getGraphics();
		g2.setColor(Color.RED);

		ImageLinePanel gui = new ImageLinePanel();
		gui.setBackground(image);
		gui.setLines(lines);
		gui.setPreferredSize(new Dimension(image.getWidth(), image.getHeight()));

		// descomentar para ver a imagem binarizada
		// ShowImages.showWindow(renderedBinary, "Detected Edges");
		ShowImages.showWindow(gui, "Detected Lines");
	}
}