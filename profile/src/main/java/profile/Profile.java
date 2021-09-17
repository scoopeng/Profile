/**
 * 
 */
package profile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

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
    private static Map<String, Integer> typeCounts = new HashMap<>();

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

	Map<String, CompareContext> fmap = new HashMap<>();
	CompareContext root = walkDirectoryTrees(originalPath, revisedPath, "/", fmap, 1, null);
	sumTree(root);
	System.out.println(CompareContext.getHeader());
	printTree(root);
	System.out.println();
	System.out.println("Extension,Count");
	for (Entry<String, Integer> countEntry : typeCounts.entrySet())
	{
	    System.out.println(countEntry.getKey() + "," + countEntry.getValue());
	}
    }

    private static void sumTree(CompareContext cc)
    {
	for (CompareContext childcc : cc.children)
	{
	    sumTree(childcc);
	    cc.add(childcc);
	}
    }

    private static void printTree(CompareContext cc)
    {
	System.out.println(cc.getLine());
	for (CompareContext childcc : cc.children)
	{
	    printTree(childcc);
	}
    }

    private static CompareContext walkDirectoryTrees(String originalRoot, String revisedRoot, String path,
	    Map<String, CompareContext> fileMap, int level, CompareContext parent)
    {
	File originaldir = null;
	File reviseddir = null;
	if (originalRoot != null)
	{
	    originaldir = new File(originalRoot + path);
	    if (!originaldir.isDirectory())
	    {
		originaldir = null;
	    }
	}
	if (revisedRoot != null)
	{
	    reviseddir = new File(revisedRoot + path);
	    if (!reviseddir.isDirectory())
	    {
		reviseddir = null;
	    }
	}
	CompareContext cc = new CompareContext();
	if (originalRoot != null && revisedRoot != null)
	{
	    cc.type = "unchanged";
	} else if (originalRoot != null)
	{
	    cc.type = "deleted";
	} else if (revisedRoot != null)
	{
	    cc.type = "added";
	}
	cc.directory = true;
	cc.originalFile = originaldir;
	cc.revisedFile = reviseddir;
	cc.level = level;
	cc.path = path;
	fileMap.put(path, cc);
	File[] originalfiles = null;
	File[] revisedfiles = null;
	Map<String, File> originalnames = new HashMap<>();
	Map<String, File> revisednames = new HashMap<>();
	if (originaldir != null)
	{
	    originalfiles = originaldir.listFiles();
	    for (File f : originalfiles)
	    {
		originalnames.put(f.getName(), f);
	    }
	}
	if (reviseddir != null)
	{
	    revisedfiles = reviseddir.listFiles();
	    for (File f : revisedfiles)
	    {
		revisednames.put(f.getName(), f);
	    }
	}
	Map<String, File> deletedNames = new HashMap<>(originalnames);
	deletedNames.keySet().removeAll(revisednames.keySet());
	Map<String, File> newNames = new HashMap<>(revisednames);
	newNames.keySet().removeAll(originalnames.keySet());
	Map<String, File> commonNames = new HashMap<>(originalnames);
	commonNames.keySet().retainAll(revisednames.keySet());
	for (String filepath : commonNames.keySet())
	{
	    File f = new File(originalRoot + path + filepath);
	    if (!validFile(f))
	    {
		continue;
	    }
	    if (f.isDirectory())
	    {
		cc.children.add(walkDirectoryTrees(originalRoot, revisedRoot, path + filepath + File.separator, fileMap,
			level + 1, cc));
	    } else
	    {
		CompareContext filecc = new CompareContext();
		filecc.directory = false;
		filecc.level = level + 1;
		filecc.originalFile = new File(originalRoot + path + filepath);
		filecc.revisedFile = new File(revisedRoot + path + filepath);
		filecc.path = path + filepath;
		cc.children.add(filecc);
		compareFiles(filecc);
	    }
	}
	for (String filepath : newNames.keySet())
	{
	    File f = new File(revisedRoot + path + filepath);
	    if (!validFile(f))
	    {
		continue;
	    }
	    if (f.isDirectory())
	    {
		cc.children.add(walkDirectoryTrees(null, revisedRoot, path + filepath + File.separator, fileMap,
			level + 1, cc));
	    } else
	    {
		CompareContext filecc = new CompareContext();
		filecc.directory = false;
		filecc.level = level + 1;
		filecc.originalFile = null;
		filecc.revisedFile = new File(revisedRoot + path + filepath);
		filecc.path = path + filepath;
		filecc.type = "added";
		cc.children.add(filecc);
		profileFile(filecc, false);
	    }

	}
	for (String filepath : deletedNames.keySet())
	{
	    File f = new File(originalRoot + path + filepath);
	    if (!validFile(f))
	    {
		continue;
	    }
	    if (f.isDirectory())
	    {
		cc.children.add(walkDirectoryTrees(originalRoot, null, path + filepath + File.separator, fileMap,
			level + 1, cc));
	    } else
	    {
		CompareContext filecc = new CompareContext();
		filecc.directory = false;
		filecc.level = level + 1;
		filecc.originalFile = new File(originalRoot + path + filepath);
		filecc.revisedFile = null;
		filecc.path = path + filepath;
		filecc.type = "deleted";
		cc.children.add(filecc);
		profileFile(filecc, true);
	    }
	}
	return cc;
    }

    private static void registerExtension(String filepath)
    {
	int index = filepath.lastIndexOf('.');
	if (index >= 0)
	{
	    String ext = filepath.substring(index + 1);
	    Integer curCount = typeCounts.get(ext);
	    if (curCount == null)
	    {
		typeCounts.put(ext, 1);
	    } else
	    {
		typeCounts.put(ext, curCount + 1);
	    }
	}
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
	    registerExtension(name);
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
	    if (deleted)
	    {
		List<String> lines = Files.readAllLines(cc.originalFile.toPath());
		cc.numFilesOriginal = 1;
		cc.numBytesOriginal = cc.originalFile.length();
		cc.numLinesOriginal = lines.size();
		cc.numFilesDeleted = 1;
		cc.numLinesDeleted = cc.numLinesOriginal;
		cc.numBytesDeleted = cc.numBytesOriginal;
	    } else
	    {
		List<String> lines = Files.readAllLines(cc.revisedFile.toPath());
		cc.numFilesRevised = 1;
		cc.numBytesRevised = cc.revisedFile.length();
		cc.numLinesRevised = lines.size();
		cc.numFilesAdded = 1;
		cc.numLinesAdded = cc.numLinesRevised;
		cc.numBytesAdded = cc.numBytesRevised;
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
    private static void compareFiles(CompareContext cc)
    {
	// build simple lists of the lines of the two testfiles
	List<String> original = null;
	List<String> revised = null;
	try
	{
	    if (cc.originalFile.exists() && cc.revisedFile.exists())
	    {
		cc.numFilesOriginal = 1;
		cc.numFilesRevised = 1;
		original = Files.readAllLines(cc.originalFile.toPath());
		revised = Files.readAllLines(cc.revisedFile.toPath());
		cc.numBytesOriginal = cc.originalFile.length();
		cc.numLinesOriginal = original.size();
		cc.numBytesRevised = cc.revisedFile.length();
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
	    cc.type = "modified";
	    cc.numFilesModified = 1;
	} else
	{
	    cc.type = "unchanged";
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
