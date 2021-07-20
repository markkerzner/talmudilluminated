package com.shmsoft.t_i;

public class PullFromBlog {
    public static void main(String [] argv) {
        PullFromBlog pull = new PullFromBlog();
        if (argv.length == 0) {
            pull.pullFromAllTractates();
        }
        else {
            pull.pullFromTractate(argv[0]);
        }
    }
    private void pullFromAllTractates() {
        System.out.println("Pulling from all tractates");
    }
    private void pullFromTractate(String tractateName) {
        System.out.println("Pulling from tractate " + tractateName);
    }
}
