import support.Song;

public class Queue {
    //<editor-fold desc="Atributos">
    /** Array contendo os objetos do tipo Song */
    private Song[] songs;
    /** Array contendo os objetos do tipo Song embaralhados*/
    private Song[] shuffledSongs;
    /** Quantos songs tem atualmente na queue */
    private int queueLength;
    /** Capacidade máxima da queue */
    private int queueCap;
    private float[][] duracaoMusicas;
    private float[][] shuffledDuracaoMusicas;
    private String[][] table;
    private String[][] shuffledTable;
    private int songPlayingIndex;
    private boolean isShuffled;
    //</editor-fold>

    public Queue() {
        this.songs = new Song[1];
        this.shuffledSongs = new Song[1];
        this.queueLength = 0;
        this.queueCap = 1;
        this.duracaoMusicas = new float[1][2];
        this.shuffledDuracaoMusicas = new float[1][2];
        this.table = new String[1][6];
        this.shuffledTable = new String[1][6];
        this.isShuffled = false;
    }
    public void addSongToQueue(Song song){
        if(queueLength < queueCap){
            // Adicionando na lista de reprodução original
            this.songs[queueLength] = song;
            this.table[queueLength][0] = song.getTitle();
            this.table[queueLength][1] = song.getAlbum();
            this.table[queueLength][2] = song.getArtist();
            this.table[queueLength][3] = song.getYear();
            this.table[queueLength][4] = song.getStrLength();
            this.table[queueLength][5] = song.getUuid();
            this.duracaoMusicas[queueLength][0] = song.getMsLength();
            this.duracaoMusicas[queueLength][1] = song.getMsPerFrame();

            // Adicionando na lista de reprodução com shuffle
            if(isShuffled){
                this.shuffledSongs[queueLength] = song;
                this.shuffledTable[queueLength][0] = song.getTitle();
                this.shuffledTable[queueLength][1] = song.getAlbum();
                this.shuffledTable[queueLength][2] = song.getArtist();
                this.shuffledTable[queueLength][3] = song.getYear();
                this.shuffledTable[queueLength][4] = song.getStrLength();
                this.shuffledTable[queueLength][5] = song.getUuid();
                this.shuffledDuracaoMusicas[queueLength][0] = song.getMsLength();
                this.shuffledDuracaoMusicas[queueLength][1] = song.getMsPerFrame();
            }

            this.queueLength++;
        } else{
            Song[] newSongs = new Song[queueCap*2];
            String[][] newTable = new String[queueCap*2][6];
            float[][] newDuracao = new float[queueCap*2][2];
            shuffledTable = new String[queueCap*2][6];
            shuffledSongs = new Song[queueCap*2];
            shuffledDuracaoMusicas = new float[queueCap*2][2];

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
    public void removeSongFromQueue(String songID){
        for (int i = 0; i < this.getQueueLength(); i++){ // Entra nesse "for" para achar o songID na tabela de músicas original
            if (this.table[i][5].equals(songID)) {
                queueLength--;
                for(int j = i; j < this.getQueueLength(); j++){ // Entra nesse "for" para excluir a música selecionada, e as músicas seguintes na ordem vão subir na lista.
                    this.songs[j] = this.songs[j+1];
                    this.table[j] = this.table[j+1];
                    this.duracaoMusicas[j] = this.duracaoMusicas[j+1];
                }

                this.songs[queueLength] = null;
                this.table[queueLength] = new String[6];
                this.duracaoMusicas[queueLength] = new float[2];

                break;
            }
        }

        if(isShuffled){
            for (int i = 0; i < this.getQueueLength()+1; i++){ // Entra nesse "for" para achar o songID na tabela de músicas com shuffle
                if (this.shuffledTable[i][5].equals(songID)) {
                    for(int j = i; j < this.getQueueLength(); j++){ // Entra nesse "for" para excluir a música selecionada, e as músicas seguintes na ordem vão subir na lista.
                        this.shuffledSongs[j] = this.shuffledSongs[j+1];
                        this.shuffledTable[j] = this.shuffledTable[j+1];
                        this.shuffledDuracaoMusicas[j] = this.shuffledDuracaoMusicas[j+1];
                    }
                    this.shuffledSongs[queueLength] = null;
                    this.shuffledTable[queueLength] = new String[6];
                    this.shuffledDuracaoMusicas[queueLength] = new float[2];

                    break;
                }
            }
        }
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

    /**
     * Método usado pelo shuffle para trocar duas músicas de posição na lista de reprodução aleatória
     * @param index1 índice da música 1 que será trocada
     * @param index2 índice da música 2 que será trocada*/
    private void permutarMusicas(int index1, int index2){
        Song tempSong = this.shuffledSongs[index1];
        String[] tempTable = this.shuffledTable[index1];
        float[] tempDur = this.shuffledDuracaoMusicas[index1];

        this.shuffledSongs[index1] = this.shuffledSongs[index2];
        this.shuffledTable[index1] = this.shuffledTable[index2];
        this.shuffledDuracaoMusicas[index1] = this.shuffledDuracaoMusicas[index2];

        this.shuffledSongs[index2] = tempSong;
        this.shuffledTable[index2] = tempTable;
        this.shuffledDuracaoMusicas[index2] = tempDur;
    }

    /**
     * Método usado para embaralhar ou desembaralhar a ordem das músicas na lista de reprodução
     * @param index índice que vai indicar se existe uma música sendo reproduzida (index = 1) ou não (index = 0)*/
    public String[][] shuflle(int index){

        if (isShuffled){ // Se já estiver aleatório, então volta ao estado original
            String songID = getSongID(getSongPlayingIndex());
            for(int i = 0; i < getQueueLength(); i++){
                if (songID != null && songID.equals(this.table[i][5])) {
                    setSongPlayingIndex(i); // Define o songPlayingIndex para o valor certo correspondente à lista original
                    break;
                }
            }
            isShuffled = false;
            return this.getTable();
        }
        else{
            String songID;
            if (index == 1) { // Se já tiver uma música tocando, então vai entrar nesse if para colocar ela no topo da lista.
                songID = getSongID(getSongPlayingIndex()); // ID da música que está tocando
                for (int i = 0; i < this.getQueueLength(); i++){ // Copia a lista de reprodução original (não aleatória) para a outra lista que vai ser embaralhada
                    this.shuffledTable[i] = this.table[i];
                    this.shuffledSongs[i] = this.songs[i];
                    this.shuffledDuracaoMusicas[i] = this.duracaoMusicas[i];

                    if (songID.equals(shuffledTable[i][5])){ // Aqui coloca a música que está tocando no topo na lista de reproudução
                        permutarMusicas(i, 0);
                        setSongPlayingIndex(0);
                    }
                }
            } else { // Se não tiver nenhuma música tocando, então a música que estará no topo da lista será aleatória.
                for (int i = 0; i < this.getQueueLength(); i++){ // Copia a lista de reprodução original (não aleatória) para a outra lista que vai ser embaralhada
                    this.shuffledTable[i] = this.table[i];
                    this.shuffledSongs[i] = this.songs[i];
                    this.shuffledDuracaoMusicas[i] = this.duracaoMusicas[i];
                }
            }

            for (int i = index; i < this.getQueueLength(); i++){ // Embaralha a cópia
                int i1 = (int) ((Math.random() * (this.getQueueLength() - index)) + index); // Gera um int aleatório para fazer o shuffle em cada iteração do laço
                permutarMusicas(i, i1);
            }
            isShuffled = true;
            return this.shuffledTable;
        }
    }

    //<editor-fold desc="Getters and Setters">
    public String getSongID(int index){
        return getTable()[index][5];
    }
    public int getDuracaoMusica(int index){
        if (isShuffled) return  (int) shuffledDuracaoMusicas[index][0];
        else return (int) duracaoMusicas[index][0];
    }
    public int getMsPerFrame(int index){
        if (isShuffled) return  (int) shuffledDuracaoMusicas[index][1];
        else return (int) duracaoMusicas[index][1];
    }
    public String[][] getTable() {
        if (isShuffled) return shuffledTable;
        else return table;
    }
    public int getQueueLength() {
        return queueLength;
    }
    public Song getSong(int index){
        if (isShuffled) return shuffledSongs[index];
        else return songs[index];
    }
    public int getSongPlayingIndex() {
        return songPlayingIndex;
    }
    public void setSongPlayingIndex(int songPlayingIndex) {
        this.songPlayingIndex = songPlayingIndex;
    }
    //</editor-fold>
}
