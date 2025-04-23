package i.am.a.cat.streemify;

public class Folder {
    private final String folderName;
    private final String folderPath;
    private final Integer videoCount;

    public Folder(String folderName,String folderPath, Integer videoCount) {
        this.folderName = folderName;
        this.folderPath = folderPath;
        this.videoCount = videoCount;
    }
    public String getFolderName() {
        return folderName;
    }

    public String getFolderPath() {
        return folderPath;
    }

    public Integer getVideoCount() {
        return videoCount;
    }
}
