package dev.nuclr.plugin.core.quick.viewer.text;

import java.awt.BorderLayout;
import java.awt.Font;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;

import org.fife.ui.rsyntaxtextarea.RSyntaxDocument;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rsyntaxtextarea.Theme;
import org.fife.ui.rtextarea.RTextScrollPane;

import dev.nuclr.plugin.QuickViewItem;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TextQuickViewPanel extends JPanel {

	private static final long MAX_FILE_SIZE = 10L * 1024 * 1024; // 10 MB

	private final RSyntaxTextArea textArea;

	public TextQuickViewPanel() {
		super(new BorderLayout());

		textArea = new RSyntaxTextArea();

		try (InputStream themeIn = getClass()
				.getResourceAsStream("/org/fife/ui/rsyntaxtextarea/themes/dark.xml")) {
			if (themeIn != null) {
				Theme.load(themeIn).apply(textArea);
			}
		} catch (IOException e) {
			log.warn("Could not load RSyntaxTextArea dark theme", e);
		}

		textArea.setFont(new Font("JetBrains Mono", Font.PLAIN, 13));
		textArea.setCodeFoldingEnabled(true);
		textArea.setAntiAliasingEnabled(true);
		textArea.setTabSize(4);
		textArea.setTabsEmulated(false);
		textArea.setEditable(false);

		var scroll = new RTextScrollPane(textArea);
		scroll.setLineNumbersEnabled(true);
		SwingUtilities.updateComponentTreeUI(scroll);

		add(scroll, BorderLayout.CENTER);
	}

	/**
	 * Loads {@code item} into the text area. Called from a background thread;
	 * all Swing updates are dispatched to the EDT.
	 *
	 * @return {@code true} if the item was handled (even if only to display an
	 *         error message), {@code false} if the item should not be shown here
	 *         (e.g. binary content detected)
	 */
	public boolean load(QuickViewItem item, AtomicBoolean cancelled) {
		if (item.sizeBytes() > MAX_FILE_SIZE) {
			log.warn("File too large for text quick view: {} ({} bytes)", item.name(), item.sizeBytes());
			showMessage(item.name(), "File is too large to display.", cancelled);
			return true;
		}

		if (isBinary(item)) {
			log.debug("Binary content detected, skipping text view: {}", item.name());
			return false;
		}

		if (cancelled.get()) return false;

		String content;
		try {
			Path path = item.path();
			if (path != null) {
				content = Files.readString(path, StandardCharsets.UTF_8);
			} else {
				try (var in = item.openStream()) {
					content = new String(in.readAllBytes(), StandardCharsets.UTF_8);
				}
			}
		} catch (Exception e) {
			log.error("Failed to read file: {}", item.name(), e);
			showMessage(item.name(), "Error reading file: " + e.getMessage(), cancelled);
			return true;
		}

		if (cancelled.get()) return false;

		final String text = content;
		SwingUtilities.invokeLater(() -> {
			if (!cancelled.get()) setText(item.name(), text);
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
	private static boolean isBinary(QuickViewItem item) {
		byte[] buf = new byte[8192];
		try (InputStream in = item.openStream()) {
			int read = in.read(buf);
			for (int i = 0; i < read; i++) {
				if (buf[i] == 0) return true;
			}
		} catch (Exception e) {
			// Unreadable → let the main read attempt fail with a proper error
		}
		return false;
	}

}
