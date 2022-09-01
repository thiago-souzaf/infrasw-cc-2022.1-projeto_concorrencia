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
    private String[][] table;
    public String[][] getTable() {
        return table;
    }
    public Queue() {
        this.queueLength = 0;
        this.queueCap = 1;
        this.table = new String[1][6];
        this.songs = new Song[1];
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
            
            this.queueLength++;
        } else{
            Song[] newSongs = new Song[queueCap*2];
            String[][] newTable = new String[queueCap*2][6];

            for (int i = 0; i < queueCap; i++){
                newSongs[i] = songs[i];
                for (int j = 0; j < 6; j++){
                    newTable[i][j] = table[i][j];
                }
            }
            queueCap *= 2;
            this.songs = newSongs;
            this.table = newTable;
            addSongToQueue(song);
        }
    }

    public void removeSongFromQueue(int index){
        queueLength--;
        for(int i = index; i < (this.queueLength); i++){
            this.songs[i] = this.songs[i+1];
            this.table[i][0] = this.table[i+1][0];
            this.table[i][1] = this.table[i+1][1];
            this.table[i][2] = this.table[i+1][2];
            this.table[i][3] = this.table[i+1][3];
            this.table[i][4] = this.table[i+1][4];
            this.table[i][5] = this.table[i+1][5];
        }
        this.songs[queueLength] = null;
        this.table[queueLength][0] = null;
        this.table[queueLength][1] = null;
        this.table[queueLength][2] = null;
        this.table[queueLength][3] = null;
        this.table[queueLength][4] = null;
        this.table[queueLength][5] = null;
    }
    public Song getSong(int index){
        return songs[index];
    }
}
