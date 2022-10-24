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
    Semaphore semaphoreAddRemoveSong;

    /** Semáforo binário usado para controlar o bitstream e device */
    Semaphore semaphoreBitstream;

    /** Semáforo binário usado para dar play e pause nas músicas*/
    Semaphore semaphorePlayPause;

    /** Semáforo binário usado para controlar o scrubber*/
    Semaphore semaphoreScrubber;

    Semaphore thread1;
    Semaphore thread2;

    int alterna;
    int button;
    int stop = 0;
    boolean isButtonAble;
    private final ActionListener buttonListenerPlayNow = e -> {
        String songID = window.getSelectedSong(); // Pega o songID da música selecionada no player
        alternarMusica(songID); // Chama o método que gerencia a troca de música
    };
    private final ActionListener buttonListenerRemove = e -> new Thread(() -> {
        try {
            semaphoreAddRemoveSong.acquire();
            String songID = window.getSelectedSong();
            if (songID.equals(queue.getSongID(queue.getSongPlayingIndex()))){ // Se a música a ser removida está sendo reproduiza no momento:
                stop = 1;
            }

            queue.removeSongFromQueue(songID);
            enablePreviousNext();
            ativarShuffle();
            semaphoreAddRemoveSong.release();
        } catch (InterruptedException ex) {
            throw new RuntimeException(ex);
        }
        window.setQueueList(queue.getTable());
    }).start();
    private final ActionListener buttonListenerAddSong = e -> new Thread (() -> {
        try {
            semaphoreAddRemoveSong.acquire();
            Song currentSong = window.openFileChooser();
            if (currentSong != null){ // Só adiciona na queue se alguma música foi selecionada
                queue.addSongToQueue(currentSong);
            }
            semaphoreAddRemoveSong.release();
        } catch (InvalidDataException | IOException | BitstreamException | UnsupportedTagException |
                 InterruptedException ex) {
            throw new RuntimeException(ex);
        }
        enablePreviousNext(); // Após inserir a música na queue, verifica se tem músicas antes e depois da música sendo tocada
        ativarShuffle();
        window.setQueueList(queue.getTable());
    }).start();
    private final ActionListener buttonListenerPlayPause = e -> {
        if (button == window.BUTTON_ICON_PAUSE){
            try {
                semaphorePlayPause.acquire(); // Quando aperta o botão pause → Pega o semáforo necessário para a música continuar tocando
            } catch (InterruptedException ex) {
                throw new RuntimeException(ex);
            }
            enablePlay(); //
        } else if (button == window.BUTTON_ICON_PLAY) {
            semaphorePlayPause.release(); // Quando aperta o botão play → Libera o semáforo necessário para a música continuar tocando
            enablePause();
        }
    };
    private final ActionListener buttonListenerStop = e -> {
        stop = 1;
        if (button == window.BUTTON_ICON_PLAY && isButtonAble){ // Se estiver em pause, e apertar no ‘stop’ → Libera o semáforo.
            semaphorePlayPause.release();
        }
    };
    private final ActionListener buttonListenerNext = e -> {
        String songID = queue.getSongID(queue.getSongPlayingIndex()+1); // Pega o songID da próxima música na lista.
        alternarMusica(songID);
    };
    private final ActionListener buttonListenerPrevious = e -> {
        String songID = queue.getSongID(queue.getSongPlayingIndex()-1); // Pega o songID da música anterior na lista.
        alternarMusica(songID);
    };
    private final ActionListener buttonListenerShuffle = e -> {
        if (isButtonAble) { // Se tiver uma música tocando, chama o método com index = 1
            window.setQueueList(queue.shuflle(1));
            enablePreviousNext();
        }
        else window.setQueueList(queue.shuflle(0)); // Se não tiver, chama com index = 0
    };
    private final ActionListener buttonListenerLoop = e -> {};
    private final MouseInputAdapter scrubberMouseInputAdapter = new MouseInputAdapter() {
        @Override
        public void mouseReleased(MouseEvent e) {
            new Thread(() -> {
                int timeMs = window.getScrubberValue(); // Tempo em milisegundos retornado pelo scrubber ao soltar o mouse.
                int msPerFrame = queue.getMsPerFrame(queue.getSongPlayingIndex()); // Quantos milisegundos tem em um frame da música que está em reprodução.
                int newFrame = timeMs/msPerFrame; // Novo frame para qual a música deve pular.
                try {
                    skipToFrame(newFrame);
                } catch (BitstreamException ex) {
                    throw new RuntimeException(ex);
                }
                semaphoreScrubber.release();
            }).start();
        }

        @Override
        public void mousePressed(MouseEvent e) {
            try {
                semaphoreScrubber.acquire(); // Semáforo para pausar a reprodução da música enquanto o scrubber estiver sendo arrastado.
            } catch (InterruptedException ex) {
                throw new RuntimeException(ex);
            }
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            int timeMs = window.getScrubberValue(); // Tempo em milisegundos retornado pelo scrubber ao arrastar o mouse.
            int msPerFrame = queue.getMsPerFrame(queue.getSongPlayingIndex()); // Quantos milisegundos tem em um frame da música que está em reprodução.
            int newFrame = timeMs/msPerFrame; // Novo frame para qual a música deve pular.
            window.setTime(newFrame*queue.getMsPerFrame(queue.getSongPlayingIndex()), queue.getDuracaoMusica(queue.getSongPlayingIndex()));
            // O mouseDragged só vai mostrar o tempo em segundos no miniplayer, não vai alterar a execução da música.
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
        semaphoreAddRemoveSong = new Semaphore(1);
        semaphoreBitstream = new Semaphore(1);
        semaphorePlayPause = new Semaphore(1);
        semaphoreScrubber = new Semaphore(1);
        thread1 = new Semaphore(1);
        thread2 = new Semaphore(1);
        queue = new Queue();
        alterna = 0;
        button = 0;
        isButtonAble = false;
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

        } else{ // Se o scrubber for usado para voltar a música, reinicia a música sendo tocada e passa o novo frame como parâmetro.
            alternarMusica(queue.getSongID(queue.getSongPlayingIndex()), newFrame);
        }
        window.setTime(newFrame*queue.getMsPerFrame(queue.getSongPlayingIndex()), queue.getDuracaoMusica(queue.getSongPlayingIndex()));
    }

    /**
     * Dá play na música indicada pelo songID e a música começa do início.
     *
     * @param alt indica qual thread chamou o método (pode ser 0 ou 1)
     * @param songID String contendo o ID da música
     */
    private void tocarMusica(int alt, String songID) throws InterruptedException, JavaLayerException, FileNotFoundException{
        int duracaoMus = 0;
        int msPerFrame= 0;
        for (int i = 0; i < queue.getQueueLength(); i++) { // Loop para achar a música na tabela
            if (queue.getTable()[i][5].equals(songID)) {
                queue.setSongPlayingIndex(i);
                enablePreviousNext(); // Verifica se tem música antes e/ou depois para ativar os botões de 'previous' e 'next'.
                String[] info = queue.getTable()[i]; //
                currentFrame = 0;
                alterna = alt;
                semaphoreBitstream.acquire();
                try { // Se já tiver um device e bitstream aberto, aqui serão fechados.
                    if (this.device.isOpen()) {
                        this.device.close();
                        this.bitstream.close();
                    }
                } catch (NullPointerException ignored) {
                }
                this.device = FactoryRegistry.systemRegistry().createAudioDevice();
                this.device.open(this.decoder = new Decoder());
                this.bitstream = new Bitstream(queue.getSong(i).getBufferedInputStream());
                semaphoreBitstream.release();
                window.setPlayingSongInfo(info[0], info[1], info[2]); // Coloca o título, album e artista da música que estiver tocando no player.
                duracaoMus = queue.getDuracaoMusica(i);
                msPerFrame = queue.getMsPerFrame(i);
                break;
            }
        }
        window.setEnabledStopButton(true);
        enablePause();
        window.setEnabledScrubber(true);
        boolean b = this.playNextFrame();
        while (b){
            semaphorePlayPause.acquire();
            semaphoreBitstream.acquire();
            if(alterna == alt) {
                b = this.playNextFrame(); // Chama esse método em loop para reproduzir a música
                semaphorePlayPause.release();
                semaphoreBitstream.release();
                currentFrame++;
                semaphoreScrubber.acquire();
                window.setTime(currentFrame*msPerFrame, duracaoMus);
                semaphoreScrubber.release();
            } else{
                semaphoreBitstream.release();
                semaphorePlayPause.release();
                break;
            }

            if (stop == 1){
                stop = 0;
                window.resetMiniPlayer();
                button = window.BUTTON_ICON_PLAY;
                isButtonAble = false;
                break;
            }
        }
        if (alterna == alt && isButtonAble){ // Quando a música sendo reproduzida acaba, entra nesse if
            if (queue.existeProximaMusica()){
                alternarMusica(queue.getSongID(queue.getSongPlayingIndex()+1)); // Toca a próxima música da lista
            }else{
                window.resetMiniPlayer(); // Não tem próxima música → reseta o miniplayer
            }
        }
    }

    /**
     * Dá play na música indicada pelo songID e a música começa a partir do newFrame.
     * Usado apenas quando o scrubber volta na música
     *
     * @param alt indica qual thread chamou o método (pode ser 0 ou 1)
     * @param songID String contendo o ID da música
     * @param newFrame Frame para qual irá pular.
     */
    private void tocarMusica(int alt, String songID, int newFrame) throws InterruptedException, JavaLayerException, FileNotFoundException{
        int duracaoMus = 0;
        int msPerFrame= 0;
        boolean isPaused = true;
        for (int i = 0; i < queue.getQueueLength(); i++) {
            if (queue.getTable()[i][5].equals(songID)) {
                queue.setSongPlayingIndex(i);
                enablePreviousNext();
                String[] info = queue.getTable()[i];
                currentFrame = 0;
                alterna = alt;
                isPaused = semaphorePlayPause.availablePermits() == 0;
                if(isPaused){ // Se a música estiver pausada quando o método tocarMusica() for chamado, ele libera os semáforos seguintes para conseguir encerrar a thread anterior.
                    semaphoreBitstream.release();
                    semaphorePlayPause.release();
                }
                semaphoreBitstream.acquire();
                try {
                    if (this.device.isOpen()) {
                        this.device.close();
                        this.bitstream.close();
                    }
                } catch (NullPointerException ignored) {
                }
                this.device = FactoryRegistry.systemRegistry().createAudioDevice();
                this.device.open(this.decoder = new Decoder());
                this.bitstream = new Bitstream(queue.getSong(i).getBufferedInputStream());
                semaphoreBitstream.release();
                window.setPlayingSongInfo(info[0], info[1], info[2]);
                duracaoMus = queue.getDuracaoMusica(i);
                msPerFrame = queue.getMsPerFrame(i);
                break;
            }
        }
        if(isPaused){ // Se a música estiver pausada quando chamar o método tocarMusica(), então a música volta a ser pausada após criar o device e bitstream.
            semaphorePlayPause.acquire();
        }
        window.setEnabledStopButton(true);
        window.setEnabledScrubber(true);
        skipToFrame(newFrame);
        boolean b = this.playNextFrame();
        while (b){
            semaphorePlayPause.acquire();
            semaphoreBitstream.acquire();
            if(alterna == alt) {
                b = this.playNextFrame();
                semaphoreBitstream.release();
                semaphorePlayPause.release();
                currentFrame++;
                semaphoreScrubber.acquire();
                window.setTime(currentFrame*msPerFrame, duracaoMus);
                semaphoreScrubber.release();
            } else{
                semaphoreBitstream.release();
                semaphorePlayPause.release();
                break;
            }

            if (stop == 1){
                stop = 0;
                window.resetMiniPlayer();
                button = window.BUTTON_ICON_PLAY;
                isButtonAble = false;
                break;
            }
        }
        if (alterna == alt && isButtonAble){ // Quando a música sendo reproduzida acaba, entra nesse if
            if (queue.existeProximaMusica()){
                alternarMusica(queue.getSongID(queue.getSongPlayingIndex()+1)); // Toca a próxima música da lista
            }else{
                window.resetMiniPlayer(); // Não tem próxima música → reseta o miniplayer
            }
        }
    }

    // Controladores dos botões
    private void enablePlay(){
        window.setEnabledPlayPauseButton(true);
        window.setPlayPauseButtonIcon(window.BUTTON_ICON_PLAY);
        button = window.BUTTON_ICON_PLAY;
        isButtonAble = true;

    }
    private void enablePause(){
        window.setEnabledPlayPauseButton(true);
        window.setPlayPauseButtonIcon(window.BUTTON_ICON_PAUSE);
        button = window.BUTTON_ICON_PAUSE;
        isButtonAble = true;
    }
    private void enablePreviousNext(){
        window.setEnabledNextButton(queue.existeProximaMusica());
        window.setEnabledPreviousButton(queue.existeMusicaAnterior());
    }

    /**
     * Alterna entre duas threads para tocar a música correspondente ao songID passado como parâmetro.
     *
     * @param songID String contendo o ID da música
     */
    private void alternarMusica(String songID){
        Thread t1 = new Thread(() -> {
            try {
                tocarMusica(1, songID);
                thread1.release();
            } catch (JavaLayerException | FileNotFoundException | InterruptedException ex) {
                throw new RuntimeException(ex);
            }
        });
        Thread t2 = new Thread(() -> {
            try {
                tocarMusica(0, songID);
                thread2.release();
            } catch (JavaLayerException | FileNotFoundException | InterruptedException ex) {
                throw new RuntimeException(ex);
            }
        });
        if (alterna == 0){
            if (thread1.tryAcquire()){
                t1.start();
            }
        } else if (alterna == 1){
            if (thread2.tryAcquire()){
                t2.start();
            }
        }
        if (button == window.BUTTON_ICON_PLAY && isButtonAble){
            semaphorePlayPause.release();
        }
    }

    /**
     * Alterna entre duas threads para tocar a música correspondente ao songID passado como parâmetro. Usado quando o scrubber pula para um frame menor que o currentFrame.
     *
     * @param songID String contendo o ID da música.
     * @param newFrame Frame para qual irá pular.
     */
    private void alternarMusica(String songID, int newFrame){
        Thread t1 = new Thread(() -> {
            try {
                tocarMusica(1, songID, newFrame);
                thread1.release();
            } catch (JavaLayerException | FileNotFoundException | InterruptedException ex) {
                throw new RuntimeException(ex);
            }
        });
        Thread t2 = new Thread(() -> {
            try {
                tocarMusica(0, songID, newFrame);
                thread2.release();
            } catch (JavaLayerException | FileNotFoundException | InterruptedException ex) {
                throw new RuntimeException(ex);
            }
        });
        if (alterna == 0){
            if (thread1.tryAcquire()){
                t1.start();
            }
        } else if (alterna == 1){
            if (thread2.tryAcquire()){
                t2.start();
            }
        }
    }

    /**
    * Habilita o botão "shuffle" se tiver mais de 2 músicas na lista de reprodução, caso contrário desabilita.
    */
    private void ativarShuffle(){
        if(queue.getQueueLength() >= 2){
            window.setEnabledShuffleButton(true);
        }else{
            window.setEnabledShuffleButton(false);
        }
    }
    //</editor-fold>
}
