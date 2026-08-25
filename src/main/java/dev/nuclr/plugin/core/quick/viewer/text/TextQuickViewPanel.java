package dev.nuclr.plugin.core.quick.viewer.text;

import java.awt.BorderLayout;
import java.awt.CardLayout;
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

	/** Files up to this size are previewed whole; bigger ones get a head snippet. */
	private static final long MAX_FILE_SIZE = 10L * 1024 * 1024; // 10 MB
	/** How much of an oversized file to read for that snippet. */
	private static final int SNIPPET_BYTES = 256 * 1024;
	private static final String CARD_TEXT = "text";
	private static final String CARD_MESSAGE = "message";

	private final RSyntaxTextArea textArea;
	private final RTextScrollPane scroll;
	private final MessagePanel message = new MessagePanel();
	private final TruncationBanner banner = new TruncationBanner();
	private final JPanel textCard = new JPanel(new BorderLayout());
	private final CardLayout cards = new CardLayout();
	private final JPanel deck = new JPanel(cards);

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

		banner.setVisible(false);
		textCard.add(banner, BorderLayout.NORTH);
		textCard.add(scroll, BorderLayout.CENTER);

		deck.add(textCard, CARD_TEXT);
		deck.add(message, CARD_MESSAGE);
		add(deck, BorderLayout.CENTER);
	}

	public void applyTheme(NuclrThemeScheme theme) {
		// Colors come from the active look-and-feel (UIManager): the host installs
		// the selected theme as the L&F and only overlays a scheme's *explicit*
		// overrides on top. Built-in themes carry no color overrides, so reading
		// solely from the scheme would yield stale defaults (e.g. black line
		// numbers, a selection indistinguishable from the panel background).
		Color background = uiColor(theme, "Panel.background", getBackground());
		Color foreground = uiColor(theme, "Panel.foreground", textArea.getForeground());

		// Load the bundled RSyntax palette whose token colors match the host
		// background luminance, so highlighting stays readable on light themes.
		// The overrides below then nudge it to the exact host colors.
		loadSyntaxTheme(isDark(background));

		textArea.setFont(editorFont(UIManager.getFont("defaultFont")));
		scroll.getGutter().setLineNumberFont(textArea.getFont());

		Color gutterBackground = uiColor(theme, "TableHeader.background", background);
		Color gutterForeground = uiColor(theme, "Label.foreground", foreground);
		// Use the host's real text-selection color so the selection is clearly
		// distinct from the panel background and tracks light/dark themes.
		Color selectionBackground = uiColor(theme, "TextArea.selectionBackground",
				uiColor(theme, "Table.selectionBackground", textArea.getSelectionColor()));

		setBackground(background);
		deck.setBackground(background);
		textCard.setBackground(background);
		message.applyTheme(background, foreground, textArea.getFont());
		banner.applyTheme(background, foreground, textArea.getFont());
		scroll.setBackground(background);
		scroll.getViewport().setBackground(background);
		scroll.getGutter().setBackground(gutterBackground);
		scroll.getGutter().setLineNumberColor(gutterForeground);

		textArea.setBackground(background);
		textArea.setForeground(foreground);
		textArea.setCaretColor(foreground);
		textArea.setSelectionColor(selectionBackground);
		textArea.setSelectedTextColor(foreground);
		textArea.setCurrentLineHighlightColor(ViewerUi.blend(background, uiColor(theme, "Table.gridColor", gutterBackground), 0.35f));
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
		// Checked before the size test so an oversized *binary* still falls through
		// to a viewer that can handle it, instead of being claimed by this one.
		if (isBinary(item)) {
			log.debug("Binary content detected, skipping text view: {}", item.getName());
			return false;
		}

		if (cancelled.get()) return false;

		if (item.getLength() > MAX_FILE_SIZE) {
			return loadSnippet(item, cancelled);
		}

		String content;
		try (var in = item.openInputStream();
				var reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
			content = readAll(reader, cancelled);
		} catch (Exception e) {
			log.error("Failed to read file: {}", item.getName(), e);
			showMessage(MessageGlyph.Kind.ERROR,
					"Couldn’t read this file",
					item.getName(),
					null,
					e.getMessage(),
					cancelled);
			return true;
		}

		if (cancelled.get()) return false;

		final String text = content;
		SwingUtilities.invokeLater(() -> {
			if (!cancelled.get()) setText(item.getName(), text, null);
		});
		return true;
	}

	/**
	 * Previews the head of a file that is too big to load whole. Reading a fixed
	 * slice keeps the cost flat however large the file is, and a banner above the
	 * text says how much of it is on screen.
	 */
	private boolean loadSnippet(NuclrResource item, AtomicBoolean cancelled) {
		log.debug("Over the full-preview limit, showing a snippet: {} ({} bytes)",
				item.getName(), item.getLength());

		TextFileSupport.Snippet snippet;
		try {
			snippet = TextFileSupport.head(item, SNIPPET_BYTES);
		} catch (Exception e) {
			log.error("Failed to read the start of: {}", item.getName(), e);
			showMessage(MessageGlyph.Kind.ERROR, "Couldn’t read this file", item.getName(), null,
					e.getMessage(), cancelled);
			return true;
		}

		if (cancelled.get()) return false;

		if (snippet.text().isEmpty()) {
			// Nothing decodable in the head of the file: fall back to the card.
			showMessage(MessageGlyph.Kind.TOO_LARGE, "Too big to preview", item.getName(),
					ViewerUi.humanSize(item.getLength()) + "  ·  preview limit "
							+ ViewerUi.humanSize(MAX_FILE_SIZE),
					"Open the file in the editor to see all of it.", cancelled);
			return true;
		}

		String shown = "First " + ViewerUi.humanSize(snippet.bytes())
				+ " of " + ViewerUi.humanSize(item.getLength());
		String withLines = shown + "  ·  " + ViewerUi.count(snippet.lines()) + " lines";
		// Longest first: the banner drops down the list to fit the pane.
		String[] detail = {
				withLines + "  ·  open it in the editor to see all of it",
				withLines,
				shown };
		SwingUtilities.invokeLater(() -> {
			if (!cancelled.get()) setText(item.getName(), snippet.text(), detail);
		});
		return true;
	}

	public void clear() {
		SwingUtilities.invokeLater(() -> {
			textArea.setDocument(new RSyntaxDocument(SyntaxConstants.SYNTAX_STYLE_NONE));
			banner.setVisible(false);
			cards.show(deck, CARD_TEXT);
		});
	}

	// ── Internals ─────────────────────────────────────────────────────────────

	private void showMessage(MessageGlyph.Kind kind, String title, String subtitle, String badge, String hint,
			AtomicBoolean cancelled) {
		SwingUtilities.invokeLater(() -> {
			if (cancelled.get()) return;
			// Drop any previously shown content so a stale document isn't kept alive
			// (and doesn't flash back when the next file loads).
			textArea.setDocument(new RSyntaxDocument(SyntaxConstants.SYNTAX_STYLE_NONE));
			message.show(kind, title, subtitle, badge, hint);
			cards.show(deck, CARD_MESSAGE);
		});
	}

	/** @param truncationDetail banner text (longest variant first) when {@code text} is only a snippet, else {@code null} */
	private void setText(String filename, String text, String[] truncationDetail) {
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

		if (truncationDetail != null) banner.setDetail(truncationDetail);
		banner.setVisible(truncationDetail != null);
		textCard.revalidate();
		cards.show(deck, CARD_TEXT);
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

	/**
	 * Resolve a UI color, preferring the theme scheme's explicit override and
	 * otherwise falling back to the active look-and-feel ({@link UIManager}). A
	 * {@code fallback} guards keys the L&amp;F does not define.
	 */
	private static Color uiColor(NuclrThemeScheme theme, String key, Color fallback) {
		Color lafColor = UIManager.getColor(key);
		Color base = lafColor != null ? lafColor : fallback;
		return theme != null ? theme.color(key, base) : base;
	}

	/** Perceived-luminance test (ITU-R BT.601); below mid-grey counts as a dark background. */
	private static boolean isDark(Color c) {
		double luminance = 0.299 * c.getRed() + 0.587 * c.getGreen() + 0.114 * c.getBlue();
		return luminance < 128;
	}

	private static Font editorFont(Font baseFont) {
		return baseFont != null ? baseFont : new Font(Font.MONOSPACED, Font.PLAIN, 13);
	}

}
