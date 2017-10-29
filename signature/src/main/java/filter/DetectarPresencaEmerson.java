package filter;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.List;

import boofcv.abst.feature.detect.line.DetectLineSegmentsGridRansac;
import boofcv.factory.feature.detect.line.FactoryDetectLineAlgs;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.struct.image.GrayF32;
import georegression.struct.line.LineSegment2D_F32;

public class DetectarPresencaEmerson implements DetectorDeAssinaturas {

	public boolean detectaAssinatura(BufferedImage image) {
		GrayF32 input = ConvertBufferedImage.convertFromSingle(image, null, GrayF32.class);

		new SimpleBorderFilter(3, 180, 10).apply(input, input);

		DetectLineSegmentsGridRansac<GrayF32, GrayF32> detector = FactoryDetectLineAlgs.lineRansac(5, 50, 90, true,
				GrayF32.class, GrayF32.class);

		List<LineSegment2D_F32> lines = detector.detect(input);
		Graphics2D g2 = (Graphics2D) image.getGraphics();
		g2.setStroke(new BasicStroke(3));
		int scale = 1;
		long length = 0;
		for (LineSegment2D_F32 s : lines) {
			length += s.getLength();
			g2.setColor(Color.RED);
			g2.drawLine((int) (scale * s.a.x), (int) (scale * s.a.y), (int) (scale * s.b.x), (int) (scale * s.b.y));
			g2.setColor(Color.BLUE);
			g2.fillOval((int) (scale * s.a.x) - 1, (int) (scale * s.a.y) - 1, 3, 3);
			g2.fillOval((int) (scale * s.b.x) - 1, (int) (scale * s.b.y) - 1, 3, 3);
		}

		return length < 8 ? false : true;
	}

}
