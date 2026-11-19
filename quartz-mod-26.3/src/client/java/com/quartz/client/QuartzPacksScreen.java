package com.quartz.client;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipFile;

public final class QuartzPacksScreen extends Screen {
    private final Screen parent;
    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger("Quartz Packs");
    private QuartzPackDownloads.Kind kind;
    private EditBox input;
    private String query = "";
    private Component notice;
    private QuartzPackDownloads.Results results = new QuartzPackDownloads.Results(List.of(), 0);
    private boolean busy;
    private int left, span, limit, offset;

    public QuartzPacksScreen(Screen parent) { this(parent, QuartzPackDownloads.Kind.RESOURCES); }

    QuartzPacksScreen(Screen parent, QuartzPackDownloads.Kind kind) {
        super(QuartzText.of("Packs & shaders", "Paczki i shadery"));
        this.parent = parent;
        this.kind = kind;
    }

    @Override
    protected void init() {
        span = Math.min(490, width - 24);
        left = (width - span) / 2;
        limit = Math.min(20, Math.max(1, (height - 116) / 24));
        int half = (span - 6) / 2;
        add(QuartzText.of("Resource packs", "Paczki zasobów"), left, 26, half,
                kind == QuartzPackDownloads.Kind.RESOURCES, () -> change(QuartzPackDownloads.Kind.RESOURCES)).active = !busy;
        add(QuartzText.of("Shaders", "Shadery"), left + half + 6, 26, half,
                kind == QuartzPackDownloads.Kind.SHADERS, () -> change(QuartzPackDownloads.Kind.SHADERS)).active = !busy;
        input = addRenderableWidget(new EditBox(font, left, 51, span - 76, 19, QuartzText.of("Search Modrinth", "Szukaj w Modrinth")));
        input.setMaxLength(100);
        input.setValue(query);
        input.setHint(QuartzText.of("Search Modrinth…", "Szukaj w Modrinth…"));
        add(QuartzText.of("Search", "Szukaj"), left + span - 70, 51, 70, true, () -> search(0)).active = !busy;
        for (int i = 0; i < Math.min(results.items().size(), limit); i++) {
            var item = results.items().get(i);
            add(QuartzText.of("Download", "Pobierz"), left + span - 77, 81 + i * 24, 70, false,
                    () -> install(item)).active = !busy;
        }
        int footer = height - 28;
        add(Component.literal("‹"), left, footer, 20, false, () -> search(Math.max(0, offset - limit))).active = !busy && offset > 0;
        add(Component.literal("›"), left + 24, footer, 20, false, () -> search(offset + limit)).active = !busy && offset + limit < results.total();
        add(QuartzText.of("Folder", "Folder"), left + 50, footer, 52, false, () -> QuartzFiles.openFolder(kind.folder));
        if (kind == QuartzPackDownloads.Kind.SHADERS && FabricLoader.getInstance().isModLoaded("iris")) {
            add(QuartzText.of("Iris settings", "Ustawienia Iris"), left + 108, footer, 88, false, this::openIris);
        }
        add(QuartzText.of("Back", "Wróć"), left + span - 55, footer, 55, false, this::onClose);
    }

    private void change(QuartzPackDownloads.Kind newType) {
        if (busy || kind == newType) return;
        kind = newType;
        results = new QuartzPackDownloads.Results(List.of(), 0);
        search(0);
    }

    private void search(int from) {
        if (busy) return;
        query = input.getValue().strip();
        offset = from;
        busy = true;
        notice = QuartzText.of("Searching Modrinth…", "Szukam w Modrinth…");
        rebuildWidgets();
        QuartzPackDownloads.search(kind, query, offset, limit).whenComplete((found, error) -> minecraft.execute(() -> {
            busy = false;
            if (error == null) {
                results = found;
                notice = found.items().isEmpty() ? QuartzText.of("No compatible results.", "Brak zgodnych wyników.") : null;
            } else {
                LOG.warn("Modrinth pack search failed", error);
                results = new QuartzPackDownloads.Results(List.of(), 0);
                notice = QuartzText.of("Modrinth search unavailable.", "Nie udało się wyszukać paczek.");
            }
            rebuildWidgets();
        }));
    }

    private void install(QuartzPackDownloads.Project item) {
        if (busy) return;
        var type = kind;
        busy = true;
        notice = QuartzText.of("Downloading " + item.title() + "…", "Pobieram " + item.title() + "…");
        rebuildWidgets();
        QuartzPackDownloads.install(type, item, minecraft.gameDirectory.toPath()).whenComplete((file, error) -> minecraft.execute(() -> {
            busy = false;
            if (error == null) {
                if (type == QuartzPackDownloads.Kind.RESOURCES) activate(file);
                else notice = QuartzText.of("Shader installed. Select it in Iris.", "Shader pobrany. Wybierz go w Iris.");
            } else {
                LOG.warn("Could not install Modrinth pack", error);
                notice = QuartzText.of("Could not install this pack.", "Nie udało się pobrać tej paczki.");
            }
            rebuildWidgets();
        }));
    }

    private void activate(String file) {
        try {
            var repository = minecraft.getResourcePackRepository();
            repository.reload();
            String id = "file/" + file;
            if (!repository.getAvailableIds().contains(id)) {
                notice = QuartzText.of("Installed. Open Minecraft resource pack settings.", "Pobrano. Sprawdź ustawienia paczek zasobów.");
                return;
            }
            var selected = new ArrayList<>(repository.getSelectedIds());
            selected.remove(id);
            selected.add(id);
            repository.setSelected(selected);
            minecraft.options.updateResourcePacks(repository);
            notice = QuartzText.of("Resource pack installed and enabled.", "Paczka zasobów pobrana i włączona.");
        } catch (RuntimeException error) {
            LOG.warn("Could not enable the downloaded resource pack", error);
            notice = QuartzText.of("Installed. Select the pack in Minecraft settings.", "Pobrano. Wybierz paczkę w ustawieniach Minecrafta.");
        }
    }

    private void openIris() {
        try {
            var screen = Class.forName("net.irisshaders.iris.gui.screen.ShaderPackScreen").getConstructor(Screen.class);
            minecraft.gui.setScreen((Screen) screen.newInstance(this));
        } catch (ReflectiveOperationException | LinkageError error) {
            notice = QuartzText.of("Open your shader settings from Video Settings.", "Otwórz shadery w ustawieniach grafiki.");
        }
    }

    @Override
    public void onFilesDrop(List<Path> files) {
        int added = 0;
        for (Path source : files) {
            String name = source.getFileName().toString();
            if (!Files.isRegularFile(source) || !name.toLowerCase(Locale.ROOT).endsWith(".zip")) continue;
            try (ZipFile zip = new ZipFile(source.toFile())) {
                String folder;
                if (zip.getEntry("pack.mcmeta") != null) folder = "resourcepacks";
                else if (zip.stream().anyMatch(entry -> entry.getName().startsWith("shaders/"))) folder = "shaderpacks";
                else continue;
                Path destination = minecraft.gameDirectory.toPath().resolve(folder);
                Files.createDirectories(destination);
                Files.copy(source, destination.resolve(name));
                added++;
            } catch (IOException ignored) { }
        }
        notice = QuartzText.of("Installed " + added + " of " + files.size(), "Zainstalowano " + added + " z " + files.size());
    }

    private QuartzButton add(Component label, int x, int y, int w, boolean accent, Runnable action) {
        return addRenderableWidget(new QuartzButton(label, x, y, w, 19, accent, action));
    }

    @Override public void extractBackground(GuiGraphicsExtractor g, int x, int y, float delta) {}

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int x, int y, float delta) {
        QuartzTheme.background(g, width, height);
        g.text(font, title, left, 12, QuartzTheme.ACCENT, false);
        for (int i = 0; i < Math.min(results.items().size(), limit); i++) {
            int row = 79 + i * 24;
            QuartzTheme.card(g, left, row, span, 22, QuartzTheme.WHITE, QuartzTheme.OUTLINE);
            String label = font.plainSubstrByWidth(results.items().get(i).title(), span - 91);
            g.text(font, label, left + 8, row + 8, QuartzTheme.INK, false);
        }
        String status = notice == null ? kind == QuartzPackDownloads.Kind.SHADERS && !FabricLoader.getInstance().isModLoaded("iris")
                ? QuartzText.of("Iris is required to use downloaded shaders.", "Do używania pobranych shaderów potrzebujesz Iris.").getString()
                : QuartzText.of("Search Modrinth or drop a ZIP here.", "Wyszukaj w Modrinth lub upuść tu ZIP.").getString()
                : notice.getString();
        g.text(font, font.plainSubstrByWidth(status, span), left, height - 43, QuartzTheme.MUTED, false);
        super.extractRenderState(g, x, y, delta);
    }

    @Override public void onClose() { Minecraft.getInstance().gui.setScreen(parent); }
}
