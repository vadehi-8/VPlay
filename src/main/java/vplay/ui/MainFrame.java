package vplay.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.net.URL;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import vplay.player.VideoPlayer;

public class MainFrame extends JFrame {

    // Baby-pink colour palette
    private final Color BABY_PINK = new Color(248, 200, 220);
    private final Color LIGHT_PINK = new Color(252, 228, 236);
    private final Color BACKGROUND = new Color(255, 247, 250);
    private final Color ACCENT = new Color(184, 92, 131);
    private final Color DARK_TEXT = new Color(80, 45, 62);

    // Playlist components
    private DefaultListModel<File> playlistModel;
    private JList<File> playlist;

    // Information labels
    private JLabel videoLabel;
    private JLabel statusLabel;
    private JLabel volumeLabel;

    // Playback components
    private VideoPlayer videoPlayer;
    private JSlider progressSlider;
    private JSlider volumeSlider;
    private Timer progressTimer;
    private boolean updatingProgress = false;

    // Full-screen state
    private boolean fullScreen = false;
    private int previousExtendedState;
    private java.awt.Rectangle previousBounds;

    public MainFrame() {
        setTitle("VPlay - Desktop Video Player");
        setSize(1100, 700);
        setMinimumSize(new Dimension(850, 550));
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBackground(BACKGROUND);
        mainPanel.setBorder(new EmptyBorder(15, 15, 15, 15));

        // Create the sections. createVideoArea() initializes VideoPlayer.
        mainPanel.add(createHeader(), BorderLayout.NORTH);
        mainPanel.add(createVideoArea(), BorderLayout.CENTER);
        mainPanel.add(createPlaylist(), BorderLayout.EAST);
        mainPanel.add(createControlPanel(), BorderLayout.SOUTH);

        setContentPane(mainPanel);

        // Start progress updates only after the slider and player exist.
        startProgressTimer();

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (progressTimer != null) {
                    progressTimer.stop();
                }
                if (videoPlayer != null) {
                    videoPlayer.release();
                }
                dispose();
            }
        });
    }

    // Application header
    private JPanel createHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(BABY_PINK);
        header.setBorder(new EmptyBorder(15, 20, 15, 20));

        JLabel title = new JLabel("VPlay");
        title.setFont(new Font("SansSerif", Font.BOLD, 28));
        title.setForeground(DARK_TEXT);

        JLabel subtitle = new JLabel("Your personal video player");
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 13));
        subtitle.setForeground(DARK_TEXT);

        JPanel textPanel = new JPanel();
        textPanel.setLayout(new javax.swing.BoxLayout(textPanel, javax.swing.BoxLayout.Y_AXIS));
        textPanel.setBackground(BABY_PINK);
        textPanel.add(title);
        textPanel.add(subtitle);

        header.add(textPanel, BorderLayout.WEST);
        return header;
    }

    // Video display area
    private JPanel createVideoArea() {
        JPanel videoPanel = new JPanel(new BorderLayout());
        videoPanel.setBackground(Color.BLACK);
        videoPanel.setBorder(BorderFactory.createLineBorder(BABY_PINK, 2));

        videoPlayer = new VideoPlayer();

        videoLabel = new JLabel("No video selected");
        videoLabel.setForeground(DARK_TEXT);
        videoLabel.setBorder(new EmptyBorder(10, 10, 10, 10));
        videoLabel.setBackground(BACKGROUND);
        videoLabel.setOpaque(true);

        videoPanel.add(videoPlayer, BorderLayout.CENTER);
        videoPanel.add(videoLabel, BorderLayout.SOUTH);
        return videoPanel;
    }

    // Playlist section
    private JPanel createPlaylist() {
        JPanel playlistPanel = new JPanel(new BorderLayout(5, 10));
        playlistPanel.setPreferredSize(new Dimension(240, 0));
        playlistPanel.setBackground(LIGHT_PINK);
        playlistPanel.setBorder(new EmptyBorder(12, 12, 12, 12));

        JLabel heading = new JLabel("My Playlist");
        heading.setFont(new Font("SansSerif", Font.BOLD, 18));
        heading.setForeground(DARK_TEXT);

        playlistModel = new DefaultListModel<>();
        playlist = new JList<>(playlistModel);
        playlist.setBackground(Color.WHITE);
        playlist.setForeground(DARK_TEXT);
        playlist.setFont(new Font("SansSerif", Font.PLAIN, 14));
        playlist.setSelectionBackground(BABY_PINK);
        playlist.setSelectionForeground(DARK_TEXT);

        // Show only the file name, not its full path.
        playlist.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(
                    JList<?> list, Object value, int index,
                    boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(
                        list, value, index, isSelected, cellHasFocus);
                if (value instanceof File) {
                    setText(((File) value).getName());
                }
                return this;
            }
        });

        // Double-click a playlist item to play it.
        playlist.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) {
                    playSelectedVideo();
                }
            }
        });

        playlist.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                File selected = playlist.getSelectedValue();
                if (selected != null && videoLabel != null) {
                    videoLabel.setText("Selected: " + selected.getName());
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(playlist);

        JButton openButton = createButton("＋ Open Video");
        openButton.addActionListener(e -> openVideo());

        playlistPanel.add(heading, BorderLayout.NORTH);
        playlistPanel.add(scrollPane, BorderLayout.CENTER);
        playlistPanel.add(openButton, BorderLayout.SOUTH);
        return playlistPanel;
    }

    // Playback controls
    private JPanel createControlPanel() {
        JPanel controls = new JPanel(new BorderLayout(10, 10));
        controls.setBackground(BABY_PINK);
        controls.setBorder(new EmptyBorder(12, 15, 12, 15));

        // Slider uses 0..1000 for finer seeking precision.
        progressSlider = new JSlider(0, 1000, 0);
        progressSlider.setBackground(BABY_PINK);
        progressSlider.setForeground(ACCENT);
        progressSlider.setEnabled(false);
        progressSlider.setToolTipText("Drag to seek through the video");

        progressSlider.addChangeListener(e -> {
            if (!updatingProgress
                    && !progressSlider.getValueIsAdjusting()
                    && videoPlayer != null
                    && playlist != null
                    && playlist.getSelectedValue() != null) {
                videoPlayer.setPosition(progressSlider.getValue() / 1000.0f);
            }
        });

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 5));
        buttonPanel.setBackground(BABY_PINK);

        JButton previousButton = createButton("Previous");
        JButton playButton = createButton("Play");
        JButton pauseButton = createButton("Pause");
        JButton nextButton = createButton("Next");
        JButton fullScreenButton = createButton("Full Screen");

        playButton.addActionListener(e -> {
            if (playlist.getSelectedValue() == null) {
                statusLabel.setText("Please open or select a video first.");
                return;
            }
            videoPlayer.resume();
            statusLabel.setText("Playing: " + playlist.getSelectedValue().getName());
        });

        pauseButton.addActionListener(e -> {
            if (playlist.getSelectedValue() == null) {
                statusLabel.setText("Please open or select a video first.");
                return;
            }
            videoPlayer.pause();
            statusLabel.setText("Video paused");
        });

        previousButton.addActionListener(e -> navigatePlaylist(-1));
        nextButton.addActionListener(e -> navigatePlaylist(1));
        fullScreenButton.addActionListener(e -> toggleFullScreen());

        buttonPanel.add(previousButton);
        buttonPanel.add(playButton);
        buttonPanel.add(pauseButton);
        buttonPanel.add(nextButton);
        buttonPanel.add(fullScreenButton);

        volumeSlider = new JSlider(0, 100, 70);
        volumeSlider.setPreferredSize(new Dimension(110, 35));
        volumeSlider.setBackground(BABY_PINK);
        volumeSlider.setForeground(ACCENT);

        volumeLabel = new JLabel("70%");
        volumeSlider.addChangeListener(e -> {
            int volume = volumeSlider.getValue();
            volumeLabel.setText(volume + "%");
            if (videoPlayer != null) {
                videoPlayer.setVolume(volume);
            }
        });

        JPanel volumePanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        volumePanel.setBackground(BABY_PINK);
        volumePanel.add(new JLabel("Volume"));
        volumePanel.add(volumeSlider);
        volumePanel.add(volumeLabel);

        statusLabel = new JLabel("Welcome to VPlay");
        statusLabel.setForeground(DARK_TEXT);

        controls.add(progressSlider, BorderLayout.NORTH);
        controls.add(buttonPanel, BorderLayout.CENTER);
        controls.add(volumePanel, BorderLayout.EAST);
        controls.add(statusLabel, BorderLayout.SOUTH);
        return controls;
    }

    // Reusable button styling
    private JButton createButton(String text) {
        JButton button = new JButton(text);
        button.setBackground(Color.WHITE);
        button.setForeground(DARK_TEXT);
        button.setFocusPainted(false);
        button.setFont(new Font("SansSerif", Font.BOLD, 12));
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ACCENT),
                new EmptyBorder(8, 12, 8, 12)));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return button;
    }

    // Open a video file
    private void openVideo() {
        JFileChooser fileChooser = new JFileChooser();
        FileNameExtensionFilter filter = new FileNameExtensionFilter(
                "Video Files", "mp4", "mkv", "avi", "mov", "wmv");
        fileChooser.setFileFilter(filter);

        int result = fileChooser.showOpenDialog(this);
        if (result != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File selectedFile = fileChooser.getSelectedFile();
        addToPlaylistAndPlay(selectedFile);
    }

    private void addToPlaylistAndPlay(File file) {
        if (file == null || !file.isFile() || !file.canRead()) {
            JOptionPane.showMessageDialog(
                    this, "The selected file cannot be read.",
                    "File Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Avoid duplicate entries in the playlist.
        boolean alreadyAdded = false;
        for (int i = 0; i < playlistModel.size(); i++) {
            if (playlistModel.get(i).equals(file)) {
                alreadyAdded = true;
                break;
            }
        }
        if (!alreadyAdded) {
            playlistModel.addElement(file);
        }

        playlist.setSelectedValue(file, true);
        playFile(file);
    }

    private void playSelectedVideo() {
        File selected = playlist.getSelectedValue();
        if (selected == null) {
            statusLabel.setText("Please select a video from the playlist.");
            return;
        }
        playFile(selected);
    }

    private void playFile(File file) {
        try {
            videoPlayer.play(file);
            videoLabel.setText(file.getName());
            statusLabel.setText("Now playing: " + file.getName());
            updatingProgress = true;
            progressSlider.setValue(0);
            updatingProgress = false;
            progressSlider.setEnabled(true);
        } catch (RuntimeException ex) {
            statusLabel.setText("Unable to play video.");
            JOptionPane.showMessageDialog(
                    this, "Unable to play video: " + ex.getMessage(),
                    "Playback Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // Update the progress slider while a video is playing.
    private void startProgressTimer() {
        progressTimer = new Timer(500, e -> {
            if (videoPlayer == null || progressSlider == null) {
                return;
            }

            // Do not fight the user while they are dragging the slider.
            if (progressSlider.getValueIsAdjusting()) {
                return;
            }

            float position = videoPlayer.getPosition();
            if (Float.isFinite(position) && position >= 0.0f && position <= 1.0f) {
                updatingProgress = true;
                progressSlider.setValue(Math.round(position * 1000));
                updatingProgress = false;
            }
        });
        progressTimer.start();
    }

    // Previous / Next playlist navigation
    private void navigatePlaylist(int direction) {
        if (playlistModel.isEmpty()) {
            statusLabel.setText("Your playlist is empty.");
            return;
        }

        int currentIndex = playlist.getSelectedIndex();
        int nextIndex;

        if (currentIndex == -1) {
            nextIndex = direction > 0 ? 0 : playlistModel.size() - 1;
        } else {
            nextIndex = currentIndex + direction;
        }

        if (nextIndex >= 0 && nextIndex < playlistModel.size()) {
            playlist.setSelectedIndex(nextIndex);
            playlist.ensureIndexIsVisible(nextIndex);
            playSelectedVideo();
        } else {
            statusLabel.setText("No more videos in this direction.");
        }
    }

    // Toggle the application window between normal and full-screen mode.
    private void toggleFullScreen() {
        GraphicsDevice device = GraphicsEnvironment
                .getLocalGraphicsEnvironment().getDefaultScreenDevice();

        if (!device.isFullScreenSupported()) {
            statusLabel.setText("Full-screen mode is not supported on this display.");
            return;
        }

        if (!fullScreen) {
            previousExtendedState = getExtendedState();
            previousBounds = getBounds();
            dispose();
            setUndecorated(true);
            setVisible(true);
            device.setFullScreenWindow(this);
            fullScreen = true;
        } else {
            device.setFullScreenWindow(null);
            dispose();
            setUndecorated(false);
            setExtendedState(previousExtendedState);
            if (previousBounds != null) {
                setBounds(previousBounds);
            }
            setVisible(true);
            fullScreen = false;
        }
    }
    private void applyWindowLogo() {
        URL logoUrl = getClass().getResource("/images/logo.png");
        if (logoUrl != null) {
            Image icon = new ImageIcon(logoUrl).getImage();
            setIconImage(icon);
        } else {
            System.err.println("Logo not found on classpath: /images/logo.png");
        }
    }
}
