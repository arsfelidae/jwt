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
 * An abstract widget that corresponds to an HTML form element
 * 
 * 
 * A WFormWidget may receive focus, can be disabled, and can have a label that
 * acts as proxy for getting focus. It provides signals which reflect changes to
 * its value, or changes to its focus.
 * <p>
 * Form widgets also have built-in support for validation, using
 * {@link WFormWidget#setValidator(WValidator validator)}. If the validator
 * provide client-side validation, then an invalid validation state is reflected
 * using the style class <code>&quot;Wt-invalid&quot;</code>. All validators
 * provided by Wt implement client-side validation.
 * <p>
 * On the server-side, use {@link WFormWidget#validate()} method to validate the
 * content using a validator previously set.
 */
public abstract class WFormWidget extends WInteractWidget {
	/**
	 * Construct a {@link WFormWidget} with an optional parent.
	 */
	public WFormWidget(WContainerWidget parent) {
		super(parent);
		this.label_ = null;
		this.validator_ = null;
		this.validateJs_ = null;
		this.filterInput_ = null;
		this.flags_ = new BitSet();
		this.flags_.set(BIT_ENABLED);
	}

	public WFormWidget() {
		this((WContainerWidget) null);
	}

	/**
	 * Destroy a {@link WFormWidget}.
	 * 
	 * If a label was associated with the widget, its buddy is reset to 0.
	 */
	public void remove() {
		if (this.label_ != null) {
			this.label_.setBuddy((WFormWidget) null);
		}
		if (this.validator_ != null) {
			this.validator_.removeFormWidget(this);
		}
		/* delete this.validateJs_ */;
		/* delete this.filterInput_ */;
		super.remove();
	}

	/**
	 * Returns the label associated with this widget.
	 * 
	 * Returns the label (if there is one) that acts as a proxy for this widget.
	 * <p>
	 * 
	 * @see WLabel#setBuddy(WFormWidget buddy)
	 */
	public WLabel getLabel() {
		return this.label_;
	}

	/**
	 * Set the hidden state of this widget.
	 * 
	 * If the widget has a label, it is hidden and shown together with this
	 * widget.
	 */
	public void setHidden(boolean hidden) {
		if (this.label_ != null) {
			this.label_.setHidden(hidden);
		}
		super.setHidden(hidden);
	}

	/**
	 * Return whether the widget is enabled.
	 */
	public boolean isEnabled() {
		return this.flags_.get(BIT_ENABLED);
	}

	/**
	 * Set a validator for this field.
	 * 
	 * The validator is used to validate the current input.
	 * <p>
	 * If the validator has no parent yet, then ownership is transferred to the
	 * form field, and thus the validator will be deleted together with the form
	 * field.
	 * <p>
	 * The default value is 0.
	 * <p>
	 * 
	 * @see WFormWidget#validate()
	 */
	public void setValidator(WValidator validator) {
		if (this.validator_ != null) {
			this.validator_.removeFormWidget(this);
		}
		this.validator_ = validator;
		if (this.validator_ != null) {
			this.validator_.addFormWidget(this);
			this.validatorChanged();
			this.setStyleClass(this.validate() == WValidator.State.Valid ? ""
					: "Wt-invalid");
		} else {
			/* delete this.validateJs_ */;
			this.validateJs_ = null;
			/* delete this.filterInput_ */;
			this.filterInput_ = null;
		}
	}

	/**
	 * Get the validator.
	 */
	public WValidator getValidator() {
		return this.validator_;
	}

	/**
	 * Validate the field.
	 */
	public WValidator.State validate() {
		return WValidator.State.Valid;
	}

	/**
	 * Change the enabled state of the widget.
	 * 
	 * A widget that is disabled cannot receive focus or user interaction.
	 */
	public void setEnabled(boolean enabled) {
		this.flags_.set(BIT_WAS_ENABLED, this.flags_.get(BIT_ENABLED));
		this.flags_.set(BIT_ENABLED, enabled);
		this.flags_.set(BIT_ENABLED_CHANGED);
		this.repaint(EnumSet.of(RepaintFlag.RepaintPropertyAttribute));
	}

	/**
	 * Enable the widget.
	 * 
	 * @see WFormWidget#setEnabled(boolean enabled)
	 */
	public void enable() {
		this.setEnabled(true);
	}

	/**
	 * Disable the widget.
	 * 
	 * @see WFormWidget#setEnabled(boolean enabled)
	 */
	public void disable() {
		this.setEnabled(false);
	}

	/**
	 * Give focus to this widget.
	 * 
	 * Giving focus to an input element only works when JavaScript is enabled.
	 */
	public void setFocus() {
		this.flags_.set(BIT_GOT_FOCUS);
		this.repaint(EnumSet.of(RepaintFlag.RepaintPropertyIEMobile));
	}

	/**
	 * Set the element read-only.
	 * 
	 * A read-only form element cannot be edited, but the contents can still be
	 * selected.
	 * <p>
	 * By default, a form element area is not read-only.
	 * <p>
	 * 
	 * @see WFormWidget#setEnabled(boolean enabled)
	 */
	public void setReadOnly(boolean readOnly) {
		this.flags_.set(BIT_READONLY, readOnly);
		this.flags_.set(BIT_READONLY_CHANGED);
		this.repaint(EnumSet.of(RepaintFlag.RepaintPropertyAttribute));
	}

	/**
	 * Returns whether the form element is read-only.
	 * 
	 * @see WFormWidget#setReadOnly(boolean readOnly)
	 */
	public boolean isReadOnly() {
		return this.flags_.get(BIT_READONLY);
	}

	/**
	 * {@link Signal} emitted when the value was changed.
	 */
	public EventSignal changed() {
		return this.voidEventSignal(CHANGE_SIGNAL, true);
	}

	/**
	 * {@link Signal} emitted when ??
	 */
	public EventSignal selected() {
		return this.voidEventSignal(SELECT_SIGNAL, true);
	}

	/**
	 * {@link Signal} emitted when the widget lost focus.
	 */
	public EventSignal blurred() {
		return this.voidEventSignal(BLUR_SIGNAL, true);
	}

	/**
	 * {@link Signal} emitted when the widget recieved focus.
	 */
	public EventSignal focussed() {
		return this.voidEventSignal(FOCUS_SIGNAL, true);
	}

	protected WLabel label_;
	protected WValidator validator_;
	protected JSlot validateJs_;
	protected JSlot filterInput_;
	private static final int BIT_ENABLED = 0;
	private static final int BIT_ENABLED_CHANGED = 1;
	private static final int BIT_WAS_ENABLED = 2;
	private static final int BIT_GOT_FOCUS = 3;
	private static final int BIT_INITIAL_FOCUS = 4;
	private static final int BIT_READONLY = 5;
	private static final int BIT_READONLY_CHANGED = 6;
	private BitSet flags_;

	private void undoEnable() {
		this.setEnabled(this.flags_.get(BIT_WAS_ENABLED));
	}

	private void undoDisable() {
		this.undoEnable();
	}

	private void undoSetFocus() {
	}

	void setLabel(WLabel label) {
		if (this.label_ != null) {
			WLabel l = this.label_;
			this.label_ = null;
			l.setBuddy((WFormWidget) null);
		}
		this.label_ = label;
		if (this.label_ != null) {
			this.label_.setHidden(this.isHidden());
		}
	}

	void validatorChanged() {
		String validateJS = this.validator_.javaScriptValidate(this.getJsRef());
		if (validateJS.length() != 0) {
			if (!(this.validateJs_ != null)) {
				this.validateJs_ = new JSlot(this);
				this.keyWentUp().addListener(this.validateJs_);
				this.changed().addListener(this.validateJs_);
				this.clicked().addListener(this.validateJs_);
			}
			this.validateJs_
					.setJavaScript("function(self, event){var v="
							+ validateJS
							+ ";self.className= v.valid ? '' : 'Wt-invalid';if (v.valid) self.removeAttribute('title');else self.setAttribute('title', v.message);}");
		} else {
			/* delete this.validateJs_ */;
			this.validateJs_ = null;
		}
		String inputFilter = this.validator_.getInputFilter();
		if (inputFilter.length() != 0) {
			if (!(this.filterInput_ != null)) {
				this.filterInput_ = new JSlot();
				this.keyPressed().addListener(this.filterInput_);
			}
			StringUtils.replace(inputFilter, '/', "\\/");
			this.filterInput_
					.setJavaScript("function(self,e){var c=String.fromCharCode((typeof e.charCode!=='undefined')?e.charCode:e.keyCode);if(/"
							+ inputFilter
							+ "/.test(c)) return true; else{Wt2_99_2.cancelEvent(e);}}");
		} else {
			/* delete this.filterInput_ */;
			this.filterInput_ = null;
		}
	}

	protected void updateDom(DomElement element, boolean all) {
		WEnvironment env = WApplication.getInstance().getEnvironment();
		if (!env.agentIsIE()
				|| !(((this) instanceof WAbstractToggleButton ? (WAbstractToggleButton) (this)
						: null) != null)) {
			EventSignal s = this.voidEventSignal(CHANGE_SIGNAL, false);
			if (s != null) {
				this.updateSignalConnection(element, s, "change", all);
			}
		}
		if (this.flags_.get(BIT_ENABLED_CHANGED) || all) {
			element.setProperty(Property.PropertyDisabled,
					this.isEnabled() ? "false" : "true");
			this.flags_.clear(BIT_ENABLED_CHANGED);
		}
		if (this.flags_.get(BIT_READONLY_CHANGED) || all) {
			element.setProperty(Property.PropertyReadOnly,
					this.isReadOnly() ? "true" : "false");
			this.flags_.clear(BIT_READONLY_CHANGED);
		}
		if (this.isEnabled()) {
			if (all && this.flags_.get(BIT_GOT_FOCUS)) {
				this.flags_.set(BIT_INITIAL_FOCUS);
			}
			if (this.flags_.get(BIT_GOT_FOCUS) || all
					&& this.flags_.get(BIT_INITIAL_FOCUS)) {
				element.callMethod("focus()");
				this.flags_.clear(BIT_GOT_FOCUS);
			}
		}
		super.updateDom(element, all);
	}

	protected void propagateRenderOk(boolean deep) {
		this.flags_.clear(BIT_ENABLED_CHANGED);
		super.propagateRenderOk(deep);
	}

	// protected AbstractEventSignal.LearningListener
	// getStateless(<pointertomember or dependentsizedarray>
	// methodpointertomember or dependentsizedarray>) ;
	protected static String CHANGE_SIGNAL = "M_change";
	private static String SELECT_SIGNAL = "select";
	private static String FOCUS_SIGNAL = "focus";
	private static String BLUR_SIGNAL = "blur";
}