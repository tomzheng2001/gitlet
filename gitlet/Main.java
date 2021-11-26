package gitlet;

import java.io.File;
import java.text.SimpleDateFormat;

/** Driver class for Gitlet, the tiny stupid version-control system.
 *  @author tomzheng
 */
public class Main {

    /** The repository. **/
    private static Repository r;

    /** The object directory. **/
    private static File objectDirectory;

    /** The working directory. **/
    private static File workingDirectory;

    /** The date pattern. **/
    private static String pattern = "EEE MMM d HH:mm:ss yyyy Z";

    /** The format of the date. **/
    private static SimpleDateFormat logTime = new SimpleDateFormat(pattern);

    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND> .... */
    public static void main(String... args) {
        if (args.length < 1) {
            System.out.println("Please enter a command.");
            System.exit(0);
        }
        workingDirectory = new File(".");
        objectDirectory = Utils.join(workingDirectory, ".gitlet");
        if (args[0].equals("init")) {
            init(args);
        }
        if (!Utils.join(objectDirectory, "Repository").exists()) {
            System.out.println("Not in an initialized Gitlet directory.");
            System.exit(0);
        }
        r = Utils.readObject(Utils.join(objectDirectory,
                "Repository"), Repository.class);
        if (args[0].equals("add")) {
            add(args);
            System.exit(0);
        } else if (args[0].equals("commit")) {
            commit(args);
            System.exit(0);
        } else if (args[0].equals("rm")) {
            rm(args);
            System.exit(0);
        } else if (args[0].equals("log")) {
            log(args);
            System.exit(0);
        } else if (args[0].equals("global-log")) {
            globallog(args);
            System.exit(0);
        } else if (args[0].equals("reset")) {
            reset(args);
            System.exit(0);
        } else if (args[0].equals("find")) {
            find(args);
            System.exit(0);
        } else if (args[0].equals("status")) {
            if (!Utils.join(objectDirectory, "Repository").exists()) {
                System.out.println("Not in an initialized Gitlet directory.");
                System.exit(0);
            }
            status(args);
        } else if (args[0].equals("checkout")) {
            checkOut(args);
        } else if (args[0].equals("branch")) {
            branch(args);
            System.exit(0);
        } else if (args[0].equals("rm-branch")) {
            rmBranch(args);
            System.exit(0);
        } else if (args[0].equals("merge")) {
            merge(args);
            System.exit(0);
        } else {
            System.out.println("No command with that name exists.");
        }
        System.exit(0);
    }

    /** The init method which takes in ARGS. **/
    public static void init(String... args) {
        File gitlet = Utils.join(objectDirectory, "Repository");
        if (gitlet.exists()) {
            System.out.println("A Gitlet version-control"
                    +  " system already exists in the current directory");
            System.exit(0);
        }
        objectDirectory.mkdir(); r = new Repository();
        Utils.writeObject(Utils.join(objectDirectory, "Repository"), r);
        System.exit(0);
    }

    /** Log helper method which takes in ARGS. **/
    public static void log(String... args) {
        if (args.length > 1) {
            System.exit(0);
        }
        String head = r.getHead();
        while (r.getCommits().contains(head)) {
            Commit c = r.getCommitWithHash(head);
            System.out.println("===");
            System.out.print("commit ");
            System.out.println(c.getHash());
            System.out.print("Date: ");
            System.out.println(logTime.format(c.getTimestamp()));
            System.out.println(c.getMessage());
            System.out.println();
            if (r.getCommitWithHash(head).getParentHash() != null) {
                head = r.getCommitWithHash(head).getParentHash();
            } else {
                System.exit(0);
            }

        }
        System.exit(0);
    }

    /** Global log helper method which takes in ARGS. **/
    public static void globallog(String... args) {
        if (args.length != 1) {
            System.out.println("Incorrect Operands");
            System.exit(0);
        }
        for (String com: r.getCommits()) {
            Commit c = r.getCommitWithHash(com);
            System.out.println("===");
            System.out.print("commit ");
            System.out.println(c.getHash());
            System.out.print("Date: ");
            System.out.println(logTime.format(c.getTimestamp()));
            System.out.println(c.getMessage());
            System.out.println();
        }
    }

    /** Find helper method which takes in ARGS. **/
    public static void find(String... args) {
        boolean notfound = true;
        if (args.length != 2) {
            System.out.println("Incorrect Operands.");
            System.exit(0);
        }
        for (String com: r.getCommits()) {
            if (r.getCommitWithHash(com).getMessage().equals(args[1])) {
                notfound = false;
                System.out.println(com);
            }
        }
        if (notfound) {
            System.out.println("Found no commit with that message.");
            System.exit(0);
        }
    }

    /** Branch helper method whiich takes in ARGS. **/
    public static void branch(String... args) {
        if (args.length != 2) {
            System.out.println("Incorrect operands.");
            System.exit(0);
        }
        try {
            r.makeBranch(args[1]);
            File repo = Utils.join(objectDirectory, "Repository");
            Utils.writeObject(repo, r);
            System.exit(0);
        } catch (GitletException e) {
            System.out.println(e.getMessage());
            System.exit(0);
        }
    }

    /** RMBranch helper method which takes in ARGS. **/
    public static void rmBranch(String... args) {
        if (args.length != 2) {
            System.out.println("Incorrect Operands.");
            System.exit(0);
        }
        try {
            r.removeBranch(args[1]);
            File repo = Utils.join(objectDirectory, "Repository");
            Utils.writeObject(repo, r);
            System.exit(0);
        } catch (GitletException e) {
            System.out.println(e.getMessage());
            System.exit(0);
        }
    }

    /** Commit helper method which takes in ARGS. **/
    public static void commit(String... args) {
        try {
            r.createCommit(args[1]);
            Utils.writeObject(Utils.join(objectDirectory, "Repository"), r);
            System.exit(0);
        } catch (GitletException e) {
            System.out.println(e.getMessage());
            System.exit(0);
        }
    }

    /** RM helper method which takes in ARGS. **/
    public static void rm(String... args) {
        try {
            String f = args[1];
            r.removeFile(f);
            File repo = Utils.join(objectDirectory, "Repository");
            Utils.writeObject(repo, r);
            System.exit(0);
        } catch (GitletException e) {
            System.out.println(e.getMessage());
            System.exit(0);
        }
    }

    /** Reset helper method which takes in ARGS. **/
    public static void reset(String... args) {
        if (args.length != 2) {
            System.out.println("Incorrect Operands.");
            System.exit(0);
        }
        try {
            r.reset(r.uidToID(args[1]));
            File repo = Utils.join(objectDirectory, "Repository");
            Utils.writeObject(repo, r);
            System.exit(0);
        } catch (GitletException e) {
            System.out.println(e.getMessage());
            System.exit(0);
        }
    }

    /** Status helper method which takes in ARGS. **/
    public static void status(String... args) {
        if (args.length != 1) {
            System.out.println("Incorrect Operands.");
            System.exit(0);
        }
        System.out.println("=== Branches ===");
        for (String branch: r.getBranches().keySet()) {
            if (branch.equals(r.getCurrentBranch())) {
                System.out.print("*"); System.out.println(branch);
            } else {
                System.out.println(branch);
            }
        }
        System.out.println();
        System.out.println("=== Staged Files ===");
        for (String f: r.getStagingArea().getBranches().keySet()) {
            if (!r.getHeadCommit().isTracked(f)) {
                System.out.println(f);
            }
        }
        System.out.println();
        System.out.println("=== Removed Files ===");
        for (String g: r.getHeadCommit().getTree().keySet()) {
            if (!r.getStagingArea().isTracked(g)) {
                System.out.println(g);
            }
        }
        System.out.println();
        System.out.println("=== Modifications Not Staged For Commit ===");
        for (String h: r.getStagingArea().getBranches().keySet()) {
            String thisHash;
            File f = Utils.join(r.getCurrentDir(), h);
            if (!f.exists()) {
                thisHash = "";
            } else {
                thisHash = Utils.sha1(Utils.readContents(f));
            }
            if (!r.getStagingArea().getBranches().get(h).equals(thisHash)) {
                if (Utils.plainFilenamesIn(r.getCurrentDir()).contains(h)) {
                    System.out.print(h); System.out.println(" (modified)");
                } else {
                    System.out.print(h);
                    System.out.println(" (deleted)");
                }
            }
        }
        System.out.println();
        System.out.println("=== Untracked Files ===");
        for (String i: Utils.plainFilenamesIn(r.getCurrentDir())) {
            if (!r.getStagingArea().getBranches().keySet().contains(i)) {
                System.out.println(i);
            }
        }
        System.out.println();
        System.exit(0);
    }

    /** Checkout helper method which takes in ARGS. **/
    public static void checkOut(String... args) {
        if (args.length > 4) {
            System.out.println("Incorrect Operands.");
            System.exit(0);
        }
        if (args[1].equals("--")) {
            if (args.length != 3) {
                System.out.println("Incorrect Operands.");
                System.exit(0);
            }
            String filename = args[2];
            try {
                r.resetFile(filename);
                Utils.writeObject(Utils.join(objectDirectory, "Repository"), r);
                System.exit(0);
            } catch (GitletException e) {
                System.out.println(e.getMessage());
                System.exit(0);
            }
        } else if (args.length == 2) {
            try {
                r.checkout(args[1]);
                Utils.writeObject(Utils.join(objectDirectory, "Repository"), r);
                System.exit(0);
            } catch (GitletException e) {
                System.out.println(e.getMessage()); System.exit(0);
            }
        } else if (args[2].equals("--")) {
            try {
                r.resetFile(args[3], r.uidToID(args[1]));
                Utils.writeObject(Utils.join(objectDirectory, "Repository"), r);
                System.exit(0);
            } catch (GitletException e) {
                System.out.println(e.getMessage());
                System.exit(0);
            }
        }  else {
            System.out.println("Incorrect Operands.");
            System.exit(0);
        }
    }


    /** Add helper method which takes in ARGS. **/
    public static void add(String... args) {
        if (args.length > 2 || args.length < 2) {
            System.exit(0);
        }
        if (!Utils.join(workingDirectory, args[1]).exists()) {
            System.out.println("File does not exist.");
            System.exit(0);
        }
        try {
            r.addFile(args[1]);
            Utils.writeObject(Utils.join(objectDirectory, "Repository"), r);
            System.exit(0);
        } catch (GitletException e) {
            System.out.println(e.getMessage());
            System.exit(0);
        }
    }

    /** Merge helper method which takes in ARGS. **/
    public static void merge(String... args) {
        if (args.length != 2) {
            System.out.println("Incorrect operands.");
            System.exit(0);
        }
        try {
            String branch = args[1];
            r.merge(branch);
            File repo = Utils.join(objectDirectory, "Repository");
            Utils.writeObject(repo, r);
            System.exit(0);
        } catch (GitletException e) {
            System.out.println(e.getMessage());
            System.exit(0);
        }
    }

}
