package edu.whu.pllab.buglocator.tests;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ListBranchCommand.ListMode;
import org.eclipse.jgit.api.ResetCommand.ResetType;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;

public class JGitTest {

	private static Repository repo;
	private static Git git;
	private static List<RevCommit> commits = new ArrayList<RevCommit>();

	public static void main(String[] args) throws Exception {
		String testDir = "D:\\data\\gitTest\\";
		setup(testDir);
		branchTest();
		diffTest();
		hardResetTest();
		FileUtils.deleteDirectory(new File(testDir));
	}

	public static void setup(String workingDirPath) {
		try {
			// Creation of a temp folder that will contain the Git repository
			File workingDirectory = new File(workingDirPath);

			workingDirectory.delete();
			workingDirectory.mkdirs();

			// Create a Repository object
			repo = FileRepositoryBuilder.create(new File(workingDirectory, ".git"));
			repo.create();
			git = new Git(repo);

			// Create a new file and add it to the index
			File dir = new File(workingDirectory, "newDir");
			dir.mkdirs();
			File newFile = new File(dir, "myNewFile");
			newFile.createNewFile();
			git.add().addFilepattern("newDir/myNewFile").call();
			
			File thirdFile = new File(workingDirectory, "myThirdFile");
			thirdFile.createNewFile();
			git.add().addFilepattern("myThirdFile").call();

			// Now, we do the commit with a message
			RevCommit rev = git.commit().setAuthor("liwei", "liwei@example.com").setMessage("My first commit").call();
			commits.add(rev);

			Ref head = repo.findRef("HEAD");
			System.out.println("Ref of HEAD: " + head.getName() + " - " + head.getObjectId().getName());
			System.out.println("Commit Id:" + commits.get(0).getName());

			// Create the second file and add it to the index
			File secondFile = new File(dir, "mySecondFile");
			BufferedWriter writer = new BufferedWriter(new FileWriter(newFile));
			writer.write("Test");
			writer.close();
			secondFile.createNewFile();
			thirdFile.delete();
			git.add().addFilepattern("newDir/myNewFile").call();
			git.add().addFilepattern("newDir/mySecondFile").call();
			git.rm().addFilepattern("myThirdFile").call();
			// commit with a message
			RevCommit secondRev = git.commit().setAuthor("liwei", "liwei@example.com").setMessage("My second commit")
					.call();
			commits.add(secondRev);

			head = repo.findRef("HEAD");
			System.out.println("Ref of HEAD: " + head.getName() + " - " + head.getObjectId().getName());
			System.out.println("Commit Id: " + commits.get(1).getName());

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void branchTest() {
		System.out.println("============== branchTest ==============");
		try {
			// Create a new branch
			git.branchCreate().setName("newBranch").call();
			// Checkout the new branch
			git.checkout().setName("newBranch").call();
			// List the existing branches
			List<Ref> listRefsBranches = git.branchList().setListMode(ListMode.ALL).call();
			// list all branches
			for (Ref refBranch : listRefsBranches) {
				System.out.println("Branch : " + refBranch.getName());
			}
			// Go back on "master" branch and remove the created one
			git.checkout().setName("master").call();
			git.branchDelete().setBranchNames("newBranch").call();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void diffTest() {
		System.out.println("============== diffTest ==============");
		try {
			// Get the id of the tree associated to the two commits
			// ObjectId head = repo.resolve("HEAD^{tree}");
			// ObjectId previousHead = repo.resolve("HEAD~^{tree}");
			ObjectId head = repo.resolve(commits.get(1).getName().substring(0, 10) + "^{tree}");
			ObjectId previousHead = repo.resolve(commits.get(0).getName().substring(0, 10) + "^{tree}");

			System.out.println("Head name:" + head.getName());
			System.out.println("previousHead name:" + previousHead.getName());

			// Instanciate a reader to read the data from the Git database
			ObjectReader reader = repo.newObjectReader();
			// Create the tree iterator for each commit
			CanonicalTreeParser oldTreeIter = new CanonicalTreeParser();
			oldTreeIter.reset(reader, previousHead);
			CanonicalTreeParser newTreeIter = new CanonicalTreeParser();
			newTreeIter.reset(reader, head);
			List<DiffEntry> listDiffs = git.diff().setOldTree(oldTreeIter).setNewTree(newTreeIter).call();
			// Simply display the diff between the two commits
			for (DiffEntry diff : listDiffs) {
				System.out.println(
						diff.getChangeType() + "\tNew Path: " + diff.getNewPath() + "\tOld Path: " + diff.getOldPath());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void hardResetTest() {
		System.out.println("============== hardResetTest ==============");
		try {
			git.reset().setMode(ResetType.HARD).setRef(commits.get(0).getName()).call();
			String[] fileList = repo.getDirectory().getParentFile().list();
			System.out.println("Temp files count: " + fileList.length);

			git.reset().setMode(ResetType.HARD).setRef(commits.get(1).getName()).call();
			fileList = repo.getDirectory().getParentFile().list();
			System.out.println("Temp files count: " + fileList.length);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
