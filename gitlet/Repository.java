package gitlet;
import java.util.ArrayList;
import java.util.TreeMap;
import java.util.HashMap;
import java.io.File;
import java.io.Serializable;
import java.util.Set;
import java.util.List;

/** Repository class.
 * @author tomzheng
 */
public class Repository implements Serializable {

    /** Current directory. **/
    private File currentDir;

    /** Folder. **/
    private File folder;

    /** List of commits. **/
    private ArrayList<String> commits;

    /** Hash of head Commit. **/
    private String head;

    /** Hash of current branch. **/
    private String _currBranch;

    /** The headcommit. **/
    private Commit headCommit;

    /** Map of all branches. **/
    private TreeMap<String, String> _branches;

    /** The origin. **/
    private File origin = new File(System.getProperty("user.dir"));

    /** The staging area for files. **/
    private StagingArea _stagingarea;

    /** Hashmap mapping hashes to commits. **/
    private HashMap<String, Commit> hashToCommit;

    /** Constructor for repository. **/
    public Repository() {
        currentDir = new File(".");
        folder = Utils.join(currentDir, ".gitlet");
        commits = new ArrayList<String>();
        headCommit = new Commit();
        head = headCommit.getHash();
        _branches = new TreeMap<String, String>();
        _currBranch = "master";
        Utils.writeContents(Utils.join(folder,
                head), Utils.serialize(headCommit));
        commits.add(head);
        hashToCommit = new HashMap<>();
        hashToCommit.put(head, headCommit);
        _branches.put(_currBranch, head);
        getCommitWithHash(head).setMessage("initial commit");
        _stagingarea = new StagingArea(headCommit);
    }

    /** Method to add FILE to repository. **/
    public void addFile(String file) {
        File fdir = Utils.join(currentDir, file);
        String hash = getFileHash(file);
        Utils.writeContents(Utils.join(folder,
                hash), Utils.readContents(fdir));
        _stagingarea.add(file, hash);
    }

    /** Method to remove FILE from repository. **/
    public void removeFile(String file) {
        if (!_stagingarea.isTracked(file)
                && !headCommit.isTracked(file)) {
            throw new GitletException("No reason to remove the file.");
        }
        if (_stagingarea.remove(file)) {
            Utils.restrictedDelete(Utils.join(currentDir, file));
        }
    }

    /** Method to create a new commit with message MESSAGE. **/
    public void createCommit(String message) {
        if (message.isEmpty()) {
            throw new GitletException("Please enter a commit message.");
        }
        if (_stagingarea.getBranches().equals(headCommit.getTree())) {
            throw new GitletException("No changes added to the commit.");
        }
        headCommit = new Commit(_stagingarea, headCommit.getHash(), message);
        headCommit.setParentHash(head);
        headCommit.setParent(getCommitWithHash(headCommit.getParentHash()));
        head = headCommit.getHash();
        commits.add(head);
        hashToCommit.put(head, headCommit);
        Utils.writeContents(Utils.join(folder,
                head), Utils.serialize(headCommit));
        _stagingarea = new StagingArea(headCommit);
        _branches.put(_currBranch, head);
    }

    /** Creates a new branch with name BRANCHNAME. **/
    public void makeBranch(String branchname) {
        if (_branches.containsKey(branchname)) {
            throw new GitletException("A branch with that name already exists");
        }
        _branches.put(branchname, head);
    }

    /** Removes a branch of name BRANCHNAME. **/
    public void removeBranch(String branchname) {
        if (!_branches.containsKey(branchname)) {
            throw new GitletException("A branch "
                    +  "with that name does not exist.");
        }
        if (_currBranch.equals(branchname)) {
            throw new GitletException("Cannot remove the current branch.");
        }
        _branches.remove(branchname);
    }

    /** Reset the file back to the previous commit.
     * Takes in FILENAME and COMMIT hash. **/
    public void resetFile(String filename, String commit) {
        String hash;
        if (getCommitWithHash(commit).isTracked(filename)) {
            hash = getCommitWithHash(commit).getTree().get(filename);
        } else {
            throw new GitletException("File does not exist in that commit.");
        }
        File fdir = Utils.join(folder, hash);
        File fdir2 = Utils.join(currentDir, filename);
        Utils.writeContents(fdir2, Utils.readContents(fdir));
    }

    /** Helper method to merge, takes in BRANCH.
     * @return splitpointHash. **/
    public String mergeHelper1(String branch) {
        if (branch.equals(_currBranch)) {
            throw new GitletException("Cannot merge a branch with itself.");
        } else if (!_branches.containsKey(branch)) {
            throw new GitletException("A branch with"
                    + " that name does not exist.");
        }
        boolean noRemoved = true;
        boolean noStaged = true;
        for (String s: headCommit.getTree().keySet()) {
            if (!_stagingarea.isTracked(s)) {
                noRemoved = false;
            }
        }
        for (String s2: _stagingarea.getBranches().keySet()) {
            if (!headCommit.isTracked(s2)) {
                noStaged = false;
            }
        }
        if (!noRemoved || !noStaged) {
            throw new GitletException("You have uncommitted changes.");
        }
        String splitPointHash = "";
        ArrayList<String> touched = new ArrayList<>();
        String startpoint = _branches.get(branch);
        Commit startCommit = getCommitWithHash(startpoint);
        while (startCommit != null) {
            touched.add(startCommit.getHash());
            startCommit = startCommit.getParent();
        }
        String startpoint2 = _branches.get(_currBranch);
        Commit startCommit2 = getCommitWithHash(startpoint2);
        while (startCommit2 != null) {
            String commithash = startCommit2.getHash();
            if (touched.contains(commithash)) {
                splitPointHash = commithash;
                break;
            }
            startCommit2 = startCommit2.getParent();
        }
        return splitPointHash;
    }

    /** Another helpepr method to merge, takes in
     * CHECKOUT, CONFLICTED, SPLITPOINT, CURRBRANCH,
     * OTHERBRANCH.
     */
    private void mergeHelper2(ArrayList<String> checkout,
                              ArrayList<String> conflicted,
                              Commit splitPoint, Commit currBranch,
                              Commit otherBranch) {
        for (String hash: otherBranch.getTree().keySet()) {
            String otherHash;
            String currentHash;
            String splitHash;
            if  (otherBranch.isTracked(hash)) {
                otherHash = otherBranch.getTree().get(hash);
            } else {
                otherHash = "";
            }
            if (currBranch.isTracked(hash)) {
                currentHash = currBranch.getTree().get(hash);
            } else {
                currentHash = "";
            }
            if (splitPoint.isTracked(hash)) {
                splitHash = splitPoint.getTree().get(hash);
            } else {
                splitHash = "";
            }
            if (!otherHash.equals(currentHash)) {
                if (!currentHash.equals(splitHash)
                        && !otherHash.equals(splitHash)) {
                    conflicted.add(hash); continue;
                }
                if (currentHash.equals(splitHash)) {
                    String filehash2 = "";
                    File f = Utils.join(currentDir, hash);
                    if (f.exists()) {
                        filehash2 = Utils.sha1
                                (Utils.readContents(f));
                    }
                    if (!filehash2.equals("") && currentHash.equals("")) {
                        throw new GitletException("There "
                                + "is an untracked file in the way;"
                                + " delete it, or add and commit it first.");
                    }
                    checkout.add(hash);
                }
            }
        }
    }

    /** Helper method to merge which takes in
     * DELETE, CONFLICTED, SPLITPOINT, CURRBRANCH,
     * and OTHERBRANCH.
     */
    private void mergeHelper3(ArrayList<String> delete,
                              ArrayList<String> conflicted,
                              Commit splitPoint, Commit currBranch,
                              Commit otherBranch) {
        for (String hash: currBranch.getTree().keySet()) {
            String otherHash;
            String currentHash;
            String splitHash;
            if  (otherBranch.isTracked(hash)) {
                otherHash = otherBranch.getTree().get(hash);
            } else {
                otherHash = "";
            }
            if (currBranch.isTracked(hash)) {
                currentHash = currBranch.getTree().get(hash);
            } else {
                currentHash = "";
            }
            if (splitPoint.isTracked(hash)) {
                splitHash = splitPoint.getTree().get(hash);
            } else {
                splitHash = "";
            }
            if (otherHash.equals("")) {
                if (!splitHash.equals("") && !currentHash.equals(splitHash)) {
                    conflicted.add(hash);
                    continue;
                }
                if (currentHash.equals(splitHash)) {
                    if (_stagingarea.isTracked(hash)) {
                        delete.add(hash);
                    } else {
                        String filehash = "";
                        if (Utils.join(currentDir, hash).exists()) {
                            filehash = Utils.sha1
                                    (Utils.readContents(Utils.
                                            join(currentDir, hash)));
                        }
                        if (!filehash.equals("")) {
                            throw new GitletException("There is an "
                                    + "untracked file in the way;"
                                    + " delete it, or "
                                    + "add and commit it first.");
                        }
                    }
                }
            }
        }
    }

    /** Method to merge current
     * branch with BRANCH. **/
    public void merge(String branch) {
        String splitPointHash = mergeHelper1(branch);
        Commit splitPoint = getCommitWithHash(splitPointHash);
        Commit currBranch = getCommitWithHash(_branches.get(_currBranch));
        Commit otherBranch = getCommitWithHash(_branches.get(branch));
        if (splitPoint.equals(currBranch)) {
            throw new GitletException("Current branch fast-forwarded.");
        }
        if (splitPoint.equals(otherBranch)) {
            head = _branches.get(branch);
            _branches.put(_currBranch, head);
            headCommit = getCommitWithHash(head);
            throw new GitletException("Given branch "
                    + "is an ancestor of the current branch.");
        }
        ArrayList<String> delete = new ArrayList<>();
        ArrayList<String> checkout = new ArrayList<>();
        ArrayList<String> conflicted = new ArrayList<>();
        mergeHelper2(checkout, conflicted, splitPoint, currBranch, otherBranch);
        mergeHelper3(delete, conflicted, splitPoint, currBranch, otherBranch);
        if (!conflicted.isEmpty()) {
            System.out.println("Encountered a merge conflict.");
        }
        for (String del: delete) {
            removeFile(del);
        }
        for (String check: checkout) {
            String hash1 = otherBranch.getHash();
            if (!getCommitWithHash(hash1).isTracked(check)) {
                throw new GitletException("File "
                        + "does not exist in that commit.");
            }
            String bHash = getCommitWithHash(hash1).getTree().get(check);
            File bDir = Utils.join(folder, bHash);
            File wDir = Utils.join(currentDir, check);
            Utils.writeContents(wDir, Utils.readContents(bDir));
            addFile(check);
        }
        for (String file: conflicted) {
            String f1;
            if (currBranch.isTracked(file)) {
                f1 = Utils.readContentsAsString(Utils.join(
                        folder, currBranch.getTree().get(file)));
            } else {
                f1 = "";
            }
            String f2;
            if (otherBranch.isTracked(file)) {
                f2 = Utils.readContentsAsString(Utils.join(
                        folder, otherBranch.getTree().get(file)));
            } else {
                f2 = "";
            }
            String message = "<<<<<<< HEAD\n" + f1
                    + "=======\n" + f2 + ">>>>>>>\n";
            Utils.writeContents(Utils.join(currentDir, file), message);
            addFile(file);
        }
        merge2(branch, "Merged " + branch + " into " + _currBranch + ".");
    }

    /** Another helper method for merge
     * takes in OTHERPARENT and MESSAGE. **/
    private void merge2(String otherparent, String message) {
        if (headCommit.getTree().equals(_stagingarea.getBranches())) {
            throw new GitletException("No changes added to the commit.");
        }
        headCommit = new Commit(_stagingarea, headCommit.getHash(), message);
        head = headCommit.getHash();
        commits.add(head);
        hashToCommit.put(head, headCommit);
        Utils.writeContents(Utils.join(folder, head),
                Utils.serialize(headCommit));
        _stagingarea = new StagingArea(headCommit);
        _branches.put(_currBranch, head);
    }

    /** Method to reset a file, taking in FILENAME. **/
    public void resetFile(String filename) {
        if (!headCommit.isTracked(filename)) {
            throw new GitletException("File does not exist in that commit.");
        }
        File fdir = Utils.join(folder, headCommit.getTree().get(filename));
        File fdir2 = Utils.join(currentDir, filename);
        Utils.writeContents(fdir2, Utils.readContents(fdir));
    }

    /** Method to obtain the corresponding
     * commit given the HASH.
     * @return Commit **/
    public Commit getCommitWithHash(String hash) {
        if (hashToCommit.containsKey(hash)) {
            return hashToCommit.get(hash);
        }
        throw new GitletException("No commit with that id exists.");
    }

    /** Get the hash of a file given the FILE.
     * @return hash **/
    private String getFileHash(String file) {
        File fdir =  Utils.join(currentDir, file);
        return Utils.sha1(Utils.readContents(fdir));
    }

    /** Getter method for hash of head.
     * @return String **/
    public String getHead() {
        return head;
    }

    /** Convert UID into ID.
     * @return ID **/
    public String uidToID(String uid) {
        for (int i = 0; i < commits.size(); i++) {
            if (commits.get(i).startsWith(uid)) {
                return commits.get(i);
            }
        }
        throw new GitletException("No commit with that id exists.");
    }

    /** Getter method for commits.
     * @return List **/
    public List<String> getCommits() {
        return commits;
    }

    /** Getter method for branches.
     * @return treemap **/
    public TreeMap<String, String> getBranches() {
        return _branches;
    }

    /** Getter method for the current branch.
     * @return string **/
    public String getCurrentBranch() {
        return _currBranch;
    }

    /** Getter method for the staging area. \
     * @return staging area **/
    public StagingArea getStagingArea() {
        return _stagingarea;
    }

    /** Getter method for the head commit.
     * @return Commit **/
    public Commit getHeadCommit() {
        return headCommit;
    }

    /** Getter method for the current directory.
     * @return file **/
    public File getCurrentDir() {
        return currentDir;
    }

    /** Main helper method to reset, taking in COMMIT. **/
    public void reset(String commit) {
        if (!commits.contains(commit)) {
            throw new GitletException("No commit with that id exists.");
        }
        Set<String> prevKeys = getCommitWithHash(commit).getTree().keySet();
        Set<String> currKeys = _stagingarea.getBranches().keySet();
        for (String f: prevKeys) {
            if (!_stagingarea.isTracked(f)) {
                File fdir = Utils.join(currentDir, f);
                if (fdir.exists()) {
                    throw new GitletException("There is an untracked"
                            + " file in the way; delete it, "
                            + "or add and commit it first.");
                }
            }
        }
        for (String g: currKeys) {
            Utils.restrictedDelete(Utils.join(currentDir, g));
        }
        for (String h: prevKeys) {
            File f = Utils.join(folder,
                    getCommitWithHash(commit).getTree().get(h));
            File g = Utils.join(currentDir, h);
            Utils.writeContents(g, Utils.readContents(f));
        }
        headCommit = getCommitWithHash(commit);
        head = headCommit.getHash();
        _branches.put(_currBranch, head);
        _stagingarea = new StagingArea(headCommit);
    }

    /** Main helper method for checkout, taking in BRANCHNAME. **/
    public void checkout(String branchname) {
        if (_currBranch.equals(branchname)) {
            throw new GitletException("No need "
                    + "to checkout the current branch.");
        }
        if (!_branches.keySet().contains(branchname)) {
            throw new GitletException("No such branch exists");
        }
        _currBranch = branchname;
        if (!commits.contains(_branches.get(branchname))) {
            throw new GitletException("No "
                    + "commit with that id exists.");
        }
        Set<String> prevKeys = getCommitWithHash
                (_branches.get(branchname)).getTree().keySet();
        Set<String> currKeys = _stagingarea.
                getBranches().keySet();
        for (String f: prevKeys) {
            if (!_stagingarea.isTracked(f)) {
                File fdir = Utils.join(currentDir, f);
                if (fdir.exists()) {
                    throw new GitletException("There is an untracked"
                            + " file in the way; "
                            + "delete it, or add and commit it first.");
                }

            }
        }
        for (String g: currKeys) {
            Utils.restrictedDelete(Utils.join(currentDir, g));
        }
        for (String h: prevKeys) {
            File f = Utils.join(folder, getCommitWithHash
                    (_branches.get(branchname)).getTree().get(h));
            File g = Utils.join(currentDir, h);
            Utils.writeContents(g, Utils.readContents(f));
        }
        headCommit = getCommitWithHash(_branches.get(branchname));
        head = headCommit.getHash();
        _branches.put(_currBranch, head);
        _stagingarea = new StagingArea(headCommit);
    }

}
