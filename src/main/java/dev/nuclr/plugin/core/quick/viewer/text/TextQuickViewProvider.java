package dev.nuclr.plugin.core.quick.viewer.text;

import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.JComponent;

import dev.nuclr.platform.NuclrThemeScheme;
import dev.nuclr.platform.plugin.NuclrPluginContext;
import dev.nuclr.platform.plugin.NuclrResource;
import dev.nuclr.platform.plugin.QuickViewNuclrPlugin;
import lombok.extern.slf4j.Slf4j;

/**
 * Quick-view provider for plain text and source code files.
 *
 * <p>
 * Matches by file extension. Binary detection (null-byte scan) happens inside
 * {@link #openResource} so that {@link #supports} stays fast and
 * allocation-free.
 *
 * <p>
 * Priority 50 ensures that specialised providers (image, PDF, …) with lower
 * priority numbers are tried first when multiple providers could match the same
 * extension.
 */
@Slf4j
public class TextQuickViewProvider implements QuickViewNuclrPlugin {

	private NuclrPluginContext context;
	private TextQuickViewPanel panel;
	private volatile AtomicBoolean currentCancelled;
	private NuclrThemeScheme theme;
	private NuclrResource currentResource;

	@Override
	public JComponent panel() {
		if (panel == null) {
			panel = new TextQuickViewPanel();
			panel.applyTheme(theme);
		}
		return panel;
	}

	@Override
	public void preinit(NuclrPluginContext context) {
		this.context = context;
		applyTheme(context != null ? context.getTheme() : null);
	}

	@Override
	public void init() {
	}

	@Override
	public NuclrPluginContext getContext() {
		return this.context;
	}

	@Override
	public void unload() {
		closeResource();
		panel = null;
		context = null;
	}

	@Override
	public boolean supports(Path path) {
		return TextFileSupport.supports(path);
	}

	@Override
	public int priority() {
		return 1;
	}

	@Override
	public boolean openResource(NuclrResource resource, AtomicBoolean cancelled) {
		if (currentCancelled != null) {
			currentCancelled.set(true);
		}
		currentResource = resource;
		currentCancelled = cancelled;
		panel();
		return panel.load(resource, cancelled);
	}

	@Override
	public void closeResource() {
		if (currentCancelled != null) {
			currentCancelled.set(true);
			currentCancelled = null;
		}
		if (panel != null) {
			panel.clear();
		}
	}

	public void applyTheme(NuclrThemeScheme theme) {
		this.theme = theme;
		if (panel != null) {
			panel.applyTheme(theme);
		}
	}

	@Override
	public boolean onFocusGained() {
		// Quick view providers do not need focus-specific behavior.
		return false;
	}

	@Override
	public void onFocusLost() {
	}

	@Override
	public boolean isFocused() {
		return false;
	}

	private String name = "Text Quick Viewer";
	private String id = "dev.nuclr.plugin.core.quickviewer.text";
	private String version = "1.0.0";
	private String description = "Syntax-highlighted quick viewer for text and source code files.";
	private String author = "Nuclr Development Team";
	private String license = "Apache-2.0";
	private String website = "https://nuclr.dev";
	private String pageUrl = "https://nuclr.dev/plugins/core/text-quick-viewer.html";
	private String docUrl = "https://nuclr.dev/plugins/core/text-quick-viewer.html";

	@Override
	public String id() {
		return id;
	}

	@Override
	public String name() {
		return name;
	}

	@Override
	public String version() {
		return version;
	}

	@Override
	public String description() {
		return description;
	}

	@Override
	public String author() {
		return author;
	}

	@Override
	public String license() {
		return license;
	}

	@Override
	public String website() {
		return website;
	}

	@Override
	public String pageUrl() {
		return pageUrl;
	}

	@Override
	public String docUrl() {
		return docUrl;
	}

	@Override
	public Developer developer() {
		return Developer.Official;
	}

	@Override
	public void updateTheme(NuclrThemeScheme themeScheme) {
		applyTheme(themeScheme);
	}

	@Override
	public NuclrResource getCurrentResource() {
		return currentResource;
	}

	@Override
	public String uuid() {
		return id();
	}

}
