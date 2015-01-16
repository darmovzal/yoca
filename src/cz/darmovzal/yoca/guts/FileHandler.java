package cz.darmovzal.yoca.guts;

import java.io.*;

public interface FileHandler {
	public String[] list();
	public InputStream read(String name) throws IOException;
	public OutputStream write(String name) throws IOException;
	public boolean remove(String name);
}

