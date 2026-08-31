package dev.nuclr.plugin.core.quick.viewer.text;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;

import org.junit.jupiter.api.Test;

class ViewerUiTest {

	@Test
	void adjustsSelectionAccentThatIsTooCloseToTheBackground() {
		Color background = new Color(0x20, 0x22, 0x24);
		Color accent = new Color(0x29, 0x2C, 0x30);

		Color adjusted = ViewerUi.ensureSelectionContrast(background, accent);

		assertTrue(ViewerUi.contrastRatio(background, adjusted) >= 3.0);
	}

	@Test
	void keepsASelectionAccentThatAlreadyHasEnoughContrast() {
		Color background = new Color(0xFA, 0xFA, 0xFA);
		Color accent = new Color(0x24, 0x64, 0xA8);

		assertEquals(accent, ViewerUi.ensureSelectionContrast(background, accent));
	}

	@Test
	void replacesUnreadableSelectionTextColor() {
		Color selection = new Color(0x33, 0x66, 0x99);
		Color foreground = ViewerUi.readableSelectionForeground(selection,
				new Color(0x3A, 0x69, 0x98), new Color(0x40, 0x70, 0xA0));

		assertTrue(ViewerUi.contrastRatio(selection, foreground) >= 4.5);
	}
}
