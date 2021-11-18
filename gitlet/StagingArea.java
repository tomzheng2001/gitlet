package gitlet;
import java.io.Serializable;
import java.util.HashMap;

/** Staging area class.
 * @author tomzheng
 */
public class StagingArea implements Serializable {

    /** Branches in staging area. **/
    private HashMap<String, String> branches;
    /** Previous branches. **/
    private HashMap<String, String> previous;

    /**Constructor for staging area
     * takes in C. **/
    public StagingArea(Commit c) {
        branches = new HashMap<>(c.getBranches());
        previous = new HashMap<>(c.getBranches());
    }

    /** Adds a file named NAME with HASH to
     * the staging area. **/
    public void add(String name, String hash) {
        branches.put(name, hash);
    }

    /** Checks if FILENAME is being tracked
     * by the staging area.
     * @return boolean **/
    public boolean isTracked(String filename) {
        if (branches.containsKey(filename)) {
            return true;
        }
        return false;
    }

    /** Getter method for branches.
     * @return hashmap **/
    public HashMap<String, String> getBranches() {
        return branches;
    }

    /** Method to check if an item NAME has been
     * removed from the staging area.
     * @return boolean **/
    public boolean remove(String name) {
        if (!previous.containsKey(name)) {
            branches.remove(name); return false;
        } else {
            boolean current = branches.containsKey(name);
            if (current) {
                branches.remove(name);
                return true;
            }
            return false;
        }
    }

}
