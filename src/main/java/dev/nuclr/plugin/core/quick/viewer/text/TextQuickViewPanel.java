package dev.nuclr.plugin.core.quick.viewer.text;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.text.BadLocationException;

import org.fife.ui.rsyntaxtextarea.RSyntaxDocument;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rsyntaxtextarea.Theme;
import org.fife.ui.rtextarea.RTextScrollPane;

import dev.nuclr.platform.NuclrThemeScheme;
import dev.nuclr.platform.plugin.NuclrResource;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TextQuickViewPanel extends JPanel {

	private static final long MAX_FILE_SIZE = 10L * 1024 * 1024; // 10 MB
	private final RSyntaxTextArea textArea;
	private final RTextScrollPane scroll;

	public TextQuickViewPanel() {
		super(new BorderLayout());

		textArea = new RSyntaxTextArea();

		loadSyntaxTheme(true);

		textArea.setFont(editorFont(UIManager.getFont("defaultFont")));
		textArea.setCodeFoldingEnabled(true);
		textArea.setAntiAliasingEnabled(true);
		textArea.setTabSize(4);
		textArea.setTabsEmulated(false);
		textArea.setEditable(false);

		scroll = new RTextScrollPane(textArea);
		scroll.setLineNumbersEnabled(true);
		SwingUtilities.updateComponentTreeUI(scroll);

		add(scroll, BorderLayout.CENTER);
	}

	public void applyTheme(NuclrThemeScheme theme) {
		if (theme == null) {
			textArea.setFont(editorFont(UIManager.getFont("defaultFont")));
			scroll.getGutter().setLineNumberFont(textArea.getFont());
			return;
		}

		Color background = theme.color("Panel.background", getBackground());
		Color foreground = theme.color("Panel.foreground", textArea.getForeground());

		// Load the bundled RSyntax palette whose token colors match the host
		// background luminance, so highlighting stays readable on light themes.
		// The background overrides below then nudge it to the exact host color.
		loadSyntaxTheme(isDark(background));

		textArea.setFont(editorFont(theme.defaultFont()));
		scroll.getGutter().setLineNumberFont(textArea.getFont());
		Color accentSelection = theme.color("Table.selectionBackground", textArea.getSelectionColor());
		Color selectionBackground = blend(background, accentSelection, 0.26f);
		Color selectionForeground = foreground;
		Color gutterBackground = theme.color("TableHeader.background", background);
		Color gutterForeground = theme.color("Label.foreground", foreground);

		setBackground(background);
		scroll.setBackground(background);
		scroll.getViewport().setBackground(background);
		scroll.getGutter().setBackground(gutterBackground);
		scroll.getGutter().setLineNumberColor(gutterForeground);

		textArea.setBackground(background);
		textArea.setForeground(foreground);
		textArea.setCaretColor(foreground);
		textArea.setSelectionColor(selectionBackground);
		textArea.setSelectedTextColor(selectionForeground);
		textArea.setCurrentLineHighlightColor(blend(background, theme.color("Table.gridColor", gutterBackground), 0.35f));
	}

	/**
	 * Loads {@code item} into the text area. Called from a background thread;
	 * all Swing updates are dispatched to the EDT.
	 *
	 * @return {@code true} if the item was handled (even if only to display an
	 *         error message), {@code false} if the item should not be shown here
	 *         (e.g. binary content detected)
	 */
	public boolean load(NuclrResource item, AtomicBoolean cancelled) {
		if (item.getLength() > MAX_FILE_SIZE) {
			log.warn("File too large for text quick view: {} ({} bytes)", item.getName(), item.getLength());
			showMessage(item.getName(), "File is too large to display.", cancelled);
			return true;
		}

		if (isBinary(item)) {
			log.debug("Binary content detected, skipping text view: {}", item.getName());
			return false;
		}

		if (cancelled.get()) return false;

		String content;
		try (var in = item.openInputStream();
				var reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
			content = readAll(reader, cancelled);
		} catch (Exception e) {
			log.error("Failed to read file: {}", item.getName(), e);
			showMessage(item.getName(), "Error reading file: " + e.getMessage(), cancelled);
			return true;
		}

		if (cancelled.get()) return false;

		final String text = content;
		SwingUtilities.invokeLater(() -> {
			if (!cancelled.get()) setText(item.getName(), text);
		});
		return true;
	}

	public void clear() {
		SwingUtilities.invokeLater(() ->
				textArea.setDocument(new RSyntaxDocument(SyntaxConstants.SYNTAX_STYLE_NONE)));
	}

	// ── Internals ─────────────────────────────────────────────────────────────

	private void showMessage(String filename, String message, AtomicBoolean cancelled) {
		SwingUtilities.invokeLater(() -> {
			if (!cancelled.get()) setText(filename, message);
		});
	}

	private void setText(String filename, String text) {
		String style = TextFileSupport.syntaxStyle(filename);

		// Replace the document entirely so the old token-factory pool can be GC'd.
		// Text is inserted before setDocument() so no undo records are created.
		RSyntaxDocument doc = new RSyntaxDocument(style);
		try {
			doc.insertString(0, text, null);
		} catch (BadLocationException e) {
			log.error("Failed to build document for: {}", filename, e);
			return;
		}
		textArea.setDocument(doc);
		textArea.setSyntaxEditingStyle(style);
		textArea.setCaretPosition(0);
		textArea.discardAllEdits();
	}

	/**
	 * Scans the first 8 KB for null bytes. Null bytes reliably indicate binary
	 * content because they are not valid in any text encoding.
	 */
	private static boolean isBinary(NuclrResource item) {
		byte[] buf = new byte[8192];
		try (InputStream in = item.openInputStream()) {
			int read = in.read(buf);
			for (int i = 0; i < read; i++) {
				if (buf[i] == 0) return true;
			}
		} catch (Exception e) {
			// Unreadable → let the main read attempt fail with a proper error
		}
		return false;
	}

	private static String readAll(Reader reader, AtomicBoolean cancelled) throws IOException {
		StringBuilder sb = new StringBuilder();
		char[] buffer = new char[8192];
		int read;
		while ((read = reader.read(buffer)) != -1) {
			if (cancelled.get()) {
				break;
			}
			sb.append(buffer, 0, read);
		}
		return sb.toString();
	}

	private void loadSyntaxTheme(boolean dark) {
		String resource = dark
				? "/org/fife/ui/rsyntaxtextarea/themes/dark.xml"
				: "/org/fife/ui/rsyntaxtextarea/themes/default.xml";
		try (InputStream themeIn = getClass().getResourceAsStream(resource)) {
			if (themeIn != null) {
				Theme.load(themeIn).apply(textArea);
			}
		} catch (IOException e) {
			log.warn("Could not load RSyntaxTextArea theme: {}", resource, e);
		}
	}

	/** Perceived-luminance test (ITU-R BT.601); below mid-grey counts as a dark background. */
	private static boolean isDark(Color c) {
		double luminance = 0.299 * c.getRed() + 0.587 * c.getGreen() + 0.114 * c.getBlue();
		return luminance < 128;
	}

	private static Font editorFont(Font baseFont) {
		return baseFont != null ? baseFont : new Font(Font.MONOSPACED, Font.PLAIN, 13);
	}

	private static Color blend(Color base, Color overlay, float overlayWeight) {
		float clamped = Math.max(0f, Math.min(1f, overlayWeight));
		float baseWeight = 1f - clamped;
		return new Color(
				Math.round(base.getRed() * baseWeight + overlay.getRed() * clamped),
				Math.round(base.getGreen() * baseWeight + overlay.getGreen() * clamped),
				Math.round(base.getBlue() * baseWeight + overlay.getBlue() * clamped));
	}

}
