package dev.nuclr.plugin.core.quick.viewer.text;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import dev.nuclr.platform.plugin.NuclrResource;

class TextFileSupportTest {

	@Test
	void supportsExtensionlessUtf8Text(@TempDir Path tempDir) throws Exception {
		Path file = tempDir.resolve("WHATSNEW");
		Files.writeString(file, "Nuclr Commander\n\n- Added quick view improvements\n");

		assertTrue(TextFileSupport.supports(resource(file)));
	}

	@Test
	void supportsShebangScriptWithoutExtension(@TempDir Path tempDir) throws Exception {
		Path file = tempDir.resolve("deploy");
		Files.writeString(file, "#!/bin/sh\necho hello\n");

		assertTrue(TextFileSupport.supports(resource(file)));
	}

	@Test
	void rejectsKnownBinarySignature(@TempDir Path tempDir) throws Exception {
		Path file = tempDir.resolve("archive");
		Files.write(file, new byte[] { 'P', 'K', 3, 4, 20, 0 });

		assertFalse(TextFileSupport.supports(resource(file)));
	}

	@Test
	void supportsAStreamOnlyResourceSelectedByName() {
		// An S3 or GCS object: no local file, name and stream only.
		assertTrue(TextFileSupport.supports(streamOnly("notes.txt", "hello\n")));
	}

	@Test
	void rejectsAStreamOnlyResourceWithoutReadingIt() {
		// Sniffing a remote resource can mean downloading it in full, so an extension-less
		// name is rejected outright rather than opened.
		var opened = new java.util.concurrent.atomic.AtomicBoolean();
		NuclrResource resource = new NuclrResource(null) {
			@Override
			public java.io.InputStream openInputStream(java.nio.file.OpenOption... options) {
				opened.set(true);
				return new java.io.ByteArrayInputStream("plain text\n".getBytes(java.nio.charset.StandardCharsets.UTF_8));
			}
		};
		resource.setName("release-notes");

		assertFalse(TextFileSupport.supports(resource));
		assertFalse(opened.get(), "a stream-only resource must not be opened during selection");
	}

	@Test
	void rejectsSvgByNameWhenThereIsNoPath() {
		assertFalse(TextFileSupport.supports(streamOnly("logo.svg", "<svg/>")));
	}

	@Test
	void rejectsFoldersAndUnreadableResources() {
		var folder = streamOnly("logs.txt", "");
		folder.setFolder(true);
		assertFalse(TextFileSupport.supports(folder));

		var unreadable = streamOnly("secret.txt", "");
		unreadable.setReadable(false);
		assertFalse(TextFileSupport.supports(unreadable));
	}

	@Test
	void headCutsAtTheLastCompleteLine(@TempDir Path tempDir) throws Exception {
		Path file = tempDir.resolve("big.log");
		Files.writeString(file, "alpha\nbravo\ncharlie\ndelta\n");

		// 14 bytes lands in the middle of "charlie".
		var snippet = TextFileSupport.head(resource(file), 14);

		assertEquals("alpha\nbravo\n", snippet.text());
		assertEquals(2, snippet.lines());
		assertEquals(12, snippet.bytes());
	}

	@Test
	void headKeepsTheWholeFileWhenItFits(@TempDir Path tempDir) throws Exception {
		Path file = tempDir.resolve("small.log");
		Files.writeString(file, "alpha\nbravo\n");

		var snippet = TextFileSupport.head(resource(file), 4096);

		assertEquals("alpha\nbravo\n", snippet.text());
		assertEquals(2, snippet.lines());
	}

	@Test
	void headKeepsAPartialLineWhenThereIsNoLineBreak(@TempDir Path tempDir) throws Exception {
		Path file = tempDir.resolve("oneline.json");
		Files.writeString(file, "{\"a\":1,\"b\":2,\"c\":3}");

		var snippet = TextFileSupport.head(resource(file), 8);

		// Cutting at a line break would leave nothing to show for a single-line file.
		assertEquals("{\"a\":1,\"", snippet.text());
		assertEquals(1, snippet.lines());
	}

	@Test
	void headDropsACharacterSplitByTheCut(@TempDir Path tempDir) throws Exception {
		Path file = tempDir.resolve("utf8.txt");
		Files.writeString(file, "héllo");

		// 2 bytes: 'h' plus the first half of the two-byte 'é'.
		var snippet = TextFileSupport.head(resource(file), 2);

		assertEquals("h", snippet.text());
	}

	@Test
	void headOfAnEmptyFileIsEmpty(@TempDir Path tempDir) throws Exception {
		Path file = tempDir.resolve("empty.txt");
		Files.writeString(file, "");

		var snippet = TextFileSupport.head(resource(file), 4096);

		assertEquals("", snippet.text());
		assertEquals(0, snippet.lines());
		assertEquals(0, snippet.bytes());
	}

	/** A resource with no local file, exactly as a remote panel supplies it. */
	private static NuclrResource streamOnly(String name, String content) {
		NuclrResource resource = new NuclrResource(null) {
			@Override
			public java.io.InputStream openInputStream(java.nio.file.OpenOption... options) {
				return new java.io.ByteArrayInputStream(content.getBytes(java.nio.charset.StandardCharsets.UTF_8));
			}
		};
		resource.setName(name);
		return resource;
	}

	private static NuclrResource resource(Path path) {
		NuclrResource resource = new NuclrResource(path) {
			@Override
			public java.io.InputStream openInputStream(java.nio.file.OpenOption... options) throws Exception {
				return Files.newInputStream(getPath(), options);
			}
		};
		resource.setName(path.getFileName().toString());
		return resource;
	}
}
