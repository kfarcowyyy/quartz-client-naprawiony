package com.quartz.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class QuartzHud {
    private static final EquipmentSlot[] SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };
    private static final List<ItemStack> EXAMPLE_ARMOR = List.of(
            new ItemStack(Items.DIAMOND_HELMET), new ItemStack(Items.DIAMOND_CHESTPLATE),
            new ItemStack(Items.DIAMOND_LEGGINGS), new ItemStack(Items.DIAMOND_BOOTS));
    private static final Identifier SLOT = Identifier.withDefaultNamespace("hud/hotbar_selection");
    private static final int CELL_WIDTH = 34;
    private static final java.time.format.DateTimeFormatter CLOCK = java.time.format.DateTimeFormatter.ofPattern("HH:mm");
    private static final com.sun.management.OperatingSystemMXBean OS =
            java.lang.management.ManagementFactory.getOperatingSystemMXBean() instanceof com.sun.management.OperatingSystemMXBean bean ? bean : null;
    private static long lastSample, joined;
    private static String cpuRam = "";
    private static Object world;
    private static double previousX, previousZ, speed;
    private static boolean hasPosition;

    static void tick(Minecraft client) {
        if (client.level != world) {
            world = client.level;
            joined = System.nanoTime();
            hasPosition = false;
            speed = 0;
        }
        if (client.player == null) return;
        double x = client.player.getX(), z = client.player.getZ();
        if (hasPosition) {
            double distance = Math.hypot(x - previousX, z - previousZ);
            speed = distance > 12 ? 0 : (speed + 20 * distance) / 2;
        }
        previousX = x;
        previousZ = z;
        hasPosition = true;
    }

    private QuartzHud() {}

    record Cell(String value, ItemStack item, Identifier icon) {}
    record Panel(String text, List<Cell> cells, int width, int height) {}
    record Position(int x, int y) {}

    static Panel panel(QuartzHudModule module, Minecraft client, boolean preview, int width, int height) {
        var settings = module.settings();
        if (!module.icons()) {
            String template = settings.text == null || settings.text.isBlank() ? module.template : settings.text;
            String measurement = value(module, client, preview);
            if (measurement.isBlank()) return new Panel("", List.of(), 0, 0);
            String text = template.replace("{value}", measurement);
            if (settings.brackets) text = "[" + text + "]";
            text = client.font.plainSubstrByWidth(text, Math.max(0, width - 12));
            return new Panel(text, List.of(), Math.min(width, client.font.width(text) + 12), 18);
        }

        List<Cell> cells = new ArrayList<>();
        if (module == QuartzHudModule.ARMOR_BAR) {
            int armor = preview ? 14 : client.player.getArmorValue();
            return new Panel(Integer.toString(armor), List.of(), 92, 12);
        }
        if (module == QuartzHudModule.ARMOR) {
            for (int i = 0; i < SLOTS.length; i++) {
                ItemStack item = preview ? EXAMPLE_ARMOR.get(i) : client.player.getItemBySlot(SLOTS[i]);
                if (item.isEmpty()) continue;
                int max = item.getMaxDamage();
                String value = max > 0 ? Math.max(0, 100 - (int) (100L * item.getDamageValue() / max)) + "%" : "";
                cells.add(new Cell(value, item, null));
            }
        } else if (preview) {
            cells.add(new Cell("1:24", ItemStack.EMPTY, Identifier.withDefaultNamespace("mob_effect/speed")));
            cells.add(new Cell("0:48", ItemStack.EMPTY, Identifier.withDefaultNamespace("mob_effect/regeneration")));
        } else {
            for (var effect : client.player.getActiveEffects()) {
                var key = effect.getEffect().unwrapKey();
                if (key.isEmpty() || !effect.showIcon()) continue;
                int seconds = Math.max(0, effect.getDuration() / 20);
                String value = effect.isInfiniteDuration() ? "∞" : String.format(Locale.ROOT, "%d:%02d", seconds / 60, seconds % 60);
                cells.add(new Cell(value, ItemStack.EMPTY, key.get().identifier().withPrefix("mob_effect/")));
            }
        }
        int columns = Math.max(1, width / CELL_WIDTH);
        int line = settings.showValue ? 35 : 26;
        int count = Math.min(cells.size(), columns * Math.max(1, height / line));
        return new Panel("", cells.subList(0, count), Math.min(columns, count) * CELL_WIDTH, ((count + columns - 1) / columns) * line);
    }

    private static String value(QuartzHudModule module, Minecraft client, boolean preview) {
        return switch (module) {
            case FPS -> Integer.toString(preview ? 144 : client.getFps());
            case CPS -> preview ? "8 | 3" : QuartzConfig.cps();
            case COORDS -> preview ? "128  72  -45" : coords(client);
            case DIRECTION -> preview ? "North" : client.player.getDirection().getName();
            case PING -> preview ? "42 ms" : ping(client);
            case SPEED -> preview ? "5.6" : String.format(Locale.ROOT, "%.1f", speed);
            case TIME -> preview ? "11:27" : java.time.LocalTime.now().format(CLOCK);
            case PLAYTIME -> preview ? "01:12:02" : elapsed();
            case SATURATION -> preview ? "20.0" : String.format(Locale.ROOT, "%.1f", client.player.getFoodData().getSaturationLevel());
            case RESOURCES -> preview ? "CPU 38% · RAM 800/2048 MB" : system();
            case PACK -> preview ? "Example resource pack" : pack(client);
            case BLOCK -> preview ? "Grass Block" : block(client);
            case ITEM_INFO -> preview ? "Diamond Pickaxe" : held(client);
            case ITEM_COUNT -> preview ? "Arrow ×64" : countItem(client);
            case ELYTRA -> preview ? "92%" : elytra(client);
            case TOTEM -> preview ? "3" : Integer.toString(count(client, Items.TOTEM_OF_UNDYING));
            case ATTACK -> preview ? "89%" : Math.round(client.player.getAttackStrengthScale(0.0f) * 100) + "%";
            case KEYSTROKES -> preview ? "W  A  S  D  ␣" : keys(client);
            case MOUSESTROKES -> preview ? "LMB ●  RMB ○" : mouse(client);
            default -> "";
        };
    }

    private static String coords(Minecraft client) {
        var p = client.player.blockPosition();
        return p.getX() + "  " + p.getY() + "  " + p.getZ();
    }

    private static String ping(Minecraft client) {
        if (client.getConnection() == null) return "—";
        var entry = client.getConnection().getPlayerInfo(client.player.getUUID());
        return entry == null ? "—" : entry.getLatency() + " ms";
    }

    private static String elapsed() {
        long s = Math.max(0, (System.nanoTime() - joined) / 1_000_000_000L);
        return String.format(Locale.ROOT, "%02d:%02d:%02d", s / 3600, s / 60 % 60, s % 60);
    }

    private static String system() {
        long now = System.nanoTime();
        if (now - lastSample < 1_000_000_000L && !cpuRam.isEmpty()) return cpuRam;
        var ram = Runtime.getRuntime();
        double cpu = OS == null ? -1 : OS.getCpuLoad();
        cpuRam = (cpu < 0 ? "" : "CPU " + Math.round(cpu * 100) + "% · ")
                + "RAM " + ((ram.totalMemory() - ram.freeMemory()) >> 20) + "/" + (ram.maxMemory() >> 20) + " MB";
        lastSample = now;
        return cpuRam;
    }

    private static String pack(Minecraft client) {
        var ids = client.getResourcePackRepository().getSelectedIds().stream().filter(id -> id.startsWith("file/")).toList();
        return ids.isEmpty() ? QuartzText.of("Default", "Domyślna").getString() : ids.getLast().substring(5);
    }

    private static String block(Minecraft client) {
        return client.hitResult instanceof net.minecraft.world.phys.BlockHitResult hit && client.level != null
                && hit.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK
                ? client.level.getBlockState(hit.getBlockPos()).getBlock().getName().getString() : "";
    }

    private static String held(Minecraft client) {
        var stack = client.player.getMainHandItem();
        return stack.isEmpty() ? "" : stack.getHoverName().getString();
    }

    private static int count(Minecraft client, net.minecraft.world.item.Item item) {
        int total = 0;
        var inventory = client.player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            var stack = inventory.getItem(i);
            if (stack.is(item)) total += stack.getCount();
        }
        return total;
    }

    private static String countItem(Minecraft client) {
        var stack = client.player.getMainHandItem();
        String target = QuartzHudModule.ITEM_COUNT.settings().target;
        if (target != null && !target.isBlank()) {
            var id = Identifier.tryParse(target);
            if (id != null) {
                var item = net.minecraft.core.registries.BuiltInRegistries.ITEM.getOptional(id);
                if (item.isPresent()) stack = new ItemStack(item.get());
            }
        }
        return stack.isEmpty() ? "" : stack.getHoverName().getString() + " ×" + count(client, stack.getItem());
    }

    private static String elytra(Minecraft client) {
        var chest = client.player.getItemBySlot(EquipmentSlot.CHEST);
        if (!chest.is(Items.ELYTRA) || chest.getMaxDamage() <= 0) return "";
        return Math.max(0, 100 - chest.getDamageValue() * 100 / chest.getMaxDamage()) + "%";
    }

    private static String keys(Minecraft client) {
        var k = client.options;
        return key(k.keyUp) + "  " + key(k.keyLeft) + "  " + key(k.keyDown) + "  " + key(k.keyRight) + "  " + key(k.keyJump);
    }

    private static String key(net.minecraft.client.KeyMapping key) {
        String name = key.getTranslatedKeyMessage().getString();
        return key.isDown() ? "[" + name + "]" : name;
    }

    private static String mouse(Minecraft client) {
        return "LMB " + (client.options.keyAttack.isDown() ? "●" : "○")
                + "  RMB " + (client.options.keyUse.isDown() ? "●" : "○");
    }

    static Position position(QuartzConfig.Hud settings, Panel panel, int width, int height) {
        int x = Math.max(0, width - panel.width()), y = Math.max(0, height - panel.height());
        return new Position(settings.oldX == null ? x * clamp(settings.x, 1000) / 1000 : clamp(settings.oldX, x),
                settings.oldY == null ? y * clamp(settings.y, 1000) / 1000 : clamp(settings.oldY, y));
    }

    static void move(QuartzConfig.Hud settings, Panel panel, int x, int y, int width, int height) {
        settings.oldX = null;
        settings.oldY = null;
        int maxX = Math.max(1, width - panel.width()), maxY = Math.max(1, height - panel.height());
        settings.x = Math.round(1000f * clamp(x, maxX) / maxX);
        settings.y = Math.round(1000f * clamp(y, maxY) / maxY);
    }

    private static int clamp(int n, int max) { return Math.max(0, Math.min(max, n)); }

    static void draw(GuiGraphics g, Minecraft client, QuartzHudModule module, Panel panel, int x, int y) {
        if (panel.width() == 0) return;
        var settings = module.settings();
        var font = client.font;
        if (module == QuartzHudModule.ARMOR_BAR) {
            if (settings.background) g.fill(x, y, x + panel.width(), y + panel.height(), settings.bg);
            int armor = Integer.parseInt(panel.text());
            for (int i = 0; i < 10; i++) {
                String sprite = armor >= (i + 1) * 2 ? "full" : armor == i * 2 + 1 ? "half" : "empty";
                g.blitSprite(RenderPipelines.GUI_TEXTURED, Identifier.withDefaultNamespace("hud/armor_" + sprite), x + 1 + i * 9, y + 1, 9, 9);
            }
            return;
        }
        if (!module.icons()) {
            if (settings.background) QuartzTheme.rounded(g, x, y, panel.width(), panel.height(), settings.bg);
            g.drawString(font, panel.text(), x + 6, y + 5, settings.fg, settings.shadow);
            return;
        }
        int columns = Math.max(1, panel.width() / CELL_WIDTH);
        int line = settings.showValue ? 35 : 26;
        for (int i = 0; i < panel.cells().size(); i++) {
            Cell cell = panel.cells().get(i);
            int left = x + i % columns * CELL_WIDTH, top = y + i / columns * line;
            if (settings.background) {
                g.fill(left + 7, top + 3, left + 27, top + 23, settings.bg);
                g.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT, left + 5, top + 1, 24, 24);
            }
            if (!cell.item().isEmpty()) g.renderItem(cell.item(), left + 9, top + 5);
            if (cell.icon() != null) g.blitSprite(RenderPipelines.GUI_TEXTURED, cell.icon(), left + 9, top + 5, 16, 16);
            if (settings.showValue) {
                String value = font.plainSubstrByWidth(cell.value(), CELL_WIDTH - 2);
                g.drawString(font, value, left + (CELL_WIDTH - font.width(value)) / 2, top + 26, settings.fg, settings.shadow);
            }
        }
    }

    static void inGame(GuiGraphics g, Minecraft client) {
        int width = g.guiWidth(), height = g.guiHeight();
        for (QuartzHudModule module : QuartzHudModule.values()) {
            if (!module.settings().enabled) continue;
            Panel panel = panel(module, client, false, width, height);
            Position pos = position(module.settings(), panel, width, height);
            draw(g, client, module, panel, pos.x(), pos.y());
        }
    }
}
