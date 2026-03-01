package dev.nuclr.plugin.core.quick.viewer.text;

import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.JComponent;

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

	static final Set<String> TEXT_EXTENSIONS = Set.of(
			// Plain text / markup
			"txt", "md", "log", "csv",
			"html", "htm", "css", "xml", "svg", "jsp",
			// Data / config
			"json", "yaml", "yml", "toml",
			"ini", "conf", "cfg", "properties", "prefs", "pref",
			// IDE / project files
			"classpath", "project", "factorypath",
			"csproj", "vsconfig", "firebaserc",
			// Web / scripting
			"js", "mjs", "ts", "tsx",
			// System languages
			"java", "py", "rb",
			"c", "cpp", "h", "hpp",
			"cs", "go", "rs", "php",
			"kt", "scala", "groovy", "gradle",
			"lua", "perl", "pl", "dart", "sql",
			//crypto
			"pub", "ppk",
			// Shells / scripts
			"sh", "bash", "bat", "cmd", "ps1",
			// Dotfiles (PathQuickViewItem returns e.g. "gitignore" for ".gitignore")
			"gitignore", "gitattributes", "meta", "dockerfile");

	private TextQuickViewPanel panel;

	@Override
	public String getPluginClass() {
		return getClass().getName();
	}

	@Override
	public boolean matches(QuickViewItem item) {
		return TEXT_EXTENSIONS.contains(item.extension().toLowerCase());
	}

	@Override
	public JComponent getPanel() {
		if (panel == null) {
			panel = new TextQuickViewPanel();
		}
		return panel;
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
