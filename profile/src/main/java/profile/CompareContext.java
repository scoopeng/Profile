/**
 * 
 */
package profile;

import java.io.File;

/**
 * @author bradpeters
 *
 */
public class CompareContext
{
    public CompareContext parent;
    public File file;
    public boolean directory;
    public int level;
    public String path;
    public long numFilesOriginal;
    public long numFilesRevised;
    public long numFilesAdded;
    public long numFilesDeleted;
    public long numFilesModified;
    public long numLinesOriginal;
    public long numLinesRevised;
    public long numLinesAdded;
    public long numLinesDeleted;
    public long numLinesModified;
    public long numBytesOriginal;
    public long numBytesRevised;
    public long numBytesAdded;
    public long numBytesDeleted;
    public long numBytesModified;

    public void add(CompareContext cc)
    {
	numFilesOriginal += cc.numFilesOriginal;
	numFilesRevised += cc.numFilesRevised;
	numFilesAdded += cc.numFilesAdded;
	numFilesDeleted += cc.numFilesDeleted;
	numFilesModified += cc.numFilesModified;
	numLinesOriginal += cc.numLinesOriginal;
	numLinesRevised += cc.numLinesRevised;
	numLinesAdded += cc.numLinesAdded;
	numLinesDeleted += cc.numLinesDeleted;
	numLinesModified += cc.numLinesModified;
	numBytesOriginal += cc.numBytesOriginal;
	numBytesRevised += cc.numBytesRevised;
	numBytesAdded += cc.numBytesAdded;
	numBytesDeleted += cc.numBytesDeleted;
	numBytesModified += cc.numBytesModified;
    }

    public static String getHeader()
    {
	return ("level,directory,path,numFilesOriginal,numFilesRevised,numFilesAdded,numFilesDeleted,numFilesModified,numLinesOriginal,numLinesRevised,numLinesAdded,numLinesDeleted,numLinesModified,numBytesOriginal,numBytesRevised,numBytesAdded,numBytesDeleted,numBytesModified");
    }

    public String toString()
    {
	return getLine();
    }

    public String getLine()
    {
	return (level + "," + (directory ? "dir" : "file") + "," + path + "," + numFilesOriginal + "," + numFilesRevised
		+ "," + numFilesAdded + "," + numFilesDeleted + "," + numFilesModified + "," + numLinesOriginal + ","
		+ numLinesRevised + "," + numLinesAdded + "," + numLinesDeleted + "," + numLinesModified + ","
		+ numBytesOriginal + "," + numBytesRevised + "," + numBytesAdded + "," + numBytesDeleted + ","
		+ numBytesModified);
    }
}
