package dev.nuclr.plugin.core.quick.viewer.text;

import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.JComponent;

import dev.nuclr.plugin.PluginTheme;
import dev.nuclr.plugin.QuickViewItem;
import dev.nuclr.plugin.QuickViewProvider;

/**
 * Quick-view provider for plain text and source code files.
 *
 * <p>Matches by file extension. Binary detection (null-byte scan) happens
 * inside {@link #open} so that {@link #matches} stays fast and allocation-free.
 *
 * <p>Priority 50 ensures that specialised providers (image, PDF, …) with
 * lower priority numbers are tried first when multiple providers could match
 * the same extension.
 */
public class TextQuickViewProvider implements QuickViewProvider {

	private TextQuickViewPanel panel;
	private PluginTheme theme;

	@Override
	public String getPluginClass() {
		return getClass().getName();
	}

	@Override
	public boolean matches(QuickViewItem item) {
		return TextFileSupport.matches(item.name()) || TextFileSupport.matchesExtension(item.extension());
	}

	@Override
	public JComponent getPanel() {
		if (panel == null) {
			panel = new TextQuickViewPanel();
			panel.applyTheme(theme);
		}
		return panel;
	}

	@Override
	public void applyTheme(PluginTheme theme) {
		this.theme = theme;
		if (panel != null) {
			panel.applyTheme(theme);
		}
	}

	@Override
	public boolean open(QuickViewItem item, AtomicBoolean cancelled) {
		getPanel(); // ensure panel is initialised
		return panel.load(item, cancelled);
	}

	@Override
	public void close() {
		if (panel != null) {
			panel.clear();
		}
	}

	@Override
	public void unload() {
		close();
		panel = null;
	}

	@Override
	public int priority() {
		return 1;
	}
}
