package dev.nuclr.plugin.core.quick.viewer.text;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.UIManager;

/**
 * Centered "nothing to show here" card, used instead of dropping a bare sentence
 * into the syntax text area where it reads like file content.
 */
class MessagePanel extends JPanel {

	private final MessageGlyph glyph = new MessageGlyph(MessageGlyph.Kind.TOO_LARGE, 64);
	private final JLabel title = new JLabel("", SwingConstants.CENTER);
	private final JLabel subtitle = new JLabel("", SwingConstants.CENTER);
	private final JLabel badge = new JLabel("", SwingConstants.CENTER) {
		@Override
		public Dimension getMinimumSize() {
			// JLabel reports its preferred size as the minimum, which makes the pill
			// overflow a narrow pane. Allow it to shrink so it ellipsizes instead.
			return new Dimension(24, super.getMinimumSize().height);
		}
	};
	private final JLabel hint = new JLabel("", SwingConstants.CENTER);
	private final RoundedPanel card = new RoundedPanel(18);
	private final RoundedPanel chip = new RoundedPanel(999);
	private final Component glyphGap = Box.createVerticalStrut(18);
	private final Component chipGap = Box.createVerticalStrut(16);
	private final Component hintGap = Box.createVerticalStrut(14);

	private boolean wantChip;
	private boolean wantHint;

	MessagePanel() {
		// Laid out by hand in doLayout() so the card can shrink with the pane.
		super(null);

		card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
		card.setBorder(BorderFactory.createEmptyBorder(32, 40, 32, 40));

		chip.setLayout(new BoxLayout(chip, BoxLayout.X_AXIS));
		chip.setBorder(BorderFactory.createEmptyBorder(5, 14, 5, 14));
		chip.add(badge);

		for (JComponent c : new JComponent[] { glyph, title, subtitle, chip, hint }) {
			c.setAlignmentX(Component.CENTER_ALIGNMENT);
		}

		card.add(glyph);
		card.add(glyphGap);
		card.add(title);
		card.add(Box.createVerticalStrut(8));
		card.add(subtitle);
		card.add(chipGap);
		card.add(chip);
		card.add(hintGap);
		card.add(hint);

		add(card);

		// Sensible defaults so the card is readable even before the host pushes a theme.
		applyTheme(ViewerUi.defaultColor("Panel.background", Color.DARK_GRAY),
				ViewerUi.defaultColor("Panel.foreground", Color.WHITE),
				UIManager.getFont("defaultFont"));
	}

	/**
	 * @param badgeText a short stat line (e.g. size vs. limit), or {@code null}
	 * @param hintText  what the user can do next, or {@code null}
	 */
	void show(MessageGlyph.Kind kind, String titleText, String subtitleText, String badgeText, String hintText) {
		glyph.setKind(kind);
		title.setText(titleText);
		// Labels don't wrap, so a long name or message would push the card past the
		// pane: shorten them here and keep the full text in the tooltip.
		subtitle.setText(ViewerUi.ellipsizeMiddle(subtitleText, 48));
		subtitle.setToolTipText(subtitleText);
		badge.setText(badgeText == null ? "" : badgeText);
		hint.setText(ViewerUi.ellipsizeEnd(hintText, 110));
		hint.setToolTipText(hintText);
		wantChip = badgeText != null && !badgeText.isBlank();
		wantHint = hintText != null && !hintText.isBlank();
		revalidate();
		repaint();
	}

	/**
	 * Recolors the card from the host's palette. {@code foreground} carries the
	 * text color; the muted tones are blended toward the background so the card
	 * sits quietly on light and dark themes alike.
	 */
	void applyTheme(Color background, Color foreground, Font baseFont) {
		setBackground(background);
		card.setColors(ViewerUi.blend(background, foreground, 0.05f),
				ViewerUi.blend(background, foreground, 0.16f));
		chip.setColors(ViewerUi.blend(background, foreground, 0.10f),
				ViewerUi.blend(background, foreground, 0.18f));

		Font base = baseFont != null ? baseFont : getFont();
		float size = base.getSize2D();
		title.setFont(base.deriveFont(Font.BOLD, size + 5f));
		subtitle.setFont(base.deriveFont(Font.PLAIN, size + 1f));
		badge.setFont(base.deriveFont(Font.BOLD, Math.max(10f, size - 1f)));
		hint.setFont(base.deriveFont(Font.PLAIN, Math.max(10f, size - 1f)));

		title.setForeground(foreground);
		subtitle.setForeground(ViewerUi.blend(background, foreground, 0.72f));
		badge.setForeground(ViewerUi.blend(background, foreground, 0.85f));
		hint.setForeground(ViewerUi.blend(background, foreground, 0.55f));

		glyph.setGlyphSize(Math.round(size * 4.9f));
		revalidate();
		repaint();
	}

	/**
	 * Centers the card and clamps it to the pane. Quick view is often short or
	 * narrow, so when the card doesn't fit vertically the least important rows are
	 * dropped one at a time (hint, then the stat chip, then the glyph) rather than
	 * clipping the message.
	 */
	@Override
	public void doLayout() {
		int availWidth = Math.max(0, getWidth() - 24);
		int availHeight = Math.max(0, getHeight() - 24);

		setRowVisible(hint, hintGap, wantHint);
		setRowVisible(chip, chipGap, wantChip);
		setRowVisible(glyph, glyphGap, true);

		Dimension pref = card.getPreferredSize();
		if (pref.height > availHeight && hint.isVisible()) {
			setRowVisible(hint, hintGap, false);
			pref = card.getPreferredSize();
		}
		if (pref.height > availHeight && chip.isVisible()) {
			setRowVisible(chip, chipGap, false);
			pref = card.getPreferredSize();
		}
		if (pref.height > availHeight && glyph.isVisible()) {
			setRowVisible(glyph, glyphGap, false);
			pref = card.getPreferredSize();
		}

		// Labels ellipsize themselves once given less than their preferred width,
		// so clamping the card is enough to keep the text inside the pane.
		int width = Math.min(pref.width, availWidth);
		int height = Math.min(pref.height, availHeight);
		card.setBounds((getWidth() - width) / 2, (getHeight() - height) / 2, width, height);
	}

	private static void setRowVisible(JComponent row, Component gap, boolean visible) {
		row.setVisible(visible);
		gap.setVisible(visible);
	}

}
