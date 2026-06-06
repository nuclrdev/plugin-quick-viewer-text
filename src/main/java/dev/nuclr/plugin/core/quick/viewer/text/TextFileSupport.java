package dev.nuclr.plugin.core.quick.viewer.text;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

import org.fife.ui.rsyntaxtextarea.SyntaxConstants;

final class TextFileSupport {
	private static final int SAMPLE_SIZE = 8192;
	private static final double MIN_PRINTABLE_RATIO = 0.85d;
	private static final double MAX_WHITESPACE_RATIO = 0.60d;

	private static final Set<String> TEXT_EXTENSIONS = Set.of(
			// Plain text / markup
			"txt", "md", "log", "csv",
			"html", "htm", "css", "xml", "jsp",
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
			"mf",
			// Crypto
			"pub", "ppk", "pem",
			// Shells / scripts
			"sh", "bash", "bat", "cmd", "ps1");

	private static final Set<String> TEXT_FILENAMES = Set.of(
			"license",
			"whatsnew",
			"dockerfile", "mvnw",
			"gitignore", "gitattributes", "meta");

	private static final Map<String, String> SYNTAX_BY_EXTENSION = Map.ofEntries(
			Map.entry("java",        SyntaxConstants.SYNTAX_STYLE_JAVA),
			Map.entry("js",          SyntaxConstants.SYNTAX_STYLE_JAVASCRIPT),
			Map.entry("mjs",         SyntaxConstants.SYNTAX_STYLE_JAVASCRIPT),
			Map.entry("ts",          SyntaxConstants.SYNTAX_STYLE_TYPESCRIPT),
			Map.entry("tsx",         SyntaxConstants.SYNTAX_STYLE_TYPESCRIPT),
			Map.entry("json",        SyntaxConstants.SYNTAX_STYLE_JSON),
			Map.entry("xml",         SyntaxConstants.SYNTAX_STYLE_XML),
			Map.entry("classpath",   SyntaxConstants.SYNTAX_STYLE_XML),
			Map.entry("factorypath", SyntaxConstants.SYNTAX_STYLE_XML),
			Map.entry("project",     SyntaxConstants.SYNTAX_STYLE_XML),
			Map.entry("csproj",      SyntaxConstants.SYNTAX_STYLE_XML),
			Map.entry("html",        SyntaxConstants.SYNTAX_STYLE_HTML),
			Map.entry("htm",         SyntaxConstants.SYNTAX_STYLE_HTML),
			Map.entry("jsp",         SyntaxConstants.SYNTAX_STYLE_HTML),
			Map.entry("css",         SyntaxConstants.SYNTAX_STYLE_CSS),
			Map.entry("py",          SyntaxConstants.SYNTAX_STYLE_PYTHON),
			Map.entry("rb",          SyntaxConstants.SYNTAX_STYLE_RUBY),
			Map.entry("sh",          SyntaxConstants.SYNTAX_STYLE_UNIX_SHELL),
			Map.entry("bash",        SyntaxConstants.SYNTAX_STYLE_UNIX_SHELL),
			Map.entry("bat",         SyntaxConstants.SYNTAX_STYLE_WINDOWS_BATCH),
			Map.entry("cmd",         SyntaxConstants.SYNTAX_STYLE_WINDOWS_BATCH),
			Map.entry("sql",         SyntaxConstants.SYNTAX_STYLE_SQL),
			Map.entry("c",           SyntaxConstants.SYNTAX_STYLE_C),
			Map.entry("h",           SyntaxConstants.SYNTAX_STYLE_C),
			Map.entry("cpp",         SyntaxConstants.SYNTAX_STYLE_CPLUSPLUS),
			Map.entry("hpp",         SyntaxConstants.SYNTAX_STYLE_CPLUSPLUS),
			Map.entry("cs",          SyntaxConstants.SYNTAX_STYLE_CSHARP),
			Map.entry("go",          SyntaxConstants.SYNTAX_STYLE_GO),
			Map.entry("rs",          SyntaxConstants.SYNTAX_STYLE_RUST),
			Map.entry("php",         SyntaxConstants.SYNTAX_STYLE_PHP),
			Map.entry("yaml",        SyntaxConstants.SYNTAX_STYLE_YAML),
			Map.entry("yml",         SyntaxConstants.SYNTAX_STYLE_YAML),
			Map.entry("toml",        SyntaxConstants.SYNTAX_STYLE_YAML),
			Map.entry("md",          SyntaxConstants.SYNTAX_STYLE_MARKDOWN),
			Map.entry("properties",  SyntaxConstants.SYNTAX_STYLE_PROPERTIES_FILE),
			Map.entry("ini",         SyntaxConstants.SYNTAX_STYLE_INI),
			Map.entry("prefs",       SyntaxConstants.SYNTAX_STYLE_INI),
			Map.entry("cfg",         SyntaxConstants.SYNTAX_STYLE_INI),
			Map.entry("groovy",      SyntaxConstants.SYNTAX_STYLE_GROOVY),
			Map.entry("gradle",      SyntaxConstants.SYNTAX_STYLE_GROOVY),
			Map.entry("kt",          SyntaxConstants.SYNTAX_STYLE_KOTLIN),
			Map.entry("scala",       SyntaxConstants.SYNTAX_STYLE_SCALA),
			Map.entry("lua",         SyntaxConstants.SYNTAX_STYLE_LUA),
			Map.entry("perl",        SyntaxConstants.SYNTAX_STYLE_PERL),
			Map.entry("pl",          SyntaxConstants.SYNTAX_STYLE_PERL),
			Map.entry("dart",        SyntaxConstants.SYNTAX_STYLE_DART),
			Map.entry("csv",         SyntaxConstants.SYNTAX_STYLE_CSV),
			Map.entry("vsconfig",    SyntaxConstants.SYNTAX_STYLE_JSON),
			Map.entry("firebaserc",  SyntaxConstants.SYNTAX_STYLE_JSON)
	);

	private static final Map<String, String> SYNTAX_BY_FILENAME = Map.ofEntries(
			Map.entry("dockerfile",  SyntaxConstants.SYNTAX_STYLE_DOCKERFILE),
			Map.entry("classpath",   SyntaxConstants.SYNTAX_STYLE_XML),
			Map.entry("factorypath", SyntaxConstants.SYNTAX_STYLE_XML),
			Map.entry("project",     SyntaxConstants.SYNTAX_STYLE_XML),
			Map.entry("firebaserc",  SyntaxConstants.SYNTAX_STYLE_JSON),
			Map.entry("vsconfig",    SyntaxConstants.SYNTAX_STYLE_JSON)
	);

	private TextFileSupport() {
	}

	static boolean supports(Path path) {
		if (path == null) {
			return false;
		}
		if (isSvg(path)) {
			return false;
		}
		return matches(getName(path))
				|| matchesExtension(extension(getName(path)))
				|| hasShebang(path)
				|| looksLikeUtf8Text(path);
	}

	static boolean matches(String filename) {
		if (filename == null || filename.isBlank()) {
			return false;
		}

		String normalizedName = normalizeFileName(filename);
		return TEXT_FILENAMES.contains(normalizedName) || TEXT_EXTENSIONS.contains(extension(filename));
	}

	static boolean matchesExtension(String extension) {
		if (extension == null || extension.isBlank()) {
			return false;
		}
		return TEXT_EXTENSIONS.contains(extension.toLowerCase()); 
	}

	static boolean looksLikeUtf8Text(Path path) {
		byte[] sample = readSample(path);
		if (sample == null) {
			return false;
		}
		if (sample.length == 0) {
			return true;
		}
		if (matchesBinarySignature(sample) || containsNullByte(sample)) {
			return false;
		}
		String decoded = decodeUtf8(sample);
		if (decoded == null || decoded.isEmpty()) {
			return false;
		}
		return isLikelyHumanText(decoded);
	}

	static String syntaxStyle(String filename) {
		String style = SYNTAX_BY_FILENAME.get(normalizeFileName(filename));
		if (style != null) {
			return style;
		}
		return SYNTAX_BY_EXTENSION.getOrDefault(extension(filename), SyntaxConstants.SYNTAX_STYLE_NONE);
	}

	private static String normalizeFileName(String filename) {
		String normalized = filename.toLowerCase();
		if (normalized.startsWith(".")) {
			return normalized.substring(1);
		}
		return normalized;
	}

	private static String extension(String filename) {
		if (filename == null || filename.isBlank()) {
			return "";
		}
		int dot = filename.lastIndexOf('.');
		return dot > 0 ? filename.substring(dot + 1).toLowerCase() : "";
	}

	private static boolean hasShebang(Path resource) {
		byte[] sample = readSample(resource);
		return sample != null && sample.length >= 2 && sample[0] == '#' && sample[1] == '!';
	}

	private static boolean isSvg(Path resource) {
		return "svg".equals(extension(getName(resource)));
	}
	
	static String getName(Path path) {
		return path != null ? path.getFileName().toString() : path.toFile().getName();
	}

	private static byte[] readSample(Path resource) {
		if (resource == null) {
			return null;
		}
		try (InputStream in = java.nio.file.Files.newInputStream(resource)) {
			return in.readNBytes(SAMPLE_SIZE);
		} catch (Exception e) {
			return null;
		}
	}

	private static boolean containsNullByte(byte[] sample) {
		for (byte b : sample) {
			if (b == 0) {
				return true;
			}
		}
		return false;
	}

	private static boolean matchesBinarySignature(byte[] sample) {
		return startsWith(sample, 'M', 'Z')
				|| startsWith(sample, 0x7F, 'E', 'L', 'F')
				|| startsWith(sample, 'P', 'K', 0x03, 0x04)
				|| startsWith(sample, 'P', 'K', 0x05, 0x06)
				|| startsWith(sample, 'P', 'K', 0x07, 0x08)
				|| startsWith(sample, 0x89, 'P', 'N', 'G')
				|| startsWith(sample, 0xFF, 0xD8, 0xFF)
				|| startsWith(sample, 'G', 'I', 'F', '8')
				|| startsWith(sample, '%', 'P', 'D', 'F')
				|| startsWith(sample, 0xCA, 0xFE, 0xBA, 0xBE)
				|| startsWith(sample, 0xFE, 0xED, 0xFA, 0xCE)
				|| startsWith(sample, 0xCE, 0xFA, 0xED, 0xFE)
				|| startsWith(sample, 0xFE, 0xED, 0xFA, 0xCF)
				|| startsWith(sample, 0xCF, 0xFA, 0xED, 0xFE)
				|| startsWith(sample, 0xCA, 0xFE, 0xBA, 0xBF)
				|| startsWith(sample, 0xBF, 0xBA, 0xFE, 0xCA);
	}

	private static boolean startsWith(byte[] sample, int... prefix) {
		if (sample.length < prefix.length) {
			return false;
		}
		for (int i = 0; i < prefix.length; i++) {
			if ((sample[i] & 0xFF) != (prefix[i] & 0xFF)) {
				return false;
			}
		}
		return true;
	}

	private static String decodeUtf8(byte[] sample) {
		CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder()
				.onMalformedInput(CodingErrorAction.REPORT)
				.onUnmappableCharacter(CodingErrorAction.REPORT);
		try {
			CharBuffer decoded = decoder.decode(ByteBuffer.wrap(sample));
			return decoded.toString();
		} catch (CharacterCodingException e) {
			return null;
		}
	}

	private static boolean isLikelyHumanText(String decoded) {
		int printable = 0;
		int whitespace = 0;
		int suspiciousControls = 0;
		int considered = 0;
		for (int i = 0; i < decoded.length(); i++) {
			char ch = decoded.charAt(i);
			if (Character.isHighSurrogate(ch) && i + 1 < decoded.length()
					&& Character.isLowSurrogate(decoded.charAt(i + 1))) {
				int codePoint = Character.toCodePoint(ch, decoded.charAt(i + 1));
				i++;
				considered++;
				if (Character.isWhitespace(codePoint)) {
					whitespace++;
					printable++;
				} else if (!Character.isISOControl(codePoint)) {
					printable++;
				}
				continue;
			}
			considered++;
			if (ch == '\n' || ch == '\r' || ch == '\t' || ch == '\f') {
				whitespace++;
				printable++;
				continue;
			}
			if (Character.isISOControl(ch)) {
				suspiciousControls++;
				continue;
			}
			if (Character.isWhitespace(ch)) {
				whitespace++;
			}
			printable++;
		}
		if (considered == 0 || suspiciousControls > 0) {
			return false;
		}
		double printableRatio = printable / (double) considered;
		double whitespaceRatio = whitespace / (double) considered;
		return printableRatio >= MIN_PRINTABLE_RATIO && whitespaceRatio <= MAX_WHITESPACE_RATIO;
	}
}
