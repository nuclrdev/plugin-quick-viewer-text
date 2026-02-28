# Text Quick Viewer

A [Nuclr Commander](https://nuclr.dev) plugin that renders plain text and source code files with syntax highlighting directly in the quick-view panel. Press **Ctrl+Q** on any supported file to preview it without leaving the file manager.

---

## Features

- **Syntax highlighting** — 50+ languages and file formats via RSyntaxTextArea
- **Dark theme** — matches the Commander UI out of the box
- **Line numbers** — shown in the gutter for easy reference
- **Code folding** — collapse blocks in languages that support it
- **10 MB guard** — files larger than 10 MB display a size warning instead of freezing the UI
- **Binary detection** — null-byte scan of the first 8 KB silently skips binary files so the next provider (or the no-provider card) is shown instead
- **NIO.2 compatible** — works inside ZIP and JAR archives (reads via `QuickViewItem.path()` or `openStream()` for non-local filesystems)
- **Cancellation-aware** — switching files mid-load immediately aborts the in-flight read; no stale content ever reaches the UI

---

## Supported File Types

| Category | Extensions |
|----------|-----------|
| Plain text | `txt`, `log`, `csv`, `md` |
| Web | `html`, `htm`, `css`, `js`, `mjs`, `ts`, `tsx`, `jsp` |
| Data / config | `json`, `yaml`, `yml`, `toml`, `ini`, `conf`, `cfg`, `properties`, `prefs`, `pref` |
| JVM | `java`, `kt`, `scala`, `groovy`, `gradle` |
| Systems | `c`, `cpp`, `h`, `hpp`, `cs`, `go`, `rs` |
| Scripting | `py`, `rb`, `php`, `lua`, `perl`, `pl`, `dart`, `sql` |
| Shell / scripts | `sh`, `bash`, `bat`, `cmd`, `ps1` |
| XML / markup | `xml`, `svg`, `html`, `htm`, `jsp`, `csproj`, `classpath`, `project`, `factorypath` |
| IDE / project | `vsconfig`, `firebaserc` |
| Dotfiles | `.gitignore`, `.gitattributes`, `.meta` |
| Container | `dockerfile` |

---

## Building

Requires **Java 21+** and **Maven 3.9+**. The plugin SDK must be installed first:

```bash
# 1. Install the SDK (one-time)
cd ../../..   # → nuclr/sources/plugins-sdk
mvn clean install

# 2. Build the plugin ZIP (no signing)
cd plugins/core/quick-viewer-text
mvn clean package -Dmaven.verify.skip=true
# Output: target/quick-view-text-1.0.0.zip
```

### Building with signing

Signing requires the Nuclr keystore at `C:/nuclr/key/nuclr-signing.p12`:

```bash
mvn clean verify -Djarsigner.storepass=<password>
# Output: target/quick-view-text-1.0.0.zip
#         target/quick-view-text-1.0.0.zip.sig
```

### Deploy to Commander

Copy both artifacts to the Commander `plugins/` directory:

```bash
cp target/quick-view-text-1.0.0.zip     /path/to/commander/plugins/
cp target/quick-view-text-1.0.0.zip.sig /path/to/commander/plugins/
```

---

## Architecture

```
TextQuickViewProvider         implements QuickViewProvider
└── TextQuickViewPanel        Swing JPanel — RSyntaxTextArea wrapped in RTextScrollPane
```

### Loading flow

1. `matches(item)` — extension lookup against `TEXT_EXTENSIONS` (no I/O)
2. `open(item, cancelled)` → `TextQuickViewPanel.load(item, cancelled)` on a virtual thread:
   - Size check (> 10 MB → show message, return `true`)
   - Binary scan (null bytes in first 8 KB → return `false`, fall through)
   - Read full content via `Path` (local / ZIP) or `InputStream` (remote)
   - Dispatch `setText(filename, content)` to the EDT
3. `setText` — selects syntax style from `EXTENSION_TO_SYNTAX`, builds a fresh `RSyntaxDocument`, swaps it into the text area (old document and its token-factory pool become GC-eligible)

### Threading model

- **EDT** — all Swing reads/writes, document swaps, repaints
- **Virtual thread** — file I/O, binary detection (called by `QuickViewPanel`)
- **Cancellation** — `AtomicBoolean cancelled` is checked before every dispatch to the EDT; setting it causes any pending `invokeLater` callbacks to no-op

### Bundled dependencies (in `lib/`)

| Artifact | Version | License |
|----------|---------|---------|
| `rsyntaxtextarea` | 3.6.1 | BSD 3-Clause |

> RSyntaxTextArea is already present in the Commander fat JAR. The copy in `lib/` is unused at runtime (parent-classloader delegation finds it first) but is included so the plugin remains self-contained if deployed to a different host.

---

## License

Apache 2.0 — see [LICENSE](LICENSE).
