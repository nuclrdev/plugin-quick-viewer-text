package dev.nuclr.plugin.core.quick.viewer.text;

import java.awt.Color;
import java.awt.Font;
import java.util.Locale;

import javax.swing.UIManager;

/** Small shared helpers for the quick-view chrome (message card, banner). */
final class ViewerUi {

	/** Amber that stays legible on both light and dark backgrounds. */
	static final Color ACCENT = new Color(0xE0, 0xA8, 0x3C);

	private ViewerUi() {
	}

	/** Formats a byte count the way a file manager would: 1.4 KB, 42.3 MB, 1.2 GB. */
	static String humanSize(long bytes) {
		if (bytes < 1024) return bytes + " B";
		String[] units = { "KB", "MB", "GB", "TB" };
		double value = bytes;
		int unit = -1;
		while (value >= 1024 && unit < units.length - 1) {
			value /= 1024;
			unit++;
		}
		return String.format(Locale.ROOT, value < 10 ? "%.1f %s" : "%.0f %s", value, units[unit]);
	}

	static String count(long value) {
		return String.format(Locale.ROOT, "%,d", value);
	}

	/** Keeps the head and tail of a path-like string: {@code averyl...ame.log}. */
	static String ellipsizeMiddle(String text, int max) {
		if (text == null) return "";
		if (text.length() <= max) return text;
		int head = (max - 1) / 2;
		int tail = max - 1 - head;
		return text.substring(0, head) + "…" + text.substring(text.length() - tail);
	}

	static String ellipsizeEnd(String text, int max) {
		if (text == null) return "";
		return text.length() <= max ? text : text.substring(0, max - 1) + "…";
	}

	/**
	 * First installed font able to render every one of {@code glyphs}, or
	 * {@code null} when the platform has none (common on bare Linux installs).
	 */
	static Font emojiFont(int size, String... glyphs) {
		String[] candidates = { "Segoe UI Emoji", "Apple Color Emoji", "Noto Color Emoji", "Noto Emoji", "Symbola" };
		for (String name : candidates) {
			Font font = new Font(name, Font.PLAIN, size);
			if (!font.getFamily().equalsIgnoreCase(name)) continue;
			boolean displaysAll = true;
			for (String glyph : glyphs) {
				if (font.canDisplayUpTo(glyph) != -1) {
					displaysAll = false;
					break;
				}
			}
			if (displaysAll) return font;
		}
		return null;
	}

	static Color defaultColor(String key, Color fallback) {
		Color color = UIManager.getColor(key);
		return color != null ? color : fallback;
	}

	static Color blend(Color base, Color overlay, float overlayWeight) {
		float clamped = Math.max(0f, Math.min(1f, overlayWeight));
		float baseWeight = 1f - clamped;
		return new Color(
				Math.round(base.getRed() * baseWeight + overlay.getRed() * clamped),
				Math.round(base.getGreen() * baseWeight + overlay.getGreen() * clamped),
				Math.round(base.getBlue() * baseWeight + overlay.getBlue() * clamped));
	}

}
