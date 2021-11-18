package gitlet;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Date;

/** Commit class.
 * @author tomzheng **/
public class Commit implements Serializable {
    /** The commit message. **/
    private String _commitmessage;

    /** The hash of the commit. **/
    private String _hash;

    /** The time the commit was created. **/
    private Date _timestamp;

    /** The commit tree. **/
    private HashMap<String, String> _commitTree;

    /** The parent of the commit. **/
    private Commit _parent;

    /** The hash of the parent commit. **/
    private String _parentHash;

    /** Coparent of this commit. **/
    private Commit _coparent;

    /** Initial commit constructor. **/
    public Commit() {
        _commitmessage = "initial commit";
        _parent = null;
        _parentHash = null;
        _timestamp = new Date(0);
        _commitTree = new HashMap<>();
        _hash = Utils.sha1(Utils.serialize(_commitTree),
                Utils.serialize(_timestamp), _commitmessage);
    }

    /** Commit constructor that takes in staging
     * area SA, the hash HEAD, and string MESSAGE. **/
    public Commit(StagingArea sa, String head, String message) {
        _timestamp = new Date();
        _parentHash = head;
        _commitmessage = message;
        _commitTree = new HashMap<>(sa.getBranches());
        _hash = Utils.sha1(Utils.serialize(_commitTree),
                Utils.serialize(_timestamp), _commitmessage);
    }

    /** Function that allows you to set the MESSAGE. **/
    public void setMessage(String message) {
        _commitmessage = message;
    }

    /** Getter method for message.
     * @return message **/
    public String getMessage() {
        return _commitmessage;
    }

    /** Getter method for timestamp.
     * @return timestamp **/
    public Date getTimestamp() {
        return _timestamp;
    }

    /** Getter method for parent.
     * @return parent **/
    public Commit getParent() {
        return _parent;
    }

    /** Getter method for parent hash.
     * @return parenthash **/
    public String getParentHash() {
        return _parentHash;
    }

    /** Method to set the PARENTHASH. **/
    public void setParentHash(String parenthash) {
        _parentHash = parenthash;
    }

    /** Method to set the PARENT. **/
    public void setParent(Commit parent) {
        _parent = parent;
    }

    /** Method to set the COPARENT. **/
    public void setCoParent(Commit coparent) {
        _coparent = coparent;
    }

    /** Getter method for commit tree.
     * @return committree **/
    public HashMap<String, String> getBranches() {
        return _commitTree;
    }

    /** Getter method for hash.
     * @return hash **/
    public String getHash() {
        return _hash;
    }


    /** Getter method for the commit tree.
     * @return committree **/
    public HashMap<String, String> getTree() {
        return _commitTree;
    }

    /** Checks if file FILENAME is tracked.
     * @return boolean **/
    public boolean isTracked(String filename) {
        if (_commitTree.containsKey(filename)) {
            return true;
        }
        return false;
    }
}

