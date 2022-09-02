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
    boolean free1; boolean free2;
    int alterna;
    private final ActionListener buttonListenerPlayNow = e -> {

        Thread t1 = new Thread(() -> {
            try {
                free1 = false;
                tocarMusicaSelecionada(1);
                free1 = true;
            } catch (JavaLayerException | FileNotFoundException | InterruptedException ex) {
                throw new RuntimeException(ex);
            }
        });
        Thread t2 = new Thread(() -> {
            try {
                free2 = false;
                tocarMusicaSelecionada(0);
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
                queue.addSongToQueue(currentSong);
                semaphore1.release();
            } catch (InvalidDataException | IOException | BitstreamException | UnsupportedTagException |
                     InterruptedException ex) {
                throw new RuntimeException(ex);
            }
            window.setQueueList(queue.getTable());
        }).start();
    };

    private final ActionListener buttonListenerPlayPause = e -> {};
    private final ActionListener buttonListenerStop = e -> {};
    private final ActionListener buttonListenerNext = e -> {};
    private final ActionListener buttonListenerPrevious = e -> {};
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
        queue = new Queue();
        alterna = 0;
        free1 = true;
        free2 = true;
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

    private void tocarMusicaSelecionada(int alt) throws InterruptedException, JavaLayerException, FileNotFoundException{
        int duracaoMus = 0;
        int msPerFrame= 0;
        String songID = window.getSelectedSong();
        for (int i = 0; i < queue.getQueueLength(); i++) {
            if (queue.getTable()[i][5].equals(songID)) {
                String[] info = queue.getTable()[i];
                Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
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
                Thread.currentThread().setPriority(Thread.NORM_PRIORITY);
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
        boolean b = this.playNextFrame();
        while (b){
            semaphore2.acquire();
            if(alterna == alt) {
                b = this.playNextFrame();
                semaphore2.release();
                currentFrame++;
                window.setTime(currentFrame*msPerFrame, duracaoMus);
            } else{
                semaphore2.release();
                break;
            }
        }
    }

    //</editor-fold>
}
