package vplay.player;

import java.awt.BorderLayout;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.swing.JPanel;
import uk.co.caprica.vlcj.factory.discovery.NativeDiscovery;
import uk.co.caprica.vlcj.player.component.CallbackMediaPlayerComponent;

public class VideoPlayer extends JPanel {

    private CallbackMediaPlayerComponent mediaPlayerComponent;

    public VideoPlayer() {
        setLayout(new BorderLayout());
        setBackground(java.awt.Color.BLACK);

        // Prepare Windows VLC paths (if applicable) before running discovery
        ensureVlcAvailable();

        // Discover VLC's native libraries
        boolean found = new NativeDiscovery().discover();

        if (!found) {
            throw new IllegalStateException(
                "VLC native libraries could not be found."
            );
        }

        // Initialize VLC media player (creates its MediaPlayerFactory internally)
        mediaPlayerComponent = new CallbackMediaPlayerComponent();

        add(mediaPlayerComponent, BorderLayout.CENTER);
    }

    private void ensureVlcAvailable() {
        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("win")) {
            // Optional: if you bundle a "vlc" folder next to the installed app
            Path bundledVlc = Paths.get(System.getProperty("user.dir"), "vlc");
            if (bundledVlc.toFile().isDirectory()) {
                System.setProperty("jna.library.path", bundledVlc.toString());
            }

            // Fallback to standard Windows VLC install paths
            String[] commonWinPaths = {
                "C:\\Program Files\\VideoLAN\\VLC",
                "C:\\Program Files (x86)\\VideoLAN\\VLC"
            };
            for (String p : commonWinPaths) {
                File dir = new File(p);
                if (dir.isDirectory()) {
                    String existingJna = System.getProperty("jna.library.path", "");
                    if (!existingJna.contains(p)) {
                        System.setProperty(
                            "jna.library.path",
                            existingJna.isEmpty() ? p : existingJna + File.pathSeparator + p
                        );
                    }
                    break;
                }
            }
        }
    }

    // Play a selected video file
    public void play(File file) {

        if (file == null || !file.isFile() || !file.canRead()) {
            throw new IllegalArgumentException(
                "Invalid or unreadable video file."
            );
        }

        mediaPlayerComponent.mediaPlayer()
                .media()
                .play(file.getAbsolutePath());
    }

    // Resume playback
    public void resume() {

        mediaPlayerComponent.mediaPlayer()
                .controls()
                .play();
    }

    // Pause playback
    public void pause() {

        mediaPlayerComponent.mediaPlayer()
                .controls()
                .setPause(true);
    }

    // Set volume between 0 and 100
    public void setVolume(int volume) {

        int safeVolume = Math.max(0, Math.min(100, volume));

        mediaPlayerComponent.mediaPlayer()
                .audio()
                .setVolume(safeVolume);
    }

    // Release VLC resources
    public void release() {

        if (mediaPlayerComponent != null) {
            mediaPlayerComponent.release();
            mediaPlayerComponent = null;
        }
    }

    // Get current playback position (0.0 to 1.0)
    public float getPosition() {
        return mediaPlayerComponent.mediaPlayer()
                .status()
                .position();
    }

    // Set playback position (0.0 to 1.0)
    public void setPosition(float position) {
        float safePosition = Math.max(0.0f, Math.min(1.0f, position));

        mediaPlayerComponent.mediaPlayer()
                .controls()
                .setPosition(safePosition);
    }
}