package gitlet;
import java.io.Serializable;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/** Blob class.
 * @author tomzheng **/
public class Blob implements Serializable {

    /** the name of the blob. **/
    private String _filename;

    /** contents of the blob. **/
    private byte[] _contains;

    /** contents displayed as a string. **/
    private String _containsStr;

    /** Hash code of the blob. **/
    private String _hash;

    /** Blob constructor that takes in NAME. **/
    public Blob(String name) {
        _filename = name;
        File f = new File(name);
        _contains = Utils.readContents(f);
        _containsStr = Utils.readContentsAsString(f);
        List<Object> thisBlob = new ArrayList<>();
        thisBlob.add(_filename); thisBlob.add(_contains);
        thisBlob.add(_containsStr);
        _hash = Utils.sha1(thisBlob);
    }

    /** getter method for filename.
     * @return filename **/
    public String fileName() {
        return _filename;
    }

    /** getter method for hash.
     * @return hash **/
    public String hash() {
        return _hash;
    }

    /** getter method for contents.
     * @return contents **/
    public byte[] contents() {
        return _contains;
    }

    /** getter method for contents as string.
     * @return string **/
    public String contentsStr() {
        return _containsStr;
    }
}
