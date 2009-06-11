package eu.webtoolkit.jwt.examples.painting;

import eu.webtoolkit.jwt.WApplication;
import eu.webtoolkit.jwt.WEnvironment;
import eu.webtoolkit.jwt.WString;
import eu.webtoolkit.jwt.WtServlet;

public class PaintMain extends WtServlet {
	public PaintMain() {
		super();
	}

	@Override
	public WApplication createApplication(WEnvironment env) {
		WApplication app = new WApplication(env);
		app.setTitle(new WString("Paint example"));
		
		app.useStyleSheet("style/painting.css");
		
		new PaintExample(app.getRoot());

		return app;
	}
}