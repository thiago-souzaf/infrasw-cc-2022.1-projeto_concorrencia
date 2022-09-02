import support.Song;

public class Queue {
    /** Array contendo os objetos do tipo Song */
    private Song[] songs;
    /** Quantos songs tem atualmente na queue */
    private int queueLength;
    public int getQueueLength() {
        return queueLength;
    }
    /** Capacidade máxima da queue */
    private int queueCap;
    private float[][] duracaoMusicas;
    public int getDuracaoMusica(int index){
        return (int) duracaoMusicas[index][0];
    }
    public int getMsPerFrame( int index){
        return (int) duracaoMusicas[index][1];
    }
    private String[][] table;
    public String[][] getTable() {
        return table;
    }
    public Queue() {
        this.queueLength = 0;
        this.queueCap = 1;
        this.table = new String[1][6];
        this.songs = new Song[1];
        this.duracaoMusicas = new float[1][2];
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
        this.table[queueLength] = null;
        this.duracaoMusicas[queueLength] = null;
    }
    public Song getSong(int index){
        return songs[index];
    }
}
