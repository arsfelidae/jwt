package eu.webtoolkit.jwt;

import java.util.*;
import java.util.regex.*;
import java.io.*;
import java.util.concurrent.locks.ReentrantLock;
import javax.servlet.http.*;
import eu.webtoolkit.jwt.*;
import eu.webtoolkit.jwt.chart.*;
import eu.webtoolkit.jwt.utils.*;
import eu.webtoolkit.jwt.servlet.*;

/**
 * Class that defines the style for pen strokes.
 * 
 * 
 * A pen defines the properties of how lines (that may surround shapes) are
 * rendered.
 * <p>
 * A pen with width 0 is a <i>cosmetic</i> pen, and is always rendered as 1
 * pixel width, regardless of transformations. Otherwized, the pen width is
 * modified by the {@link WPainter#getWorldTransform() transformation} set on
 * the painter.
 * <p>
 * 
 * @see WPainter#setPen(WPen p)
 * @see WBrush
 */
public class WPen {
	/**
	 * Default constructor.
	 * 
	 * Constructs a black solid pen of 0 width, with
	 * {@link PenCapStyle#SquareCap SquareCap} line ends and
	 * {@link PenJoinStyle#BevelJoin BevelJoin} line join style.
	 */
	public WPen() {
		this.penStyle_ = PenStyle.SolidLine;
		this.penCapStyle_ = PenCapStyle.SquareCap;
		this.penJoinStyle_ = PenJoinStyle.BevelJoin;
		this.width_ = new WLength(0);
		this.color_ = WColor.black;
	}

	/**
	 * Construct a black pen with a particular style.
	 * 
	 * Constructs a black pen of 0 width, with {@link PenCapStyle#SquareCap
	 * SquareCap} line ends and {@link PenJoinStyle#BevelJoin BevelJoin} line
	 * join style.
	 * <p>
	 * The line style is set to <i>style</i>.
	 */
	public WPen(PenStyle style) {
		this.penStyle_ = style;
		this.penCapStyle_ = PenCapStyle.SquareCap;
		this.penJoinStyle_ = PenJoinStyle.BevelJoin;
		this.width_ = new WLength(0);
		this.color_ = WColor.black;
	}

	/**
	 * Construct a solid pen of a particular color.
	 * 
	 * Constructs a solid pen of 0 width, with {@link PenCapStyle#SquareCap
	 * SquareCap} line ends and {@link PenJoinStyle#BevelJoin BevelJoin} line
	 * join style.
	 * <p>
	 * The pen color is set to <i>color</i>.
	 */
	public WPen(WColor color) {
		this.penStyle_ = PenStyle.SolidLine;
		this.penCapStyle_ = PenCapStyle.SquareCap;
		this.penJoinStyle_ = PenJoinStyle.BevelJoin;
		this.width_ = new WLength(0);
		this.color_ = color;
	}

	public WPen clone() {
		WPen result = new WPen();
		result.penStyle_ = this.penStyle_;
		result.penCapStyle_ = this.penCapStyle_;
		result.penJoinStyle_ = this.penJoinStyle_;
		result.width_ = this.width_;
		result.color_ = this.color_;
		return result;
	}

	/**
	 * Comparison operator.
	 * 
	 * Returns true if the pens are exactly the same.
	 */
	public boolean equals(WPen other) {
		return this.penStyle_ == other.penStyle_
				&& this.penCapStyle_ == other.penCapStyle_
				&& this.penJoinStyle_ == other.penJoinStyle_
				&& this.width_.equals(other.width_)
				&& this.color_.equals(other.color_);
	}

	/**
	 * Change the pen style.
	 * 
	 * @see WPen#getStyle()
	 */
	public void setStyle(PenStyle style) {
		this.penStyle_ = style;
	}

	/**
	 * Return the pen style.
	 * 
	 * @see WPen#setStyle(PenStyle style)
	 */
	public PenStyle getStyle() {
		return this.penStyle_;
	}

	/**
	 * Change the style for rendering line ends.
	 * 
	 * @see WPen#getCapStyle()
	 */
	public void setCapStyle(PenCapStyle style) {
		this.penCapStyle_ = style;
	}

	/**
	 * Returns the style for rendering line ends.
	 * 
	 * @see WPen#setCapStyle(PenCapStyle style)
	 */
	public PenCapStyle getCapStyle() {
		return this.penCapStyle_;
	}

	/**
	 * Change the style for rendering line joins.
	 * 
	 * @see WPen#getJoinStyle()
	 */
	public void setJoinStyle(PenJoinStyle style) {
		this.penJoinStyle_ = style;
	}

	/**
	 * Returns the style for rendering line joins.
	 * 
	 * @see WPen#setJoinStyle(PenJoinStyle style)
	 */
	public PenJoinStyle getJoinStyle() {
		return this.penJoinStyle_;
	}

	/**
	 * Change the pen width.
	 * 
	 * A pen width <i>must</i> be specified using {@link WLength.Unit#Pixel}
	 * units.
	 * <p>
	 * 
	 * @see WPen#getWidth()
	 */
	public void setWidth(WLength width) {
		this.width_ = width;
	}

	/**
	 * Returns the pen width.
	 * 
	 * @see WPen#setWidth(WLength width)
	 */
	public WLength getWidth() {
		return this.width_;
	}

	/**
	 * Change the pen color.
	 * 
	 * @see WPen#getColor()
	 */
	public void setColor(WColor color) {
		this.color_ = color;
	}

	/**
	 * Returns the pen color.
	 * 
	 * @see WPen#getColor()
	 */
	public WColor getColor() {
		return this.color_;
	}

	private PenStyle penStyle_;
	private PenCapStyle penCapStyle_;
	private PenJoinStyle penJoinStyle_;
	private WLength width_;
	private WColor color_;
}