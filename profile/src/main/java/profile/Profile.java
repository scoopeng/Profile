/**
 * 
 */
package profile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.github.difflib.DiffUtils;
import com.github.difflib.patch.AbstractDelta;
import com.github.difflib.patch.ChangeDelta;
import com.github.difflib.patch.DeleteDelta;
import com.github.difflib.patch.InsertDelta;
import com.github.difflib.patch.Patch;

/**
 * @author bradpeters
 *
 */
public class Profile
{
    private static String[] extensions = new String[] { ".java", ".js", ".html", ".gradle" };

    /**
     * @param args
     */
    public static void main(String[] args)
    {
	if (args.length != 2)
	{
	    System.out.println("Usage: profile originalPath revisedPath");
	    return;
	}
	String originalPath = args[0];
	String revisedPath = args[1];
	Map<String, CompareContext> originalFiles = new HashMap<>();
	Map<String, CompareContext> revisedFiles = new HashMap<>();
	CompareContext originalRoot = walkDirectoryTree(originalPath, "/", originalFiles, 1, null);
	CompareContext revisedRoot = walkDirectoryTree(revisedPath, "/", revisedFiles, 1, null);

	Set<String> deletedFiles = new HashSet<String>(originalFiles.keySet());
	deletedFiles.removeAll(revisedFiles.keySet());
	Set<String> newFiles = new HashSet<String>(revisedFiles.keySet());
	newFiles.removeAll(originalFiles.keySet());
	Set<String> commonFiles = new HashSet<String>(originalFiles.keySet());
	commonFiles.retainAll(revisedFiles.keySet());
	for (String key : commonFiles)
	{
	    CompareContext originalcc = originalFiles.get(key);
	    CompareContext revisedcc = revisedFiles.get(key);
	    if (!originalcc.directory && !revisedcc.directory)
	    {
		compareFiles(originalcc, revisedcc);
	    }
	}
	for (String key : deletedFiles)
	{
	    CompareContext originalcc = originalFiles.get(key);
	    if (!originalcc.directory)
	    {
		profileFile(originalcc, true);
	    }
	}
	for (String key : newFiles)
	{
	    CompareContext revisedcc = revisedFiles.get(key);
	    if (!revisedcc.directory)
	    {
		profileFile(revisedcc, false);
	    }
	}
	originalRoot.add(revisedRoot);
	// Print out
	System.out.println(CompareContext.getHeader());
	for (String key : commonFiles)
	{
	    CompareContext originalcc = originalFiles.get(key);
	    System.out.println(originalcc.getLine("modified"));
	}
	for (String key : deletedFiles)
	{
	    CompareContext originalcc = originalFiles.get(key);
	    System.out.println(originalcc.getLine("deleted"));
	}
	for (String key : newFiles)
	{
	    CompareContext revisedcc = revisedFiles.get(key);
	    System.out.println(revisedcc.getLine("added"));
	}
    }

    private static CompareContext walkDirectoryTree(String root, String path, Map<String, CompareContext> fileMap,
	    int level, CompareContext parent)
    {
	File dir = new File(root + path);
	if (!validFile(dir) || !dir.isDirectory())
	{
	    return null;
	}
	CompareContext cc = new CompareContext();
	cc.parent = parent;
	cc.directory = true;
	cc.file = dir;
	cc.level = level;
	cc.path = path;
	fileMap.put(path, cc);
	File[] files = dir.listFiles();
	for (File f : files)
	{
	    if (!validFile(f))
	    {
		continue;
	    }
	    if (f.isFile())
	    {
		CompareContext fcc = new CompareContext();
		fcc.parent = cc;
		fcc.directory = false;
		fcc.file = f;
		fcc.level = level + 1;
		fcc.path = path + f.getName() + "/";
		fileMap.put(fcc.path, fcc);
	    } else
	    {
		walkDirectoryTree(root, path + f.getName() + "/", fileMap, level + 1, cc);
	    }
	}
	return cc;
    }

    private static boolean validFile(File f)
    {
	if (f.isDirectory() && !f.isHidden())
	{
	    return true;
	}
	if (f.isFile())
	{
	    String name = f.getName();
	    for (String ext : extensions)
	    {
		if (name.toLowerCase().endsWith(ext))
		{
		    return true;
		}
	    }
	}
	return false;
    }

    /**
     * Profile files that were either deleted or added (not deleted). Assumes
     * modified files analyzed with compareFiles
     * 
     * @param cc
     * @param deleted
     */
    private static void profileFile(CompareContext cc, boolean deleted)
    {
	try
	{
	    List<String> lines = Files.readAllLines(cc.file.toPath());
	    if (deleted)
	    {
		cc.numFilesOriginal = 1;
		cc.numBytesOriginal = cc.file.length();
		cc.numLinesOriginal = lines.size();
		cc.numFilesDeleted = 1;
		cc.numLinesDeleted = cc.numLinesOriginal;
		cc.numBytesDeleted = cc.numBytesOriginal;
	    } else
	    {
		cc.numFilesRevised = 1;
		cc.numBytesRevised = cc.file.length();
		cc.numLinesRevised = lines.size();
		cc.numFilesAdded = 1;
		cc.numLinesAdded = cc.numLinesRevised;
		cc.numBytesAdded = cc.numBytesRevised;
	    }
	    // Add up the tree
	    CompareContext parentcc = cc.parent;
	    while (parentcc != null)
	    {
		parentcc.add(cc);
		parentcc = parentcc.parent;
	    }
	} catch (IOException e)
	{
	    e.printStackTrace();
	    return;
	}
    }

    /**
     * Compare files and update the statistics in the original compare context
     * 
     * @param originalcc
     * @param revisedcc
     * @return
     */
    private static void compareFiles(CompareContext originalcc, CompareContext revisedcc)
    {
	File originalFile = originalcc.file;
	File revisedFile = revisedcc.file;
	CompareContext cc = originalcc;
	// build simple lists of the lines of the two testfiles
	List<String> original = null;
	List<String> revised = null;
	try
	{
	    if (originalFile.exists() && revisedFile.exists())
	    {
		cc.numFilesOriginal = 1;
		cc.numFilesRevised = 1;
		original = Files.readAllLines(originalFile.toPath());
		revised = Files.readAllLines(revisedFile.toPath());
		cc.numBytesOriginal = originalFile.length();
		cc.numLinesOriginal = original.size();
		cc.numBytesRevised = revisedFile.length();
		cc.numLinesRevised = revised.size();
	    }
	} catch (IOException e)
	{
	    e.printStackTrace();
	    return;
	}
	if (original == null || revised == null)
	{
	    return;
	}

	// compute the patch: this is the diffutils part
	Patch<String> patch = DiffUtils.diff(original, revised);

	for (AbstractDelta<String> delta : patch.getDeltas())
	{
	    if (delta instanceof InsertDelta)
	    {
		for (String s : delta.getTarget().getLines())
		{
		    cc.numLinesAdded++;
		    cc.numBytesAdded += s.length() + 1;
		}
	    } else if (delta instanceof DeleteDelta)
	    {
		for (String s : delta.getSource().getLines())
		{
		    cc.numLinesDeleted++;
		    cc.numBytesDeleted += s.length() + 1;
		}
	    } else if (delta instanceof ChangeDelta)
	    {
		StringBuilder one = new StringBuilder();
		StringBuilder two = new StringBuilder();
		for (String s : delta.getSource().getLines())
		{
		    one.append(s + "\n");
		}
		for (String s : delta.getTarget().getLines())
		{
		    two.append(s + "\n");
		}
		cc.numLinesModified += delta.getSource().getLines().size();
		StringChanges sc = numChangedCharacters(one.toString(), two.toString());
		cc.numBytesModified += sc.bytesModified;
		cc.numBytesAdded += sc.bytesAdded;
		cc.numBytesDeleted += sc.bytesDeleted;
	    }
	}
	if (patch.getDeltas().size() > 0)
	{
	    cc.numFilesModified = 1;
	}
	// Add up the tree
	CompareContext parentcc = cc.parent;
	while (parentcc != null)
	{
	    parentcc.add(cc);
	    parentcc = parentcc.parent;
	}
	return;
    }

    private static class StringChanges
    {
	int bytesAdded;
	int bytesModified;
	int bytesDeleted;
    }

    private static StringChanges numChangedCharacters(String one, String two)
    {
	int len1 = one.length();
	int len2 = two.length();
	int minLength = len1 < len2 ? len1 : len2;
	int maxLength = len1 < len2 ? len2 : len1;
	int start = 0;
	int end = 0;
	for (; start < minLength; start++)
	{
	    if (one.charAt(start) != two.charAt(start))
	    {
		break;
	    }
	}
	for (; end < minLength; end++)
	{
	    if (one.charAt(len1 - end - 1) != two.charAt(len2 - end - 1))
	    {
		break;
	    }
	}
	int changed = maxLength - start - end;
	StringChanges sc = new StringChanges();
	if (len2 > len1)
	{
	    sc.bytesAdded = Math.min(changed, len2 - len1);
	    sc.bytesModified = changed - sc.bytesAdded;
	} else if (len1 > len2)
	{
	    sc.bytesDeleted = len1 - len2;
	    sc.bytesModified = changed - sc.bytesDeleted;
	} else
	{
	    sc.bytesModified = changed;
	}
	return sc;
    }
}
