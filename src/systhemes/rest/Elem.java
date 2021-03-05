package systhemes.rest;

/**
 * Created by prog on 2017-08-28.
 */
public class Elem {
    private String name;
    private Metadata meta;
    private String timeLastModified;
    private String serverRelativeUrl;
    private String uniqueID;
    private int itemCount;

    public Elem() {
        meta = new Metadata();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Metadata getMeta() { return meta; }

    public void setMeta(Metadata meta) {
        this.meta = meta;
    }

    public String getTimeLastModified() { return timeLastModified; }

    public void setTimeLastModified(String timeLastModified) { this.timeLastModified = timeLastModified; }

    public String getServerRelativeUrl() { return serverRelativeUrl; }

    public void setServerRelativeUrl(String serverRelativeUrl) { this.serverRelativeUrl = serverRelativeUrl; }

    public String getUniqueID() { return uniqueID; }

    public void setUniqueID(String uniqueID) { this.uniqueID = uniqueID; }

    public int getItemCount() { return itemCount; }

    public void setItemCount(int itemCount) { this.itemCount = itemCount; }
}
