package dev.nuclr.plugin.core.quick.viewer.text;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.OpenOption;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;

import dev.nuclr.platform.plugin.NuclrResource;

class TextThumbnailTest {

	private final TextQuickViewProvider provider = new TextQuickViewProvider();

	@Test
	void drawsAPortraitPageWithinTheBoxBeforeInit() {
		assertTrue(provider.supportsThumbnails());

		BufferedImage image = provider.thumbnail(resource("Main.java", "class Main {\n\tint x;\n}\n"), 200, 200,
				new AtomicBoolean());

		assertNotNull(image);
		assertEquals(200, image.getHeight());
		assertTrue(image.getWidth() < image.getHeight());
	}

	@Test
	void drawsReadableAndIconSizedPagesAlike() {
		String text = "line of text\n".repeat(200);
		for (int size : List.of(24, 64, 512)) {
			BufferedImage image = provider.thumbnail(resource("notes.txt", text), size, size, new AtomicBoolean());
			assertNotNull(image, "size " + size);
			assertTrue(image.getWidth() <= size && image.getHeight() <= size, "size " + size);
		}
	}

	@Test
	void returnsNullForBinaryEmptyForeignOrCancelled() {
		assertNull(provider.thumbnail(resource("data.txt", "abc\u0000def"), 100, 100, new AtomicBoolean()));
		assertNull(provider.thumbnail(resource("empty.txt", ""), 100, 100, new AtomicBoolean()));
		assertNull(provider.thumbnail(resource("photo.png", "text"), 100, 100, new AtomicBoolean()));
		assertNull(provider.thumbnail(resource("notes.txt", "text"), 100, 100, new AtomicBoolean(true)));
		assertNull(provider.thumbnail(resource("notes.txt", "text"), 0, 100, new AtomicBoolean()));
	}

	private static NuclrResource resource(String name, String content) {
		byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
		NuclrResource resource = new NuclrResource(null) {
			private static final long serialVersionUID = 1L;

			@Override
			public InputStream openInputStream(OpenOption... options) {
				return new ByteArrayInputStream(bytes);
			}
		};
		resource.setUuid(name);
		resource.setName(name);
		resource.setLength(bytes.length);
		return resource;
	}
}
