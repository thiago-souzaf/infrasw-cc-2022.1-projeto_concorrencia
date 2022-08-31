import support.Song;

public class Queue {
    private Song[] songs;
    private int queueLength;
    public int getQueueLength() {
        return queueLength;
    }
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
    public Song getSong(int index){
        return songs[index];
    }
}
