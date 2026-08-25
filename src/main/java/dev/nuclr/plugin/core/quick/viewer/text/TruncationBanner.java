package dev.nuclr.plugin.core.quick.viewer.text;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Insets;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * Strip shown above the text area when only the head of a file is being
 * previewed. Deliberately compact: the snippet below it is the point, so the
 * banner only has to say what is missing and why.
 */
class TruncationBanner extends JPanel {

	private final MessageGlyph glyph = new MessageGlyph(MessageGlyph.Kind.TOO_LARGE, 20);
	private final JLabel title = new JLabel("Preview truncated");
	private final JLabel detail = new JLabel();
	private final RoundedPanel strip = new RoundedPanel(12);
	private static final int STRIP_GAP = 10;
	private static final int TEXT_GAP = 8;

	private String[] variants = {};

	TruncationBanner() {
		super(new BorderLayout());
		setBorder(BorderFactory.createEmptyBorder(8, 8, 4, 8));

		JPanel text = new JPanel(new BorderLayout(TEXT_GAP, 0));
		text.setOpaque(false);
		text.add(title, BorderLayout.WEST);
		// The detail sits in CENTER so it - and not the title - gives up room and
		// ellipsizes when the quick-view pane is narrow.
		text.add(detail, BorderLayout.CENTER);

		strip.setLayout(new BorderLayout(STRIP_GAP, 0));
		strip.setBorder(BorderFactory.createEmptyBorder(7, 12, 7, 12));
		strip.add(glyph, BorderLayout.WEST);
		strip.add(text, BorderLayout.CENTER);

		add(strip, BorderLayout.CENTER);
	}

	/**
	 * Sets the detail text, longest variant first. The widest one that fits the
	 * pane is shown, so a narrow quick view loses the trailing advice instead of
	 * having the file sizes cut off mid-word.
	 */
	void setDetail(String... variants) {
		this.variants = variants.clone();
		detail.setToolTipText(variants[0]);
		detail.setText(variants[0]);
		revalidate();
	}

	@Override
	public void doLayout() {
		if (variants.length > 0) detail.setText(widestThatFits());
		super.doLayout();
	}

	/**
	 * Picks a variant against the width the detail label will actually get: what
	 * is left after the padding, the glyph and the (fixed) title.
	 */
	private String widestThatFits() {
		Insets outer = getInsets();
		Insets inner = strip.getInsets();
		int available = getWidth()
				- outer.left - outer.right
				- inner.left - inner.right
				- glyph.getPreferredSize().width - STRIP_GAP
				- title.getPreferredSize().width - TEXT_GAP;

		var metrics = detail.getFontMetrics(detail.getFont());
		for (String variant : variants) {
			if (metrics.stringWidth(variant) <= available) return variant;
		}
		// Nothing fits; the shortest still reads best once the label ellipsizes it.
		return variants[variants.length - 1];
	}

	/** Tints the strip toward the accent so it reads as chrome, not as content. */
	void applyTheme(Color background, Color foreground, Font baseFont) {
		setBackground(background);
		strip.setColors(ViewerUi.blend(background, ViewerUi.ACCENT, 0.13f),
				ViewerUi.blend(background, ViewerUi.ACCENT, 0.40f));

		Font base = baseFont != null ? baseFont : getFont();
		float size = base.getSize2D();
		title.setFont(base.deriveFont(Font.BOLD, size));
		detail.setFont(base.deriveFont(Font.PLAIN, size));
		title.setForeground(foreground);
		detail.setForeground(ViewerUi.blend(background, foreground, 0.68f));
		glyph.setGlyphSize(Math.round(size * 1.5f));
	}

}
