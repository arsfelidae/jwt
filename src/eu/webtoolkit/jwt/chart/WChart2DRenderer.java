package eu.webtoolkit.jwt.chart;

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
 * Helper class for rendering a cartesian chart.
 * 
 * 
 * This class is used by {@link WCartesianChart} during rendering, and normally,
 * you will not need to use this class directly. You may want to specialize this
 * class if you want to override particular aspects of how the chart is
 * renderered. In that case, you will want to instantiate the specialized class
 * in {@link WCartesianChart#createRenderer(WPainter painter, WRectF rectangle)}
 * .
 * <p>
 * To simplify the simulatenous handling of Horizontal and Vertical charts, the
 * renderer makes abstraction of the orientation of the chart: regardless of the
 * chart orientation, the {@link WChart2DRenderer#getWidth()} corresponds to the
 * length along the X axis, and {@link WChart2DRenderer#getHeight()} corresponds
 * to the length along the Y axis. Similarly,
 * {@link WChart2DRenderer#calcChartArea()} and
 * {@link WChart2DRenderer#getChartArea()} return a rectangle where the bottom
 * side corresponds to the lowest displayed Y values, and the left side
 * corresponds to the lowest displayed X values. To map these &quot;chart
 * coordinates&quot; to painter coordinates, use one of the
 * {@link WChart2DRenderer#hv(double x, double y)} methods.
 * <p>
 * <i>Note, this class is part of the internal charting API, and may be subject
 * of changes and refactorings.</i>
 */
public class WChart2DRenderer {
	/**
	 * Create a renderer.
	 * 
	 * Creates a renderer for the cartesian chart <i>chart</i>, for rendering in
	 * the specified <i>rectangle</i> of the <i>painter</i>.
	 */
	public WChart2DRenderer(WCartesianChart chart, WPainter painter,
			WRectF rectangle) {
		this.chart_ = chart;
		this.painter_ = painter;
		this.chartArea_ = new WRectF();
		this.tildeStartMarker_ = new WPainterPath();
		this.tildeEndMarker_ = new WPainterPath();
		this.segmentMargin_ = 40;
		this.painter_.save();
		if (this.chart_.getOrientation() == Orientation.Vertical) {
			this.painter_.translate(rectangle.getTopLeft());
			this.width_ = (int) rectangle.getWidth();
			this.height_ = (int) rectangle.getHeight();
		} else {
			this.painter_.translate(rectangle.getTopLeft());
			this.width_ = (int) rectangle.getHeight();
			this.height_ = (int) rectangle.getWidth();
		}
		for (int i = 0; i < 3; ++i) {
			this.location_[i] = AxisLocation.MinimumValue;
		}
	}

	/**
	 * Destructor.
	 */
	public void destroy() {
		this.painter_.restore();
	}

	/**
	 * Returns the corresponding chart.
	 */
	public WCartesianChart getChart() {
		return this.chart_;
	}

	/**
	 * Returns a reference to the painter.
	 */
	public WPainter getPainter() {
		return this.painter_;
	}

	/**
	 * Returns the main plotting area rectangle.
	 * 
	 * This area is calculated and cached by
	 * {@link WChart2DRenderer#calcChartArea()}.
	 */
	public WRectF getChartArea() {
		return this.chartArea_;
	}

	/**
	 * Calculates the main plotting area rectangle.
	 * 
	 * This method calculates the main plotting area, and stores it in the
	 * memeber chartArea_. The default implementation simply removes the plot
	 * area padding from the entire painting rectangle.
	 * <p>
	 * 
	 * @see WAbstractChart#getPlotAreaPadding(Side side)
	 */
	public void calcChartArea() {
		if (this.chart_.getOrientation() == Orientation.Vertical) {
			this.chartArea_ = new WRectF(this.chart_
					.getPlotAreaPadding(Side.Left), this.chart_
					.getPlotAreaPadding(Side.Top), this.width_
					- this.chart_.getPlotAreaPadding(Side.Left)
					- this.chart_.getPlotAreaPadding(Side.Right), this.height_
					- this.chart_.getPlotAreaPadding(Side.Top)
					- this.chart_.getPlotAreaPadding(Side.Bottom));
		} else {
			this.chartArea_ = new WRectF(this.chart_
					.getPlotAreaPadding(Side.Top), this.chart_
					.getPlotAreaPadding(Side.Right), this.width_
					- this.chart_.getPlotAreaPadding(Side.Top)
					- this.chart_.getPlotAreaPadding(Side.Bottom), this.height_
					- this.chart_.getPlotAreaPadding(Side.Right)
					- this.chart_.getPlotAreaPadding(Side.Left));
		}
	}

	/**
	 * Renders the chart.
	 * 
	 * This method renders the chart. The default implementation does the
	 * following:
	 * <p>
	 * You may want to reimplement this method to change the sequence of steps
	 * for rendering the chart.
	 */
	public void render() {
		this.tildeStartMarker_.assign(new WPainterPath());
		this.tildeStartMarker_.moveTo(0, 0);
		this.tildeStartMarker_.lineTo(0, this.segmentMargin_ - 25);
		this.tildeStartMarker_.moveTo(-15, this.segmentMargin_ - 10);
		this.tildeStartMarker_.lineTo(15, this.segmentMargin_ - 20);
		this.tildeEndMarker_.assign(new WPainterPath());
		this.tildeEndMarker_.moveTo(0, 0);
		this.tildeEndMarker_.lineTo(0, -(this.segmentMargin_ - 25));
		this.tildeEndMarker_.moveTo(-15, -(this.segmentMargin_ - 20));
		this.tildeEndMarker_.lineTo(15, -(this.segmentMargin_ - 10));
		this.calcChartArea();
		this.prepareAxes();
		this.renderBackground();
		this.renderAxes(EnumSet.of(WChart2DRenderer.AxisProperty.Grid));
		this.renderSeries();
		this.renderAxes(EnumSet.of(WChart2DRenderer.AxisProperty.Line,
				WChart2DRenderer.AxisProperty.Labels));
		this.renderLegend();
	}

	/**
	 * Map a (X, Y) point to chart coordinates.
	 * 
	 * This method maps the point with given (<i>xValue</i>, <i>yValue</i>) to
	 * chart coordinates. The y value is mapped by one of the Y axes indicated
	 * by <i>axis</i>.
	 * <p>
	 * Note that chart coordinates may not be the same as painter coordinates,
	 * because of the chart orientation. To map from chart coordinates to
	 * painter coordinates, use {@link WChart2DRenderer#hv(double x, double y)}.
	 * <p>
	 * The <i>currentXSegment</i> and <i>currentYSegment</i> specify the axis
	 * segments in which you wish to map the point.
	 */
	public WPointF map(double xValue, double yValue, Axis axis,
			int currentXSegment, int currentYSegment) {
		return new WPointF(this.chart_.axis(Axis.XAxis).map(xValue,
				this.location_[Axis.YAxis.getValue()], currentXSegment),
				this.chart_.axis(axis).map(yValue,
						this.location_[Axis.XAxis.getValue()], currentYSegment));
	}

	public final WPointF map(double xValue, double yValue) {
		return map(xValue, yValue, Axis.OrdinateAxis, 0, 0);
	}

	public final WPointF map(double xValue, double yValue, Axis axis) {
		return map(xValue, yValue, axis, 0, 0);
	}

	public final WPointF map(double xValue, double yValue, Axis axis,
			int currentXSegment) {
		return map(xValue, yValue, axis, currentXSegment, 0);
	}

	/**
	 * Utility function for rendering text.
	 * 
	 * This method renders text on the chart position <i>pos</i>, with a
	 * particular alignment <i>flags</i>. These are both specified in chart
	 * coordinates. The position is converted to painter coordinates using
	 * {@link WChart2DRenderer#hv(double x, double y)}, and the alignment flags
	 * are changed accordingly. The rotation, indicated by <i>angle</i> is
	 * specified in painter coordinates and thus an angle of 0 always indicates
	 * horizontal text, regardless of the chart orientation.
	 */
	public void renderLabel(CharSequence text, WPointF p, WColor color,
			EnumSet<AlignmentFlag> flags, double angle, int margin) {
		AlignmentFlag horizontalAlign = EnumUtils.enumFromSet(EnumUtils.mask(
				flags, AlignmentFlag.AlignHorizontalMask));
		AlignmentFlag verticalAlign = EnumUtils.enumFromSet(EnumUtils.mask(
				flags, AlignmentFlag.AlignVerticalMask));
		AlignmentFlag rHorizontalAlign = horizontalAlign;
		AlignmentFlag rVerticalAlign = verticalAlign;
		double width = 100;
		double height = 20;
		WPointF pos = this.hv(p);
		double left = pos.getX();
		double top = pos.getY();
		if (this.chart_.getOrientation() == Orientation.Horizontal) {
			switch (horizontalAlign) {
			case AlignLeft:
				rVerticalAlign = AlignmentFlag.AlignTop;
				break;
			case AlignCenter:
				rVerticalAlign = AlignmentFlag.AlignMiddle;
				break;
			case AlignRight:
				rVerticalAlign = AlignmentFlag.AlignBottom;
				break;
			default:
				break;
			}
			switch (verticalAlign) {
			case AlignTop:
				rHorizontalAlign = AlignmentFlag.AlignRight;
				break;
			case AlignMiddle:
				rHorizontalAlign = AlignmentFlag.AlignCenter;
				break;
			case AlignBottom:
				rHorizontalAlign = AlignmentFlag.AlignLeft;
				break;
			default:
				break;
			}
		}
		switch (rHorizontalAlign) {
		case AlignLeft:
			left = pos.getX() + margin;
			break;
		case AlignCenter:
			left = pos.getX() - width / 2;
			break;
		case AlignRight:
			left = pos.getX() - width - margin;
		default:
			break;
		}
		switch (rVerticalAlign) {
		case AlignTop:
			top = pos.getY() + margin;
			break;
		case AlignMiddle:
			top = pos.getY() - height / 2;
			break;
		case AlignBottom:
			top = pos.getY() - height - margin;
			break;
		default:
			break;
		}
		WPen pen = new WPen(color);
		WPen oldPen = this.painter_.getPen();
		this.painter_.setPen(pen);
		if (angle == 0) {
			this.painter_.drawText(new WRectF(left, top, width, height),
					EnumSet.of(rHorizontalAlign, rVerticalAlign), text);
		} else {
			this.painter_.save();
			this.painter_.translate(pos);
			this.painter_.rotate(-angle);
			this.painter_.drawText(new WRectF(left - pos.getX(), top
					- pos.getY(), width, height), EnumSet.of(rHorizontalAlign,
					rVerticalAlign), text);
			this.painter_.restore();
		}
		this.painter_.setPen(oldPen);
	}

	/**
	 * Conversion between chart and painter coordinates.
	 * 
	 * Converts from chart coordinates to painter coordinates, taking into
	 * account the chart orientation.
	 */
	public WPointF hv(double x, double y) {
		if (this.chart_.getOrientation() == Orientation.Vertical) {
			return new WPointF(x, y);
		} else {
			return new WPointF(this.height_ - y, x);
		}
	}

	/**
	 * Conversion between chart and painter coordinates.
	 * 
	 * Converts from chart coordinates to painter coordinates, taking into
	 * account the chart orientation.
	 */
	public WPointF hv(WPointF p) {
		return this.hv(p.getX(), p.getY());
	}

	/**
	 * Conversion between chart and painter coordinates.
	 * 
	 * Converts from chart coordinates to painter coordinates, taking into
	 * account the chart orientation.
	 */
	public WRectF hv(WRectF r) {
		if (this.chart_.getOrientation() == Orientation.Vertical) {
			return r;
		} else {
			WPointF tl = this.hv(r.getBottomLeft());
			return new WRectF(tl.getX(), tl.getY(), r.getHeight(), r.getWidth());
		}
	}

	/**
	 * Returns the segment area for a combination of X and Y segments.
	 * 
	 * This segment area is used for clipping when rendering in a particular
	 * segment.
	 */
	public WRectF chartSegmentArea(WAxis yAxis, int xSegment, int ySegment) {
		WAxis xAxis = this.chart_.axis(Axis.XAxis);
		WAxis.Segment xs = xAxis.segments_.get(xSegment);
		WAxis.Segment ys = yAxis.segments_.get(ySegment);
		double x1 = xs.renderStart
				+ (xSegment == 0 ? 0 : -this.segmentMargin_ / 2);
		double x2 = xs.renderStart
				+ xs.renderLength
				+ (xSegment == xAxis.getSegmentCount() - 1 ? 0
						: this.segmentMargin_ / 2);
		double y1 = ys.renderStart
				- ys.renderLength
				- (ySegment == yAxis.getSegmentCount() - 1 ? 0
						: this.segmentMargin_ / 2);
		double y2 = ys.renderStart
				+ (ySegment == 0 ? 0 : this.segmentMargin_ / 2);
		return new WRectF(Math.floor(x1 + 0.5), Math.floor(y1 + 0.5), Math
				.floor(x2 - x1 + 0.5), Math.floor(y2 - y1 + 0.5));
	}

	/**
	 * Enumeration that specifies a property of the axes.
	 */
	public enum AxisProperty {
		Labels, Grid, Line;

		public int getValue() {
			return ordinal();
		}
	}

	/**
	 * Prepare the axes for rendering.
	 * 
	 * Computes axis properties such as the range (if not manually specified),
	 * label interval (if not manually specified) and axis locations. These
	 * properties are stored within the axes (we may want to change that later
	 * to allow for reentrant rendering by multiple renderers ?).
	 */
	protected void prepareAxes() {
		this.chart_.axis(Axis.XAxis).prepareRender(this);
		this.chart_.axis(Axis.Y1Axis).prepareRender(this);
		this.chart_.axis(Axis.Y2Axis).prepareRender(this);
		if (this.chart_.axis(Axis.XAxis).getScale() == AxisScale.CategoryScale) {
			switch (this.chart_.axis(Axis.XAxis).getLocation()) {
			case MinimumValue:
			case ZeroValue:
				this.location_[Axis.XAxis.getValue()] = AxisLocation.MinimumValue;
				break;
			case MaximumValue:
				this.location_[Axis.XAxis.getValue()] = AxisLocation.MaximumValue;
			}
		}
		WAxis xAxis = this.chart_.axis(Axis.XAxis);
		WAxis yAxis = this.chart_.axis(Axis.YAxis);
		AxisLocation xAxisLocation = this.location_[Axis.XAxis.getValue()];
		AxisLocation yAxisLocation = this.location_[Axis.YAxis.getValue()];
		for (int i = 0; i < 2; ++i) {
			AxisLocation location = i == 0 ? xAxisLocation : yAxisLocation;
			location = i == 0 ? xAxis.getLocation() : yAxis.getLocation();
			WAxis other = i == 0 ? yAxis : xAxis;
			if (location == AxisLocation.ZeroValue) {
				if (other.segments_.get(0).renderMaximum < 0) {
					location = AxisLocation.MaximumValue;
				} else {
					if (other.segments_.get(0).renderMinimum > 0) {
						location = AxisLocation.MinimumValue;
					}
				}
			} else {
				if (location == AxisLocation.MinimumValue) {
					if (other.segments_.get(0).renderMinimum == 0) {
						location = AxisLocation.ZeroValue;
					}
				} else {
					if (other.segments_.get(0).renderMaximum == 0) {
						location = AxisLocation.MaximumValue;
					}
				}
			}
		}
		if (this.chart_.axis(Axis.Y2Axis).isVisible()) {
			if (!(this.location_[Axis.Y1Axis.getValue()] == AxisLocation.ZeroValue && this.chart_
					.axis(Axis.XAxis).segments_.get(0).renderMinimum == 0)) {
				this.location_[Axis.Y1Axis.getValue()] = AxisLocation.MinimumValue;
			}
			this.location_[Axis.Y2Axis.getValue()] = AxisLocation.MaximumValue;
		} else {
			this.location_[Axis.Y2Axis.getValue()] = AxisLocation.MaximumValue;
		}
	}

	/**
	 * Renders the background.
	 */
	protected void renderBackground() {
		if (this.chart_.getBackground().getStyle() != WBrushStyle.NoBrush) {
			this.painter_.fillRect(this.hv(this.chartArea_), this.chart_
					.getBackground());
		}
	}

	/**
	 * Renders one or more properties of the axes.
	 */
	protected void renderAxes(EnumSet<WChart2DRenderer.AxisProperty> properties) {
		this.renderAxis(this.chart_.axis(Axis.XAxis), this.location_[0],
				properties);
		this.renderAxis(this.chart_.axis(Axis.Y1Axis), this.location_[1],
				properties);
		this.renderAxis(this.chart_.axis(Axis.Y2Axis), this.location_[2],
				properties);
	}

	protected final void renderAxes(WChart2DRenderer.AxisProperty propertie,
			WChart2DRenderer.AxisProperty... properties) {
		renderAxes(EnumSet.of(propertie, properties));
	}

	/**
	 * Renders all series data, including value labels.
	 */
	protected void renderSeries() {
		{
			SeriesRenderIterator iterator = new SeriesRenderIterator(this);
			this.iterateSeries(iterator, true);
		}
		{
			LabelRenderIterator iterator = new LabelRenderIterator(this);
			this.iterateSeries(iterator);
		}
		{
			MarkerRenderIterator iterator = new MarkerRenderIterator(this);
			this.iterateSeries(iterator);
		}
	}

	/**
	 * Renders the (default) legend and chart titles.
	 */
	protected void renderLegend() {
		boolean vertical = this.chart_.getOrientation() == Orientation.Vertical;
		if (this.chart_.isLegendEnabled()) {
			int numSeriesWithLegend = 0;
			for (int i = 0; i < this.chart_.getSeries().size(); ++i) {
				if (this.chart_.getSeries().get(i).isLegendEnabled()) {
					++numSeriesWithLegend;
				}
			}
			final int lineHeight = 25;
			int x = (int) (vertical ? this.chartArea_.getRight() : this.height_
					- this.chartArea_.getTop()) + 20;
			if (vertical && this.chart_.axis(Axis.Y2Axis).isVisible()) {
				x += 40;
			}
			int y = (vertical ? (int) this.chartArea_.getCenter().getY()
					: (int) this.chartArea_.getCenter().getX())
					- lineHeight * numSeriesWithLegend / 2;
			this.painter_.setPen(new WPen());
			for (int i = 0; i < this.chart_.getSeries().size(); ++i) {
				if (this.chart_.getSeries().get(i).isLegendEnabled()) {
					this.chart_
							.renderLegendItem(this.painter_, new WPointF(x, y
									+ lineHeight / 2), this.chart_.getSeries()
									.get(i));
					y += lineHeight;
				}
			}
		}
		if (!(this.chart_.getTitle().length() == 0)) {
			int x = vertical ? this.width_ / 2 : this.height_ / 2;
			WFont oldFont = this.painter_.getFont();
			WFont titleFont = this.chart_.getTitleFont();
			this.painter_.setFont(titleFont);
			this.painter_.drawText(x - 50, 5, 100, 50, EnumSet.of(
					AlignmentFlag.AlignCenter, AlignmentFlag.AlignTop),
					this.chart_.getTitle());
			this.painter_.setFont(oldFont);
		}
	}

	/**
	 * Returns the width along the X axis (as if orientation is Vertical).
	 */
	protected int getWidth() {
		return this.width_;
	}

	/**
	 * Returns the height along the Y axis (as if orientation is Vertical).
	 */
	protected int getHeight() {
		return this.height_;
	}

	/**
	 * Returns the segment margin.
	 * 
	 * This is the separation between segments, and defaults to 40 pixels.
	 */
	protected int getSegmentMargin() {
		return this.segmentMargin_;
	}

	private WCartesianChart chart_;
	private WPainter painter_;
	private int width_;
	private int height_;
	private int segmentMargin_;
	private WRectF chartArea_;
	private WPainterPath tildeStartMarker_;
	private WPainterPath tildeEndMarker_;
	/**
	 * The computed axis locations.
	 * 
	 * @see WChart2DRenderer#prepareAxes()
	 */
	protected AxisLocation[] location_ = new AxisLocation[3];

	/**
	 * Render properties of one axis.
	 * 
	 * @see WChart2DRenderer#renderAxes(EnumSet properties)
	 */
	protected void renderAxis(WAxis axis, AxisLocation l,
			EnumSet<WChart2DRenderer.AxisProperty> properties) {
		boolean vertical = axis.getId() != Axis.XAxis;
		double u = 0;
		final int Left = 0;
		final int Right = 1;
		final int Both = 2;
		int tickPos = Left;
		AlignmentFlag labelHFlag = AlignmentFlag.AlignLeft;
		switch (l) {
		case MinimumValue:
			tickPos = Left;
			if (vertical) {
				labelHFlag = AlignmentFlag.AlignRight;
				u = this.chartArea_.getLeft() - 0.5 - axis.getMargin();
			} else {
				labelHFlag = AlignmentFlag.AlignTop;
				u = this.chartArea_.getBottom() + 0.5 + axis.getMargin();
			}
			break;
		case MaximumValue:
			tickPos = Right;
			if (vertical) {
				labelHFlag = AlignmentFlag.AlignLeft;
				u = this.chartArea_.getRight() + 0.5 + axis.getMargin();
			} else {
				labelHFlag = AlignmentFlag.AlignBottom;
				u = this.chartArea_.getTop() - 0.5 - axis.getMargin();
			}
			break;
		case ZeroValue:
			tickPos = Both;
			if (vertical) {
				labelHFlag = AlignmentFlag.AlignRight;
				u = Math.floor(this.map(0, 0, Axis.YAxis).getX()) + 0.5;
			} else {
				labelHFlag = AlignmentFlag.AlignTop;
				u = Math.floor(this.map(0, 0, Axis.YAxis).getY()) + 0.5;
			}
			break;
		}
		for (int segment = 0; segment < axis.getSegmentCount(); ++segment) {
			WAxis.Segment s = axis.segments_.get(segment);
			if (!EnumUtils.mask(properties, WChart2DRenderer.AxisProperty.Line)
					.isEmpty()
					&& axis.isVisible()) {
				this.painter_.setPen(axis.getPen());
				WPointF begin = new WPointF();
				WPointF end = new WPointF();
				if (vertical) {
					begin = this.hv(u, s.renderStart);
					end = this.hv(u, s.renderStart - s.renderLength);
				} else {
					begin = this.hv(s.renderStart, u);
					end = this.hv(s.renderStart + s.renderLength, u);
				}
				this.painter_.drawLine(begin, end);
				boolean rotate = this.chart_.getOrientation() == Orientation.Vertical != vertical;
				if (segment != 0) {
					this.painter_.save();
					this.painter_.translate(begin);
					if (rotate) {
						this.painter_.rotate(90);
					}
					this.painter_.drawPath(this.tildeStartMarker_);
					this.painter_.restore();
				}
				if (segment != axis.getSegmentCount() - 1) {
					this.painter_.save();
					this.painter_.translate(end);
					if (rotate) {
						this.painter_.rotate(90);
					}
					this.painter_.drawPath(this.tildeEndMarker_);
					this.painter_.restore();
				}
			}
			WPainterPath gridPath = new WPainterPath();
			WPainterPath ticksPath = new WPainterPath();
			List<WAxis.TickLabel> ticks = new ArrayList<WAxis.TickLabel>();
			axis.getLabelTicks(this, ticks, segment);
			for (int i = 0; i < ticks.size(); ++i) {
				double d = ticks.get(i).u;
				double dd = axis.map(d,
						axis.getId() == Axis.XAxis ? this.location_[Axis.YAxis
								.getValue()] : this.location_[Axis.XAxis
								.getValue()], segment);
				dd = Math.floor(dd) + 0.5;
				int tickLength = ticks.get(i).tickLength == WAxis.TickLabel.TickLength.Long ? TICK_LENGTH
						: TICK_LENGTH / 2;
				WPointF labelPos = new WPointF();
				switch (l) {
				case MinimumValue:
					if (vertical) {
						labelPos = new WPointF(u - tickLength, dd);
					} else {
						labelPos = new WPointF(dd, u + tickLength);
					}
					break;
				case MaximumValue:
					if (vertical) {
						labelPos = new WPointF(u + tickLength, dd);
					} else {
						labelPos = new WPointF(dd, u - tickLength);
					}
					break;
				case ZeroValue:
					if (vertical) {
						if (this.chart_.getType() == ChartType.CategoryChart) {
							labelPos = new WPointF(this.chartArea_.getLeft()
									- 0.5 - axis.getMargin() - tickLength, dd);
						} else {
							labelPos = new WPointF(u - tickLength, dd);
						}
					} else {
						if (this.chart_.getType() == ChartType.CategoryChart) {
							labelPos = new WPointF(dd, this.chartArea_
									.getBottom()
									+ 0.5 + axis.getMargin() + tickLength);
						} else {
							labelPos = new WPointF(dd, u + tickLength);
						}
					}
				}
				if (ticks.get(i).tickLength != WAxis.TickLabel.TickLength.Zero) {
					if (vertical) {
						ticksPath.moveTo(this
								.hv(u
										+ ((tickPos & Left) != 0 ? -tickLength
												: 0), dd));
						ticksPath.lineTo(this.hv(u
								+ ((tickPos & Right) != 0 ? +tickLength : 0),
								dd));
						if (ticks.get(i).tickLength == WAxis.TickLabel.TickLength.Long) {
							gridPath.moveTo(this.hv(this.chartArea_.getLeft(),
									dd));
							gridPath.lineTo(this.hv(this.chartArea_.getRight(),
									dd));
						}
					} else {
						ticksPath.moveTo(this.hv(dd, u
								+ ((tickPos & Right) != 0 ? -tickLength : 0)));
						ticksPath.lineTo(this.hv(dd, u
								+ ((tickPos & Left) != 0 ? +tickLength : 0)));
						if (ticks.get(i).tickLength == WAxis.TickLabel.TickLength.Long) {
							gridPath.moveTo(this.hv(dd, this.chartArea_
									.getTop()));
							gridPath.lineTo(this.hv(dd, this.chartArea_
									.getBottom()));
						}
					}
				}
				if (!EnumUtils.mask(properties,
						WChart2DRenderer.AxisProperty.Labels).isEmpty()
						&& !(ticks.get(i).label.length() == 0)
						&& axis.isVisible()) {
					EnumSet<AlignmentFlag> labelFlags = EnumSet.of(labelHFlag);
					if (vertical) {
						if (axis.getLabelAngle() == 0) {
							labelFlags.add(AlignmentFlag.AlignMiddle);
						} else {
							if (axis.getLabelAngle() > 0) {
								labelFlags.add(AlignmentFlag.AlignTop);
							} else {
								labelFlags.add(AlignmentFlag.AlignBottom);
							}
						}
					} else {
						if (axis.getLabelAngle() == 0) {
							labelFlags.add(AlignmentFlag.AlignCenter);
						} else {
							if (axis.getLabelAngle() > 0) {
								labelFlags.add(AlignmentFlag.AlignRight);
							} else {
								labelFlags.add(AlignmentFlag.AlignLeft);
							}
						}
					}
					this.renderLabel(ticks.get(i).label, labelPos,
							WColor.black, labelFlags, axis.getLabelAngle(), 3);
				}
			}
			if (!EnumUtils.mask(properties, WChart2DRenderer.AxisProperty.Grid)
					.isEmpty()
					&& axis.isGridLinesEnabled()) {
				this.painter_.strokePath(gridPath, axis.getGridLinesPen());
			}
			if (!EnumUtils.mask(properties, WChart2DRenderer.AxisProperty.Line)
					.isEmpty()
					&& axis.isVisible()) {
				this.painter_.strokePath(ticksPath, axis.getPen());
			}
			if (segment == 0
					&& !EnumUtils.mask(properties,
							WChart2DRenderer.AxisProperty.Labels).isEmpty()
					&& !(axis.getTitle().length() == 0)) {
				WFont oldFont = this.painter_.getFont();
				WFont titleFont = axis.getTitleFont();
				this.painter_.setFont(titleFont);
				boolean chartVertical = this.chart_.getOrientation() == Orientation.Vertical;
				if (vertical) {
					if (chartVertical) {
						this.renderLabel(axis.getTitle(), new WPointF(u
								+ (labelHFlag == AlignmentFlag.AlignRight ? 15
										: -15), this.chartArea_.getTop() - 8),
								WColor.black, EnumSet.of(labelHFlag,
										AlignmentFlag.AlignBottom), 0, 0);
					} else {
						this
								.renderLabel(
										axis.getTitle(),
										new WPointF(
												u
														+ (labelHFlag == AlignmentFlag.AlignRight ? -40
																: +40),
												this.chartArea_.getCenter()
														.getY()),
										WColor.black,
										EnumSet
												.of(
														labelHFlag == AlignmentFlag.AlignRight ? AlignmentFlag.AlignLeft
																: AlignmentFlag.AlignRight,
														AlignmentFlag.AlignMiddle),
										0, 0);
					}
				} else {
					if (chartVertical) {
						this.renderLabel(axis.getTitle(), new WPointF(
								this.chartArea_.getCenter().getX(), u + 22),
								WColor.black, EnumSet.of(
										AlignmentFlag.AlignTop,
										AlignmentFlag.AlignCenter), 0, 0);
					} else {
						this.renderLabel(axis.getTitle(), new WPointF(
								this.chartArea_.getRight(), u), WColor.black,
								EnumSet.of(AlignmentFlag.AlignTop,
										AlignmentFlag.AlignLeft), 0, 8);
					}
				}
				this.painter_.setFont(oldFont);
			}
		}
	}

	protected final void renderAxis(WAxis axis, AxisLocation l,
			WChart2DRenderer.AxisProperty propertie,
			WChart2DRenderer.AxisProperty... properties) {
		renderAxis(axis, l, EnumSet.of(propertie, properties));
	}

	/**
	 * Calculate the total number of bar groups.
	 */
	protected int getCalcNumBarGroups() {
		List<WDataSeries> series = this.chart_.getSeries();
		int numBarGroups = 0;
		boolean newGroup = true;
		for (int i = 0; i < series.size(); ++i) {
			if (series.get(i).getType() == SeriesType.BarSeries) {
				if (newGroup || !series.get(i).isStacked()) {
					++numBarGroups;
				}
				newGroup = false;
			} else {
				newGroup = true;
			}
		}
		return numBarGroups;
	}

	/**
	 * Iterate over the series using an iterator.
	 */
	protected void iterateSeries(SeriesIterator iterator, boolean reverseStacked) {
		List<WDataSeries> series = this.chart_.getSeries();
		WAbstractItemModel model = this.chart_.getModel();
		int rows = model.getRowCount();
		double groupWidth;
		int numBarGroups;
		int currentBarGroup;
		List<Double> stackedValuesInit = new ArrayList<Double>();
		{
			int insertPos = 0;
			for (int ii = 0; ii < rows; ++ii)
				stackedValuesInit.add(insertPos + ii, 0.0);
		}
		;
		final boolean scatterPlot = this.chart_.getType() == ChartType.ScatterPlot;
		if (scatterPlot) {
			groupWidth = CATEGORY_WIDTH
					* (this.map(2, 0).getX() - this.map(1, 0).getX());
			numBarGroups = 1;
			currentBarGroup = 0;
		} else {
			groupWidth = CATEGORY_WIDTH
					* (this.map(2, 0).getX() - this.map(1, 0).getX());
			numBarGroups = this.getCalcNumBarGroups();
			currentBarGroup = 0;
		}
		boolean containsBars = false;
		for (int g = 0; g < series.size(); ++g) {
			if (containsBars) {
				++currentBarGroup;
			}
			containsBars = false;
			int startSeries;
			int endSeries;
			if (scatterPlot) {
				startSeries = endSeries = g;
			} else {
				for (int i = 0; i < rows; ++i) {
					stackedValuesInit.set(i, 0.0);
				}
				if (reverseStacked) {
					endSeries = g;
					Axis a = series.get(g).getAxis();
					for (;;) {
						if (g < series.size()
								&& ((int) g == endSeries || series.get(g)
										.isStacked())
								&& series.get(g).getAxis() == a) {
							if (series.get(g).getType() == SeriesType.BarSeries) {
								containsBars = true;
							}
							for (int row = 0; row < rows; ++row) {
								double y = StringUtils.asNumber(model.getData(
										row, series.get(g).getModelColumn()));
								if (!myisnan(y)) {
									stackedValuesInit.set(row, y);
								}
							}
							++g;
						} else {
							break;
						}
					}
					--g;
					startSeries = g;
				} else {
					startSeries = g;
					Axis a = series.get(g).getAxis();
					if (series.get(g).getType() == SeriesType.BarSeries) {
						containsBars = true;
					}
					++g;
					for (;;) {
						if (g < series.size() && series.get(g).isStacked()
								&& series.get(g).getAxis() == a) {
							if (series.get(g).getType() == SeriesType.BarSeries) {
								containsBars = true;
							}
							++g;
						} else {
							break;
						}
					}
					--g;
					endSeries = g;
				}
			}
			int i = startSeries;
			for (;;) {
				boolean doSeries = iterator.startSeries(series.get(i),
						groupWidth, numBarGroups, currentBarGroup);
				List<Double> stackedValues = new ArrayList<Double>();
				if (doSeries || !scatterPlot && i != endSeries) {
					for (int currentXSegment = 0; currentXSegment < this.chart_
							.axis(Axis.XAxis).getSegmentCount(); ++currentXSegment) {
						for (int currentYSegment = 0; currentYSegment < this.chart_
								.axis(series.get(i).getAxis())
								.getSegmentCount(); ++currentYSegment) {
							stackedValues = stackedValuesInit;
							WRectF csa = this.chartSegmentArea(this.chart_
									.axis(series.get(i).getAxis()),
									currentXSegment, currentYSegment);
							iterator.setSegment(currentXSegment,
									currentYSegment, csa);
							this.painter_.save();
							this.painter_.setClipping(true);
							WPainterPath clipPath = new WPainterPath();
							clipPath.addRect(this.hv(csa));
							this.painter_.setClipPath(clipPath);
							for (int row = 0; row < rows; ++row) {
								double x;
								if (scatterPlot) {
									if (this.chart_.XSeriesColumn() != -1) {
										x = StringUtils.asNumber(model.getData(
												row, this.chart_
														.XSeriesColumn()));
									} else {
										x = row;
									}
								} else {
									x = row;
								}
								double y = StringUtils.asNumber(model.getData(
										row, series.get(i).getModelColumn()));
								double prevStack;
								if (scatterPlot) {
									iterator.newValue(series.get(i), x, y, 0);
								} else {
									prevStack = stackedValues.get(row);
									double nextStack = stackedValues.get(row);
									if (!myisnan(y)) {
										if (reverseStacked) {
											nextStack -= y;
										} else {
											nextStack += y;
										}
									}
									stackedValues.set(row, nextStack);
									if (doSeries) {
										if (reverseStacked) {
											iterator.newValue(series.get(i), x,
													prevStack, nextStack);
										} else {
											iterator.newValue(series.get(i), x,
													nextStack, prevStack);
										}
									}
								}
							}
							this.painter_.restore();
						}
					}
					stackedValuesInit = stackedValues;
				}
				if (doSeries) {
					iterator.endSeries();
				}
				if (i == endSeries) {
					break;
				} else {
					if (endSeries < startSeries) {
						--i;
					} else {
						++i;
					}
				}
			}
		}
	}

	protected final void iterateSeries(SeriesIterator iterator) {
		iterateSeries(iterator, false);
	}

	static final int TICK_LENGTH = 5;
	static final double CATEGORY_WIDTH = 0.8;

	static boolean myisnan(double d) {
		return !(d == d);
	}
}