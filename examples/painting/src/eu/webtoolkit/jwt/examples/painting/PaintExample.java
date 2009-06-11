package eu.webtoolkit.jwt.examples.painting;

import java.util.EnumSet;

import eu.webtoolkit.jwt.AlignmentFlag;
import eu.webtoolkit.jwt.Orientation;
import eu.webtoolkit.jwt.Side;
import eu.webtoolkit.jwt.Signal1;
import eu.webtoolkit.jwt.WContainerWidget;
import eu.webtoolkit.jwt.WGridLayout;
import eu.webtoolkit.jwt.WLength;
import eu.webtoolkit.jwt.WPaintedWidget;
import eu.webtoolkit.jwt.WSlider;
import eu.webtoolkit.jwt.WString;
import eu.webtoolkit.jwt.WText;

public class PaintExample extends WContainerWidget {
	public PaintExample(WContainerWidget root) {
		super(root);
		new WText(
				new WString(
						"<h2>Paint example</h2><p>A simple example demonstrating cross-browser vector graphics.</p><p>The emweb logo below is painted using the Wt WPainter API, and rendered to the browser using inline SVG, inline VML or the HTML 5 &lt;canvas&gt; element.</p>"),
				this);
		WContainerWidget emweb = new WContainerWidget(this);
		emweb.setMargin(new WLength(), EnumSet.of(Side.Left, Side.Right));
		WGridLayout layout = new WGridLayout();
		emweb.setLayout(layout, EnumSet.of(AlignmentFlag.AlignCenter, AlignmentFlag.AlignTop));
		WSlider scaleSlider = new WSlider(Orientation.Horizontal);
		scaleSlider.setMinimum(0);
		scaleSlider.setMaximum(20);
		scaleSlider.setValue(10);
		scaleSlider.setTickInterval(5);
		scaleSlider.setTickPosition(WSlider.TicksBothSides);
		scaleSlider.resize(new WLength(300), new WLength(50));
		scaleSlider.valueChanged().addListener(this, new Signal1.Listener<Integer>() {
			public void trigger(Integer e1) {
				PaintExample.this.scaleShape(e1);
			}
		});
		layout.addWidget(scaleSlider, 0, 1, EnumSet.of(AlignmentFlag.AlignCenter, AlignmentFlag.AlignMiddle));
		WSlider rotateSlider = new WSlider(Orientation.Vertical);
		rotateSlider.setMinimum(-30);
		rotateSlider.setMaximum(30);
		rotateSlider.setValue(0);
		rotateSlider.setTickInterval(10);
		rotateSlider.setTickPosition(WSlider.TicksBothSides);
		rotateSlider.resize(new WLength(50), new WLength(400));
		rotateSlider.valueChanged().addListener(this, new Signal1.Listener<Integer>() {
			public void trigger(Integer e1) {
				PaintExample.this.rotateShape(e1);
			}
		});
		layout.addWidget(rotateSlider, 1, 0, EnumSet.of(AlignmentFlag.AlignCenter, AlignmentFlag.AlignMiddle));
		this.shapes_ = new ShapesWidget();
		this.shapes_.setAngle(0.0);
		this.shapes_.setRelativeSize(0.5);
		this.shapes_.setPreferredMethod(WPaintedWidget.Method.InlineSvgVml);
		layout.addWidget(this.shapes_, 1, 1, EnumSet.of(AlignmentFlag.AlignCenter, AlignmentFlag.AlignMiddle));
	}

	private ShapesWidget shapes_;

	private void rotateShape(int v) {
		this.shapes_.setAngle(v / 2.0);
		this.shapes_.setPreferredMethod(v < 0 ? WPaintedWidget.Method.InlineSvgVml : WPaintedWidget.Method.HtmlCanvas);
	}

	private void scaleShape(int v) {
		this.shapes_.setRelativeSize(0.1 + 0.9 * (v / 20.0));
	}
}