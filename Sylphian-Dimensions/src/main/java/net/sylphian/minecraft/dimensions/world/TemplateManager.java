package net.sylphian.minecraft.dimensions.world;

import net.sylphian.minecraft.dimensions.model.Dimension;
import org.bukkit.Bukkit;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.Comparator;
import java.util.Properties;
import java.util.logging.Logger;
import java.util.stream.Stream;

/**
 * Copies dimension templates into active dimension folders under the unified
 * world storage layout ({@code world/dimensions/sylphian/<name>/}).
 *
 * <p>The active copy records the template name and version it was built from
 * plus the copy timestamp; a name or version mismatch on startup forces a
 * fresh re-copy, which is also the mechanism for deliberate full resets
 * (bump the version in config).</p>
 *
 * <p>A template may be either a bare dimension folder ({@code region/},
 * {@code entities/}, {@code poi/}, {@code data/}) or a copied world folder
 * from a build server, in which case the overworld dimension inside it
 * ({@code dimensions/minecraft/overworld/}) is used as the source.</p>
 */
public class TemplateManager {

    private static final String VERSION_FILE = ".sylphian-template-version";

    private final Path templatesDir;
    private final Path dimensionsRoot;
    private final Logger logger;

    /**
     * @param templatesDir   the folder holding template folders
     * @param dimensionsRoot the active dimensions folder, i.e. {@code world/dimensions/sylphian}
     * @param logger         the logger for progress and warnings
     */
    public TemplateManager(Path templatesDir, Path dimensionsRoot, Logger logger) {
        this.templatesDir = templatesDir;
        this.dimensionsRoot = dimensionsRoot;
        this.logger = logger;
    }

    /**
     * Ensures the active dimension folder exists and matches its template
     * version. Copies from the template when missing or outdated.
     * Blocking IO; call from onEnable before the world is loaded.
     *
     * @param dimension the dimension to prepare
     * @throws IOException if the template folder is missing or the copy fails
     */
    public void prepare(Dimension dimension) throws IOException {
        Path active = dimensionsRoot.resolve(dimension.name());
        if (Files.isDirectory(active) && upToDate(active, dimension)) return;

        // Never touch files under a world the server currently has loaded
        if (Bukkit.getWorld(DimensionManager.worldKey(dimension.name())) != null) {
            logger.severe("World '" + DimensionManager.worldKey(dimension.name())
                    + "' is already loaded; skipping template copy for dimension '" + dimension.name() + "'.");
            return;
        }

        Path source = resolveTemplateRoot(templatesDir.resolve(dimension.template()));

        if (Files.exists(active)) {
            logger.info("Template changed for dimension '" + dimension.name() + "'; re-copying from template.");
            deleteRecursively(active);
        } else {
            logger.info("Creating active dimension '" + dimension.name() + "' from template '" + dimension.template() + "'.");
        }

        Files.createDirectories(active.getParent());
        copyRecursively(source, active);
        writeMarker(active, dimension);
    }

    /**
     * Deletes the active copy and re-copies it from the template, regardless
     * of version. The caller must ensure the world is unloaded first.
     * Blocking IO; safe to run off the main thread.
     *
     * @param dimension the dimension to re-copy
     * @throws IOException if the template folder is missing or the copy fails
     */
    public void recopy(Dimension dimension) throws IOException {
        Path active = dimensionsRoot.resolve(dimension.name());
        Path source = resolveTemplateRoot(templatesDir.resolve(dimension.template()));

        deleteRecursively(active);
        Files.createDirectories(active.getParent());
        copyRecursively(source, active);
        writeMarker(active, dimension);
    }

    /**
     * Returns the folder inside a template that holds the dimension data.
     * Accepts a bare dimension folder or a copied world folder from a build server.
     */
    private Path resolveTemplateRoot(Path template) throws IOException {
        if (Files.isDirectory(template.resolve("region"))) return template;

        Path nested = template.resolve("dimensions").resolve("minecraft").resolve("overworld");
        if (Files.isDirectory(nested.resolve("region"))) return nested;

        throw new IOException("Template '" + template + "' contains no region folder "
                + "(expected region/ directly, or dimensions/minecraft/overworld/region for a copied world folder).");
    }

    /**
     * Returns when the active copy was created, read from its marker.
     *
     * @param dimension the dimension to query
     * @return the copied-at timestamp, or "unknown" if the marker is missing or unreadable
     */
    public String copiedAt(Dimension dimension) {
        Properties marker = new Properties();
        try (InputStream in = Files.newInputStream(dimensionsRoot.resolve(dimension.name()).resolve(VERSION_FILE))) {
            marker.load(in);
        } catch (IOException e) {
            return "unknown";
        }
        return marker.getProperty("copied-at", "unknown");
    }

    /**
     * Returns whether the active copy was built from the same template name
     * and version the dimension is configured with. An unreadable or missing
     * marker counts as outdated.
     */
    private boolean upToDate(Path active, Dimension dimension) {
        Properties marker = new Properties();
        try (InputStream in = Files.newInputStream(active.resolve(VERSION_FILE))) {
            marker.load(in);
        } catch (IOException e) {
            return false;
        }
        return dimension.template().equals(marker.getProperty("template"))
                && Integer.toString(dimension.templateVersion()).equals(marker.getProperty("version"));
    }

    private void writeMarker(Path active, Dimension dimension) throws IOException {
        Properties marker = new Properties();
        marker.setProperty("template", dimension.template());
        marker.setProperty("version", Integer.toString(dimension.templateVersion()));
        marker.setProperty("copied-at", Instant.now().toString());
        try (OutputStream out = Files.newOutputStream(active.resolve(VERSION_FILE))) {
            marker.store(out, "Sylphian-Dimensions active copy marker");
        }
    }

    private void deleteRecursively(Path path) throws IOException {
        if (!Files.exists(path)) return;
        try (Stream<Path> walk = Files.walk(path)) {
            walk.sorted(Comparator.reverseOrder()).forEach(p -> {
                try {
                    Files.delete(p);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        } catch (UncheckedIOException e) {
            throw e.getCause();
        }
    }

    private void copyRecursively(Path source, Path target) throws IOException {
        try (Stream<Path> walk = Files.walk(source)) {
            walk.forEach(p -> {
                try {
                    Files.copy(p, target.resolve(source.relativize(p).toString()),
                            StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        } catch (UncheckedIOException e) {
            throw e.getCause();
        }
    }
}
