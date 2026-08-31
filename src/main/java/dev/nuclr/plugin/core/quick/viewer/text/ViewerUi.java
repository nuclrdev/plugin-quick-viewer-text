package dev.nuclr.plugin.core.quick.viewer.text;

import java.awt.Color;
import java.awt.Font;
import java.util.Locale;

import javax.swing.UIManager;

/** Small shared helpers for the quick-view chrome (message card, banner). */
final class ViewerUi {

	/** Amber that stays legible on both light and dark backgrounds. */
	static final Color ACCENT = new Color(0xE0, 0xA8, 0x3C);
	private static final double MIN_SELECTION_BACKGROUND_CONTRAST = 3.0;
	private static final double MIN_SELECTION_TEXT_CONTRAST = 4.5;

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

	/** Keeps the theme accent, adjusting it only when it is too close to the editor background. */
	static Color ensureSelectionContrast(Color background, Color accent) {
		if (contrastRatio(background, accent) >= MIN_SELECTION_BACKGROUND_CONTRAST) {
			return accent;
		}

		Color target = contrastRatio(background, Color.BLACK) >= contrastRatio(background, Color.WHITE)
				? Color.BLACK
				: Color.WHITE;
		float low = 0f;
		float high = 1f;
		for (int i = 0; i < 12; i++) {
			float weight = (low + high) / 2f;
			if (contrastRatio(background, blend(accent, target, weight))
					>= MIN_SELECTION_BACKGROUND_CONTRAST) {
				high = weight;
			} else {
				low = weight;
			}
		}
		return blend(accent, target, high);
	}

	static Color readableSelectionForeground(Color background, Color preferred, Color fallback) {
		if (contrastRatio(background, preferred) >= MIN_SELECTION_TEXT_CONTRAST) {
			return preferred;
		}
		if (contrastRatio(background, fallback) >= MIN_SELECTION_TEXT_CONTRAST) {
			return fallback;
		}
		return contrastRatio(background, Color.BLACK) >= contrastRatio(background, Color.WHITE)
				? Color.BLACK
				: Color.WHITE;
	}

	static double contrastRatio(Color first, Color second) {
		double lighter = Math.max(relativeLuminance(first), relativeLuminance(second));
		double darker = Math.min(relativeLuminance(first), relativeLuminance(second));
		return (lighter + 0.05) / (darker + 0.05);
	}

	private static double relativeLuminance(Color color) {
		return 0.2126 * linearChannel(color.getRed())
				+ 0.7152 * linearChannel(color.getGreen())
				+ 0.0722 * linearChannel(color.getBlue());
	}

	private static double linearChannel(int channel) {
		double value = channel / 255.0;
		return value <= 0.04045 ? value / 12.92 : Math.pow((value + 0.055) / 1.055, 2.4);
	}

}
