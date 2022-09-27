import support.Song;

public class Queue {
    //<editor-fold desc="Atributos">
    /** Array contendo os objetos do tipo Song */
    private Song[] songs;
    /** Quantos songs tem atualmente na queue */
    private int queueLength;
    /** Capacidade máxima da queue */
    private int queueCap;
    private float[][] duracaoMusicas;
    private String[][] table;
    private int songPlayingIndex;
    //</editor-fold>

    public Queue() {
        this.songs = new Song[1];
        this.queueLength = 0;
        this.queueCap = 1;
        this.duracaoMusicas = new float[1][2];
        this.table = new String[1][6];
    }
    public void addSongToQueue(Song song){
        if(queueLength < queueCap){
            this.songs[queueLength] = song;
            this.table[queueLength][0] = song.getTitle();
            this.table[queueLength][1] = song.getAlbum();
            this.table[queueLength][2] = song.getArtist();
            this.table[queueLength][3] = song.getYear();
            this.table[queueLength][4] = song.getStrLength();
            this.table[queueLength][5] = song.getUuid();
            this.duracaoMusicas[queueLength][0] = song.getMsLength();
            this.duracaoMusicas[queueLength][1] = song.getMsPerFrame();

            this.queueLength++;
        } else{
            Song[] newSongs = new Song[queueCap*2];
            String[][] newTable = new String[queueCap*2][6];
            float[][] newDuracao = new float[queueCap*2][2];

            for (int i = 0; i < queueCap; i++){
                newSongs[i] = songs[i];
                newDuracao[i] = duracaoMusicas[i];
                newTable[i] = table[i];
            }
            queueCap *= 2;
            this.songs = newSongs;
            this.table = newTable;
            this.duracaoMusicas = newDuracao;
            addSongToQueue(song);
        }
    }
    public void removeSongFromQueue(int index){
        queueLength--;
        for(int i = index; i < (this.queueLength); i++){
            this.songs[i] = this.songs[i+1];
            this.table[i] = this.table[i+1];
            this.duracaoMusicas[i] = this.duracaoMusicas[i+1];
        }
        this.songs[queueLength] = null;
        this.table[queueLength] = new String[6];
        this.duracaoMusicas[queueLength] = new float[2];
    }
    public boolean existeProximaMusica(){
        try{
            return this.getTable()[this.getSongPlayingIndex()+ 1][0] != null;
        }
        catch (ArrayIndexOutOfBoundsException e){
            return false;
        }
    }
    public boolean existeMusicaAnterior(){
        try{
            return this.getTable()[this.getSongPlayingIndex() - 1][0] != null;
        }
        catch (ArrayIndexOutOfBoundsException e){
            return false;
        }
    }
    public String getSongID(int index){
        return table[index][5];
    }

    //<editor-fold desc="Getters and Setters">
    public int getDuracaoMusica(int index){
        return (int) duracaoMusicas[index][0];
    }
    public int getMsPerFrame( int index){
        return (int) duracaoMusicas[index][1];
    }
    public String[][] getTable() {
        return table;
    }
    public int getQueueLength() {
        return queueLength;
    }
    public Song getSong(int index){
        return songs[index];
    }
    public int getSongPlayingIndex() {
        return songPlayingIndex;
    }
    public void setSongPlayingIndex(int songPlayingIndex) {
        this.songPlayingIndex = songPlayingIndex;
    }
    //</editor-fold>
}
