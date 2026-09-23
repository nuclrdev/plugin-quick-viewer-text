package dev.nuclr.plugin.core.quick.viewer.text;

import java.awt.image.BufferedImage;
import java.util.List;
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
 * Priority 50 ensures that specialised providers (image, PDF, ...) with lower
 * priority numbers are tried first when multiple providers could match the same
 * extension.
 */
@Slf4j
public class TextQuickViewProvider implements QuickViewNuclrPlugin {

	/** Read for a thumbnail: far more than a page has room to show. */
	private static final int THUMBNAIL_BYTES = 16 * 1024;
	private static final int THUMBNAIL_LINES = 150;

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
	public boolean supports(NuclrResource resource) {
		return TextFileSupport.supports(resource);
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
	public boolean supportsThumbnails() {
		return true;
	}

	/** The head of the file, as a page of monospaced text. */
	@Override
	public BufferedImage thumbnail(NuclrResource resource, int maxWidth, int maxHeight, AtomicBoolean cancelled) {
		if (maxWidth <= 0 || maxHeight <= 0 || !supports(resource)) {
			return null;
		}
		try {
			String text = TextFileSupport.head(resource, THUMBNAIL_BYTES).text();
			// Same test the panel applies: a NUL byte means binary, which is not ours to draw.
			if (text.isBlank() || text.indexOf('\0') >= 0 || (cancelled != null && cancelled.get())) {
				return null;
			}
			List<PageThumbnail.Line> lines = text.lines()
					.limit(THUMBNAIL_LINES)
					.map(PageThumbnail.Line::mono)
					.toList();
			return PageThumbnail.render(lines, maxWidth, maxHeight, cancelled);
		} catch (Exception e) {
			log.debug("No thumbnail for {}: {}", resource.getName(), e.toString());
			return null;
		}
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

	private String id = "dev.nuclr.plugin.core.quickviewer.text";


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
		return id;
	}

	@Override
	public String getWindowTitle() {
		return "Quick View: " + (currentResource != null ? currentResource.getName() : "");
	}
	
	


}
