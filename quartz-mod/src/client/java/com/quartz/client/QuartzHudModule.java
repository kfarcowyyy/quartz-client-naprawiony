package com.quartz.client;

import net.minecraft.network.chat.Component;

enum QuartzHudModule {
    FPS("FPS", "FPS", "FPS {value}", true, 0, 0),
    CPS("CPS", "CPS", "CPS {value}", true, 0, 80),
    ARMOR("Armor Status", "Stan zbroi", "", false, 1000, 900),
    POTIONS("Potion Effects", "Efekty mikstur", "", false, 1000, 0),
    ARMOR_BAR("Armor Bar", "Pasek zbroi", "", false, 440, 940),
    COORDS("Coordinates", "Współrzędne", "XYZ {value}", false, 0, 110),
    DIRECTION("Direction", "Kierunek", "{value}", false, 1000, 158),
    PING("Ping", "Ping", "Ping {value}", false, 0, 206),
    SPEED("Speed Meter", "Prędkość", "{value}", false, 1000, 254),
    TIME("Time", "Zegar", "{value}", false, 0, 302),
    PLAYTIME("Playtime", "Czas gry", "{value}", false, 1000, 350),
    SATURATION("Saturation", "Nasycenie", "{value}", false, 0, 398),
    RESOURCES("System Resources", "Zasoby systemowe", "{value}", false, 1000, 446),
    PACK("Pack Display", "Aktywna paczka", "{value}", false, 0, 494),
    BLOCK("Block Indicator", "Wskaźnik bloku", "{value}", false, 1000, 542),
    ITEM_INFO("Item Info", "Informacje o przedmiocie", "{value}", false, 0, 590),
    ITEM_COUNT("Item Counter", "Licznik przedmiotów", "{value}", false, 1000, 638),
    ELYTRA("Elytras", "Elytra", "Elytra {value}", false, 0, 686),
    TOTEM("Totem", "Totem", "Totem ×{value}", false, 1000, 734),
    ATTACK("Attack Indicator", "Wskaźnik ataku", "{value}", false, 0, 782),
    KEYSTROKES("Keystrokes", "Naciśnięte klawisze", "{value}", false, 1000, 830),
    MOUSESTROKES("Mousestrokes", "Przyciski myszy", "{value}", false, 0, 878),
    INVENTORY("Inventory", "Ekwipunek", "", false, 1000, 840),
    LIGHT("Light Level", "Poziom światła", "Light {value}", false, 0, 520),
    REACH("Reach Display", "Zasięg uderzenia", "{value} m", false, 520, 620),
    DEATH("Death Info", "Pozycja śmierci", "XYZ {value}", false, 860, 510),
    STOPWATCH("Stopwatch", "Stoper", "{value}", false, 930, 460);

    final String en, pl, template;
    final boolean initiallyEnabled;
    final int x, y;

    QuartzHudModule(String en, String pl, String template, boolean initiallyEnabled, int x, int y) {
        this.en = en;
        this.pl = pl;
        this.template = template;
        this.initiallyEnabled = initiallyEnabled;
        this.x = x;
        this.y = y;
    }

    Component label() { return QuartzText.of(en, pl); }
    boolean icons() { return this == ARMOR || this == POTIONS || this == ARMOR_BAR || this == INVENTORY; }
    Component alternateLabel(boolean alternate) {
        return switch (this) {
            case CPS -> alternate ? QuartzText.of("Both buttons", "Oba przyciski") : QuartzText.of("Left button", "Lewy przycisk");
            case COORDS, DEATH -> alternate ? QuartzText.of("XYZ", "XYZ") : QuartzText.of("XZ only", "Tylko XZ");
            case TIME -> alternate ? QuartzText.of("12-hour clock", "Zegar 12 h") : QuartzText.of("24-hour clock", "Zegar 24 h");
            case SPEED -> alternate ? QuartzText.of("km/h", "km/h") : QuartzText.of("m/s", "m/s");
            case RESOURCES -> alternate ? QuartzText.of("RAM only", "Tylko RAM") : QuartzText.of("CPU and RAM", "CPU i RAM");
            case LIGHT -> alternate ? QuartzText.of("Block light", "Światło bloków") : QuartzText.of("Combined", "Łączne");
            case TOTEM -> alternate ? QuartzText.of("Hide zero", "Ukryj zero") : QuartzText.of("Show zero", "Pokazuj zero");
            default -> null;
        };
    }

    QuartzConfig.Hud settings() { return QuartzConfig.data.modules.get(name()); }
}
