package dev.nuclr.plugin.core.quick.viewer.text;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;

import javax.swing.JPanel;

/** Filled, softly outlined rounded rectangle used for cards, chips and banners. */
class RoundedPanel extends JPanel {

	private final int arc;
	private Color fill;
	private Color border;

	RoundedPanel(int arc) {
		this.arc = arc;
		setOpaque(false);
	}

	void setColors(Color fill, Color border) {
		this.fill = fill;
		this.border = border;
		repaint();
	}

	@Override
	protected void paintComponent(Graphics g) {
		if (fill == null) {
			super.paintComponent(g);
			return;
		}
		Graphics2D g2 = (Graphics2D) g.create();
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		int radius = Math.min(arc, Math.min(getWidth(), getHeight()));
		var shape = new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1f, getHeight() - 1f, radius, radius);
		g2.setColor(fill);
		g2.fill(shape);
		if (border != null) {
			g2.setColor(border);
			g2.setStroke(new BasicStroke(1f));
			g2.draw(shape);
		}
		g2.dispose();
	}

}
