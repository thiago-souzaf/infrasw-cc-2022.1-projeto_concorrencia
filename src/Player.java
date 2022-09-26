import com.mpatric.mp3agic.InvalidDataException;
import com.mpatric.mp3agic.UnsupportedTagException;
import javazoom.jl.decoder.*;
import javazoom.jl.player.AudioDevice;
import javazoom.jl.player.FactoryRegistry;
import support.PlayerWindow;
import support.Song;

import javax.swing.event.MouseInputAdapter;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.concurrent.Semaphore;

public class Player {
    Queue queue;
    /**
     * The MPEG audio bitstream.
     */
    private Bitstream bitstream;
    /**
     * The MPEG audio decoder.
     */
    private Decoder decoder;
    /**
     * The AudioDevice where audio samples are written to.
     */
    private AudioDevice device;
    private PlayerWindow window;
    private int currentFrame = 0;
    /** Semáforo binário usado para adicionar e remover músicas */
    Semaphore semaphore1;
    /** Semáforo binário usado para controlar o bitstream e device */
    Semaphore semaphore2;
    /** Semáforo binário usado para dar play e pause nas músicas*/
    Semaphore semaphore3;
    boolean free1; boolean free2;
    int alterna;
    int button;
    int stop = 0;
    boolean isButtonAbled;
    private final ActionListener buttonListenerPlayNow = e -> {
        String songID = window.getSelectedSong();
        alternarMusica(songID);
    };
    private final ActionListener buttonListenerRemove = e -> {
        new Thread(() -> {
            try {
                semaphore1.acquire();
                String songID = window.getSelectedSong();
                for (int i =0; i < queue.getQueueLength(); i++){
                    if (queue.getTable()[i][5].equals(songID)) {
                        queue.removeSongFromQueue(i);
                        break;
                    }
                }
                semaphore1.release();
            } catch (InterruptedException ex) {
                throw new RuntimeException(ex);
            }
            window.setQueueList(queue.getTable());
        }).start();
    };
    private final ActionListener buttonListenerAddSong = e -> {
        new Thread (() -> {
            try {
                semaphore1.acquire();
                Song currentSong = window.openFileChooser();
                if (currentSong != null){
                    queue.addSongToQueue(currentSong);
                }
                semaphore1.release();
            } catch (InvalidDataException | IOException | BitstreamException | UnsupportedTagException |
                     InterruptedException ex) {
                throw new RuntimeException(ex);
            }
            window.setQueueList(queue.getTable());
        }).start();
    };
    private final ActionListener buttonListenerPlayPause = e -> {
        if (button == window.BUTTON_ICON_PLAY){
            System.out.println("Resumir a música");
            semaphore3.release();
            ablePause();
        } else if (button == window.BUTTON_ICON_PAUSE) {
            System.out.println("Pausar música");
            try {
                semaphore3.acquire();
            } catch (InterruptedException ex) {
                throw new RuntimeException(ex);
            }
            ablePlay();
        }
    };
    private final ActionListener buttonListenerStop = e -> {
        stop = 1;
        if (button == window.BUTTON_ICON_PLAY && isButtonAbled){
            semaphore3.release();
        }
    };
    private final ActionListener buttonListenerNext = e -> {
        String songID = queue.getSongID(queue.getSongPlayingIndex()+1);
        alternarMusica(songID);
    };
    private final ActionListener buttonListenerPrevious = e -> {
        String songID = queue.getSongID(queue.getSongPlayingIndex()-1);
        alternarMusica(songID);
    };
    private final ActionListener buttonListenerShuffle = e -> {};
    private final ActionListener buttonListenerLoop = e -> {};
    private final MouseInputAdapter scrubberMouseInputAdapter = new MouseInputAdapter() {
        @Override
        public void mouseReleased(MouseEvent e) {
        }

        @Override
        public void mousePressed(MouseEvent e) {
        }

        @Override
        public void mouseDragged(MouseEvent e) {
        }
    };

    public Player() {
        EventQueue.invokeLater(() -> window = new PlayerWindow(
                "Reprodutor MP3",
                queue.getTable(),
                buttonListenerPlayNow,
                buttonListenerRemove,
                buttonListenerAddSong,
                buttonListenerShuffle,
                buttonListenerPrevious,
                buttonListenerPlayPause,
                buttonListenerStop,
                buttonListenerNext,
                buttonListenerLoop,
                scrubberMouseInputAdapter)
        );
        semaphore1 = new Semaphore(1);
        semaphore2 = new Semaphore(1);
        semaphore3 = new Semaphore(1);
        queue = new Queue();
        alterna = 0;
        free1 = true;
        free2 = true;
        button = 0;
        isButtonAbled = false;
    }

    //<editor-fold desc="Essential">

    /**
     * @return False if there are no more frames to play.
     */
    private boolean playNextFrame() throws JavaLayerException {
        // TODO Is this thread safe?
        if (device != null) {
            Header h = bitstream.readFrame();
            if (h == null) return false;

            SampleBuffer output = (SampleBuffer) decoder.decodeFrame(h, bitstream);
            device.write(output.getBuffer(), 0, output.getBufferLength());
            bitstream.closeFrame();
        }
        return true;
    }

    /**
     * @return False if there are no more frames to skip.
     */
    private boolean skipNextFrame() throws BitstreamException {
        // TODO Is this thread safe?
        Header h = bitstream.readFrame();
        if (h == null) return false;
        bitstream.closeFrame();
        currentFrame++;
        return true;
    }

    /**
     * Skips bitstream to the target frame if the new frame is higher than the current one.
     *
     * @param newFrame Frame to skip to.
     * @throws BitstreamException Generic Bitstream exception.
     */
    private void skipToFrame(int newFrame) throws BitstreamException {
        // TODO Is this thread safe?
        if (newFrame > currentFrame) {
            int framesToSkip = newFrame - currentFrame;
            boolean condition = true;
            while (framesToSkip-- > 0 && condition) condition = skipNextFrame();
        }
    }

    private void tocarMusica(int alt, String songID) throws InterruptedException, JavaLayerException, FileNotFoundException{
        int duracaoMus = 0;
        int msPerFrame= 0;
        for (int i = 0; i < queue.getQueueLength(); i++) {
            if (queue.getTable()[i][5].equals(songID)) {
                queue.setSongPlayingIndex(i);
                ablePreviousNext(i);
                String[] info = queue.getTable()[i];
                semaphore2.acquire();
                currentFrame = 0;
                alterna = alt;
                try {
                    if (this.device.isOpen()) {
                        this.device.close();
                        this.bitstream.close();
                    }
                } catch (NullPointerException exc) {
                    System.out.println("nenhum device existente");
                }
                this.device = FactoryRegistry.systemRegistry().createAudioDevice();
                this.device.open(this.decoder = new Decoder());
                this.bitstream = new Bitstream(queue.getSong(i).getBufferedInputStream());
                semaphore2.release();
                window.setPlayingSongInfo(info[0], info[1], info[2]);
                duracaoMus = queue.getDuracaoMusica(i);
                msPerFrame = queue.getMsPerFrame(i);
                break;
            }
        }
        window.setEnabledStopButton(true);
        ablePause();
        boolean b = this.playNextFrame();
        while (b){
            semaphore2.acquire();
            semaphore3.acquire();
            if(alterna == alt) {
                b = this.playNextFrame();
                semaphore2.release();
                semaphore3.release();
                currentFrame++;
                window.setTime(currentFrame*msPerFrame, duracaoMus);
            } else{
                semaphore2.release();
                semaphore3.release();
                break;
            }

            if (stop == 1){
                stop = 0;
                window.resetMiniPlayer();
                button = window.BUTTON_ICON_PLAY;
                isButtonAbled = false;
                break;
            }
        }
        if (alterna == alt){ // Quando a música sendo reproduzida acaba, entra nesse if
            if (existeProximaMusica(queue.getSongPlayingIndex())){
                alternarMusica(queue.getSongID(queue.getSongPlayingIndex()+1)); // Toca a próxima música da lista
            }else{
                window.resetMiniPlayer(); // Não tem próxima música -> reseta o miniplayer
            }
        }
    }
    private void tocarMusicaSelecionada(int alt) throws InterruptedException, JavaLayerException, FileNotFoundException{
        int duracaoMus = 0;
        int msPerFrame= 0;
        String songID = window.getSelectedSong();
        for (int i = 0; i < queue.getQueueLength(); i++) {
            if (queue.getTable()[i][5].equals(songID)) {
                ablePreviousNext(i);
                String[] info = queue.getTable()[i];
                semaphore2.acquire();
                currentFrame = 0;
                alterna = alt;
                try {
                    if (this.device.isOpen()) {
                        this.device.close();
                        this.bitstream.close();
                    }
                } catch (NullPointerException exc) {
                    System.out.println("nenhum device existente");
                }
                this.device = FactoryRegistry.systemRegistry().createAudioDevice();
                this.device.open(this.decoder = new Decoder());
                this.bitstream = new Bitstream(queue.getSong(i).getBufferedInputStream());
                semaphore2.release();
                window.setPlayingSongInfo(info[0], info[1], info[2]);
                duracaoMus = queue.getDuracaoMusica(i);
                msPerFrame = queue.getMsPerFrame(i);
                break;
            }
        }
        window.setEnabledStopButton(true);
        ablePause();
        boolean b = this.playNextFrame();
        while (b){
            semaphore2.acquire();
            semaphore3.acquire();
            if(alterna == alt) {
                b = this.playNextFrame();
                semaphore2.release();
                semaphore3.release();
                currentFrame++;
                window.setTime(currentFrame*msPerFrame, duracaoMus);
            } else{
                semaphore2.release();
                semaphore3.release();
                break;
            }

            if (stop == 1){
                stop = 0;
                window.resetMiniPlayer();
                button = window.BUTTON_ICON_PLAY;
                isButtonAbled = false;
                break;
            }
        }
        if (alterna == alt){
            window.resetMiniPlayer();
        }
    }

    // Controladores dos botões
    private void ablePlay(){
        window.setEnabledPlayPauseButton(true);
        window.setPlayPauseButtonIcon(window.BUTTON_ICON_PLAY);
        button = window.BUTTON_ICON_PLAY;
        isButtonAbled = true;

    }
    private void ablePause(){
        window.setEnabledPlayPauseButton(true);
        window.setPlayPauseButtonIcon(window.BUTTON_ICON_PAUSE);
        button = window.BUTTON_ICON_PAUSE;
        isButtonAbled = true;
    }
    private void ablePreviousNext(int songIndex){
        try{
            window.setEnabledPreviousButton(queue.getTable()[songIndex - 1][0] != null);
        }
        catch (ArrayIndexOutOfBoundsException e){
            window.setEnabledPreviousButton(false);
        }
        try{
            window.setEnabledNextButton(queue.getTable()[songIndex + 1][0] != null);
        }
        catch (ArrayIndexOutOfBoundsException e) {
            window.setEnabledNextButton(false);
        }
    }

    private boolean existeProximaMusica(int songIndex){
        try{
            return queue.getTable()[songIndex + 1][0] != null;
        }
        catch (ArrayIndexOutOfBoundsException e){
            return false;
        }
    }
    private void alternarMusica(String songID){
        Thread t1 = new Thread(() -> {
            try {
                free1 = false;
                tocarMusica(1, songID);
                free1 = true;
            } catch (JavaLayerException | FileNotFoundException | InterruptedException ex) {
                throw new RuntimeException(ex);
            }
        });
        Thread t2 = new Thread(() -> {
            try {
                free2 = false;
                tocarMusica(0, songID);
                free2 = true;
            } catch (JavaLayerException | FileNotFoundException | InterruptedException ex) {
                throw new RuntimeException(ex);
            }
        });
        if (alterna == 0 && free1){
            t1.start();
            System.out.println("t1 iniciada");
        } else if (alterna == 1 && free2){
            t2.start();
            System.out.println("t2 iniciada");
        }
        if (button == window.BUTTON_ICON_PLAY && isButtonAbled){
            semaphore3.release();
        }
    }

    //</editor-fold>
}
