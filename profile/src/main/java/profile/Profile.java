/**
 * 
 */
package profile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

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
	System.out.println(CompareContext.getHeader());
	CompareContext cc = walkDirectories(originalPath, revisedPath, "/");
	int a = 1;
    }

    public static CompareContext walkDirectories(String originalRoot, String revisedRoot, String path)
    {
	File originalFile = new File(originalRoot + path);
	File revisedFile = new File(revisedRoot + path);
	CompareContext cc = new CompareContext();
	cc.path = path;
	File[] originals = originalFile.listFiles();
	File[] revised = revisedFile.listFiles();
	for (File of : originals)
	{
	    if (!validFile(of))
	    {
		continue;
	    }
	    for (File rf : revised)
	    {
		if (!validFile(rf))
		{
		    continue;
		}
		if (of.getName().equals(rf.getName()))
		{
		    if (of.isDirectory() && rf.isDirectory())
		    {
			CompareContext newcc = walkDirectories(originalRoot, revisedRoot, path + of.getName() + "/");
			newcc.directory = true;
			if (newcc != null)
			{
			    System.out.println(newcc.getLine());
			    cc.add(newcc);
			}
		    } else if (of.isFile() && rf.isFile())
		    {
			CompareContext newcc = compareFiles(originalRoot, revisedRoot, path + of.getName());
			if (newcc != null)
			{
			    System.out.println(newcc.getLine());
			    cc.add(newcc);
			}
		    }
		}
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

    private static CompareContext compareFiles(String originalRoot, String revisedRoot, String path)
    {
	CompareContext cc = new CompareContext();
	cc.path = path;
	File originalFile = new File(originalRoot + path);
	File revisedFile = new File(revisedRoot + path);
	// build simple lists of the lines of the two testfiles
	List<String> original = null;
	List<String> revised = null;
	try
	{
	    if (originalFile.exists() && !revisedFile.exists())
	    {
		original = Files.readAllLines(originalFile.toPath());
		cc.numFilesOriginal = 1;
		cc.numFilesDeleted = 1;
		cc.numBytesOriginal = originalFile.length();
		cc.numLinesOriginal = original.size();
		return cc;
	    }
	    if (!originalFile.exists() && revisedFile.exists())
	    {
		revised = Files.readAllLines(revisedFile.toPath());
		cc.numFilesRevised = 1;
		cc.numFilesAdded = 1;
		cc.numBytesRevised = revisedFile.length();
		cc.numLinesRevised = revised.size();
		return cc;
	    }
	    if (!originalFile.exists() && !revisedFile.exists())
	    {
		return null;
	    } else
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
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}
	if (original == null || revised == null)
	{
	    return null;
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
	return cc;
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
