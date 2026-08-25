package dev.nuclr.plugin.core.quick.viewer.text;

import java.awt.BasicStroke;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;

import javax.swing.JComponent;

/**
 * The leading glyph of a message card or banner: the emoji where the platform
 * has a font for it, otherwise an equivalent shape drawn with Java2D, so it
 * never degrades into a missing-glyph box.
 */
class MessageGlyph extends JComponent {

	/** Which glyph to draw. */
	enum Kind {
		/** Document with a folded corner - the file is readable, just oversized. */
		TOO_LARGE("📄"),
		/** Warning triangle - the file could not be read at all. */
		ERROR("⚠");

		private final String emoji;

		Kind(String emoji) {
			this.emoji = emoji;
		}
	}

	/** Proportions below are expressed against this box, then scaled. */
	private static final float DESIGN_SIZE = 64f;

	private Kind kind;
	private Font emojiFont;

	MessageGlyph(Kind kind, int size) {
		this.kind = kind;
		setGlyphSize(size);
	}

	void setKind(Kind kind) {
		this.kind = kind;
		repaint();
	}

	/**
	 * Sizes the glyph in pixels. Callers derive this from the host font so the
	 * chrome scales with the UI rather than staying fixed on HiDPI displays.
	 */
	final void setGlyphSize(int size) {
		Dimension d = new Dimension(size, size);
		setPreferredSize(d);
		setMinimumSize(d);
		setMaximumSize(d);
		// ~0.72 keeps the emoji's ink about as large as the drawn fallback.
		emojiFont = ViewerUi.emojiFont(Math.max(8, Math.round(size * 0.72f)),
				Kind.TOO_LARGE.emoji, Kind.ERROR.emoji);
		revalidate();
		repaint();
	}

	@Override
	protected void paintComponent(Graphics g) {
		Graphics2D g2 = (Graphics2D) g.create();
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
		g2.setColor(ViewerUi.ACCENT);

		if (emojiFont != null) {
			g2.setFont(emojiFont);
			var metrics = g2.getFontMetrics();
			int x = (getWidth() - metrics.stringWidth(kind.emoji)) / 2;
			int y = (getHeight() - metrics.getHeight()) / 2 + metrics.getAscent();
			g2.drawString(kind.emoji, x, y);
		} else if (kind == Kind.ERROR) {
			paintWarning(g2, Math.min(getWidth(), getHeight()) / DESIGN_SIZE);
		} else {
			paintDocument(g2, Math.min(getWidth(), getHeight()) / DESIGN_SIZE);
		}
		g2.dispose();
	}

	/** Page with a folded corner and three text rules. */
	private void paintDocument(Graphics2D g2, float scale) {
		float w = 40f * scale, h = 52f * scale, fold = 13f * scale;
		float x = (getWidth() - w) / 2f, y = (getHeight() - h) / 2f;

		Path2D page = new Path2D.Float();
		page.moveTo(x, y);
		page.lineTo(x + w - fold, y);
		page.lineTo(x + w, y + fold);
		page.lineTo(x + w, y + h);
		page.lineTo(x, y + h);
		page.closePath();

		g2.setStroke(stroke(2.4f * scale));
		g2.draw(page);

		Path2D foldMark = new Path2D.Float();
		foldMark.moveTo(x + w - fold, y);
		foldMark.lineTo(x + w - fold, y + fold);
		foldMark.lineTo(x + w, y + fold);
		g2.draw(foldMark);

		g2.setStroke(stroke(2.2f * scale));
		for (int i = 0; i < 3; i++) {
			float lineY = y + h - 30f * scale + i * 9f * scale;
			g2.drawLine(Math.round(x + 9f * scale), Math.round(lineY),
					Math.round(x + w - (9f + i * 7f) * scale), Math.round(lineY));
		}
	}

	/** Triangle with an exclamation mark. */
	private void paintWarning(Graphics2D g2, float scale) {
		float w = 52f * scale, h = 46f * scale;
		float x = (getWidth() - w) / 2f, y = (getHeight() - h) / 2f;

		Path2D triangle = new Path2D.Float();
		triangle.moveTo(x + w / 2f, y);
		triangle.lineTo(x + w, y + h);
		triangle.lineTo(x, y + h);
		triangle.closePath();

		g2.setStroke(stroke(2.6f * scale));
		g2.draw(triangle);
		g2.setStroke(stroke(3f * scale));
		g2.drawLine(Math.round(x + w / 2f), Math.round(y + 16f * scale),
				Math.round(x + w / 2f), Math.round(y + h - 15f * scale));
		float dot = Math.max(2f, 4f * scale);
		g2.fillOval(Math.round(x + w / 2f - dot / 2f), Math.round(y + h - 10f * scale),
				Math.round(dot), Math.round(dot));
	}

	private static BasicStroke stroke(float width) {
		return new BasicStroke(Math.max(1f, width), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
	}

}
