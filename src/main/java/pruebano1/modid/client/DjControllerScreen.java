package pruebano1.modid.client;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import pruebano1.modid.casino.CasinoSongs;
import pruebano1.modid.network.CasinoMusicControlPayload;

import java.util.ArrayList;
import java.util.List;

/**
 * Interfaz del DJ Controller: lista de canciones del casino, botón para detener y control de volumen.
 * Todo lo que se elige aquí se manda al servidor, que lo reparte a todos los jugadores.
 */
public class DjControllerScreen extends Screen {

    private static final int PANEL_WIDTH = 260;
    private static final int ROW_HEIGHT = 22;
    private static final int BUTTON_WIDTH = 230;

    // Colores Draftea (morado, negro y blanco)
    private static final int COLOR_PANEL = 0xEE0B0B0D;
    private static final int COLOR_BORDER = 0xFF9B5CFF;
    private static final int COLOR_HEADER = 0xFF2A1450;

    private final BlockPos controllerPos;
    private final List<ButtonWidget> songButtons = new ArrayList<>();
    /** Índice en CasinoSongs.SONGS de cada botón de la página actual. */
    private final List<Integer> songIndexes = new ArrayList<>();
    /** Página actual de canciones (-1 = elegir al abrir). */
    private int page = -1;
    private Text pageLabel;
    private int pageLabelY;
    private VolumeSlider volumeSlider;
    private int panelTop;
    private int panelHeight;
    private int statusTextY;
    /** Último volumen que mandamos, para no reenviar el mismo valor. */
    private int lastSentVolume = -1;

    public DjControllerScreen(BlockPos controllerPos) {
        super(Text.literal("Cabina DJ - Draftea"));
        this.controllerPos = controllerPos;
    }

    @Override
    protected void init() {
        songButtons.clear();
        songIndexes.clear();
        int songs = CasinoSongs.SONGS.size();

        // Cuántas canciones caben por página según el alto de la ventana (entre 3 y 8)
        int fixedHeight = 34 + 8 + ROW_HEIGHT + 12 + ROW_HEIGHT * 2 + 8;
        int perPage = MathHelper.clamp((this.height - 16 - fixedHeight - ROW_HEIGHT) / ROW_HEIGHT, 3, 8);
        if (songs <= perPage + 1) {
            perPage = songs; // Si caben todas (o sobra solo una), sin páginas
        }
        int pages = Math.max(1, (songs + perPage - 1) / perPage);
        if (page < 0) {
            // Al abrir: ir a la página de la canción que está sonando
            int playing = CasinoMusicClient.getSongIndex();
            page = CasinoSongs.isValid(playing) ? playing / perPage : 0;
        }
        page = MathHelper.clamp(page, 0, pages - 1);
        int first = page * perPage;
        int shown = Math.min(perPage, songs - first);
        boolean paged = pages > 1;

        // Título + canciones (+ fila de páginas) + detener + volumen + botones de volumen + márgenes
        panelHeight = fixedHeight + perPage * ROW_HEIGHT + (paged ? ROW_HEIGHT : 0);
        panelTop = Math.max(8, (this.height - panelHeight) / 2);
        int x = (this.width - BUTTON_WIDTH) / 2;
        int y = panelTop + 34;

        // 1. Una fila por canción de esta página
        for (int i = 0; i < shown; i++) {
            final int index = first + i;
            ButtonWidget button = ButtonWidget.builder(Text.empty(), b -> send(CasinoMusicControlPayload.ACTION_PLAY, index, 0))
                    .dimensions(x, y, BUTTON_WIDTH, 20)
                    .build();
            songButtons.add(addDrawableChild(button));
            songIndexes.add(index);
            y += ROW_HEIGHT;
        }
        y += (perPage - shown) * ROW_HEIGHT; // Mantener el panel del mismo tamaño en la última página

        // 1b. Cambiar de página
        if (paged) {
            final int lastPage = pages - 1;
            int arrowWidth = 40;
            ButtonWidget prev = addDrawableChild(ButtonWidget.builder(Text.literal("◀"), b -> {
                page--;
                clearAndInit();
            }).dimensions(x, y, arrowWidth, 20).build());
            ButtonWidget next = addDrawableChild(ButtonWidget.builder(Text.literal("▶"), b -> {
                page++;
                clearAndInit();
            }).dimensions(x + BUTTON_WIDTH - arrowWidth, y, arrowWidth, 20).build());
            prev.active = page > 0;
            next.active = page < lastPage;
            pageLabel = Text.literal("Página " + (page + 1) + " / " + pages).formatted(Formatting.GRAY);
            pageLabelY = y + 6;
            y += ROW_HEIGHT;
        } else {
            pageLabel = null;
        }

        // 2. Detener
        y += 8;
        addDrawableChild(ButtonWidget.builder(
                        Text.literal("■ Detener música").formatted(Formatting.RED),
                        b -> send(CasinoMusicControlPayload.ACTION_STOP, -1, 0))
                .dimensions(x, y, BUTTON_WIDTH, 20)
                .build());
        statusTextY = y + ROW_HEIGHT + 1;
        y += ROW_HEIGHT + 12;

        // 3. Volumen: deslizador + botones rápidos
        int volume = CasinoMusicClient.getVolume();
        lastSentVolume = volume;
        volumeSlider = addDrawableChild(new VolumeSlider(x, y, BUTTON_WIDTH, 20, volume));
        y += ROW_HEIGHT;

        int small = (BUTTON_WIDTH - 12) / 4;
        addDrawableChild(ButtonWidget.builder(Text.literal("-10"), b -> nudgeVolume(-10)).dimensions(x, y, small, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("+10"), b -> nudgeVolume(10)).dimensions(x + (small + 4), y, small, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Silencio"), b -> setVolume(0)).dimensions(x + (small + 4) * 2, y, small, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Máx."), b -> setVolume(100)).dimensions(x + (small + 4) * 3, y, small, 20).build());

        refreshSongLabels();
    }

    @Override
    public void tick() {
        super.tick();
        // Si otro jugador cambió la canción o el volumen, reflejarlo aquí
        refreshSongLabels();
        int serverVolume = CasinoMusicClient.getVolume();
        if (volumeSlider != null && !volumeSlider.isDragging() && serverVolume != volumeSlider.getPercent()) {
            volumeSlider.setPercent(serverVolume);
            lastSentVolume = serverVolume;
        }
    }

    private void refreshSongLabels() {
        int playing = CasinoMusicClient.getSongIndex();
        for (int i = 0; i < songButtons.size(); i++) {
            int songIndex = songIndexes.get(i);
            String title = CasinoSongs.SONGS.get(songIndex).title();
            boolean isPlaying = songIndex == playing;
            String prefix = isPlaying ? "▶ " : "♪ ";
            String trimmed = this.textRenderer.trimToWidth(prefix + title, BUTTON_WIDTH - 12);
            Text label = isPlaying
                    ? Text.literal(trimmed).formatted(Formatting.LIGHT_PURPLE, Formatting.BOLD)
                    : Text.literal(trimmed);
            songButtons.get(i).setMessage(label);
        }
    }

    private void nudgeVolume(int delta) {
        setVolume(volumeSlider.getPercent() + delta);
    }

    private void setVolume(int percent) {
        percent = MathHelper.clamp(percent, 0, 100);
        volumeSlider.setPercent(percent);
        sendVolume(percent);
    }

    private void sendVolume(int percent) {
        if (percent != lastSentVolume) {
            lastSentVolume = percent;
            send(CasinoMusicControlPayload.ACTION_VOLUME, -1, percent);
        }
    }

    private void send(int action, int song, int volume) {
        ClientPlayNetworking.send(new CasinoMusicControlPayload(controllerPos, action, song, volume));
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        super.renderBackground(context, mouseX, mouseY, delta);
        int left = (this.width - PANEL_WIDTH) / 2;
        int right = left + PANEL_WIDTH;
        int bottom = panelTop + panelHeight;
        // Marco morado
        context.fill(left - 1, panelTop - 1, right + 1, bottom + 1, COLOR_BORDER);
        context.fill(left, panelTop, right, bottom, COLOR_PANEL);
        // Cabecera
        context.fill(left, panelTop, right, panelTop + 26, COLOR_HEADER);
        context.fill(left, panelTop + 26, right, panelTop + 27, COLOR_BORDER);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer,
                Text.literal("♫ ").append(this.title).append(" ♫").formatted(Formatting.WHITE, Formatting.BOLD),
                this.width / 2, panelTop + 9, 0xFFFFFF);
        if (pageLabel != null) {
            context.drawCenteredTextWithShadow(this.textRenderer, pageLabel, this.width / 2, pageLabelY, 0xAAAAAA);
        }

        Text status;
        if (!CasinoMusicClient.isLocalPlayerInsideCasino()) {
            status = Text.literal("Estás fuera del casino: aquí no se escucha").formatted(Formatting.RED);
        } else if (CasinoSongs.isValid(CasinoMusicClient.getSongIndex())) {
            status = Text.literal("Sonando en todo el casino").formatted(Formatting.GRAY);
        } else {
            status = Text.literal("Sin música").formatted(Formatting.GRAY);
        }
        context.drawCenteredTextWithShadow(this.textRenderer, status, this.width / 2, statusTextY, 0xAAAAAA);
    }

    @Override
    public boolean shouldPause() {
        return false; // Que la música y el juego sigan mientras se elige
    }

    /** Deslizador de volumen 0-100%. Manda el valor al servidor mientras se mueve. */
    private class VolumeSlider extends SliderWidget {

        private boolean dragging = false;

        VolumeSlider(int x, int y, int width, int height, int percent) {
            super(x, y, width, height, Text.empty(), percent / 100.0D);
            updateMessage();
        }

        int getPercent() {
            return (int) Math.round(this.value * 100.0D);
        }

        void setPercent(int percent) {
            this.value = MathHelper.clamp(percent / 100.0D, 0.0D, 1.0D);
            updateMessage();
        }

        boolean isDragging() {
            return dragging;
        }

        @Override
        protected void updateMessage() {
            setMessage(Text.literal("Volumen: " + getPercent() + "%"));
        }

        @Override
        protected void applyValue() {
            sendVolume(getPercent());
        }

        @Override
        public void onClick(double mouseX, double mouseY) {
            dragging = true;
            super.onClick(mouseX, mouseY);
        }

        @Override
        public void onRelease(double mouseX, double mouseY) {
            dragging = false;
            super.onRelease(mouseX, mouseY);
        }
    }
}
