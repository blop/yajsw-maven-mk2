package org.rzo.yajsw.util;

import org.apache.commons.vfs2.cache.SoftRefFilesCache;
import org.apache.commons.vfs2.FileName;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemOptions;
import org.apache.commons.vfs2.FileType;
import org.apache.commons.vfs2.VFS;
import org.apache.commons.vfs2.impl.DefaultFileSystemManager;
import org.apache.commons.vfs2.impl.VirtualFileName;
import org.apache.commons.vfs2.operations.FileOperationProvider;
import org.apache.commons.vfs2.provider.AbstractFileName;
import org.apache.commons.vfs2.provider.http.HttpFileSystemConfigBuilder;
import org.apache.commons.vfs2.provider.jar.JarFileObject;
import org.apache.commons.vfs2.provider.jar.JarFileProvider;
import org.apache.commons.vfs2.provider.jar.JarFileSystem;
import org.apache.commons.vfs2.provider.local.DefaultLocalFileProvider;
import org.apache.commons.vfs2.provider.local.LocalFileName;
import org.apache.commons.vfs2.provider.res.ResourceFileProvider;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

public class VFSUtils
{
	static DefaultFileSystemManager	fsManager	= null;
	static FileSystemOptions		opts		= new FileSystemOptions();
	static FileObject res;
	static String yajswBase;
	private static boolean resolveRes=true;
	
	public static void init() throws FileSystemException
	{
		if (fsManager != null)
			return;
		
		fsManager = (DefaultFileSystemManager) VFS.getManager();
		String httpProxy = System.getProperty("http.proxyHost");
		String httpPort = System.getProperty("http.proxyPort");
		yajswBase = System.getProperty("wrapper.base.dir", "../../yajsw-base/");
		String jarResFlag=System.getProperty("wrapper.usejarresources");
		resolveRes=(jarResFlag==null || !jarResFlag.equals("false"));
		if (resolveRes)
		{
			Logger.getLogger(SoftRefFilesCache.class).setLevel(Level.ERROR);
			Logger.getLogger(org.apache.commons.vfs2.cache.SoftRefFilesCache.class).setLevel(Level.ERROR);
			res=fsManager.resolveFile("res:resources");
			resolveRes=(res!=null);
		}
		if (httpProxy != null)
		{
			HttpFileSystemConfigBuilder.getInstance().setProxyHost(opts, httpProxy);
			int port = 8080;
			if (httpPort != null)
				try
				{
					port = Integer.parseInt(httpPort);
				}
				catch (Exception ex)
				{
					ex.printStackTrace();
				}
			HttpFileSystemConfigBuilder.getInstance().setProxyPort(opts, port);
		}
		
		///JarFileProvider jfp=new JarFileProvider();
		//DefaultLocalFileProvider dfp=new DefaultLocalFileProvider();
		//dfp.init();
		try{
		//File yajswjar=new File("C:/Users/Administrator/git/yajsw-maven-mk2/maven/yajsw-maven-parent/yajsw-core/target/yajsw-core-0.0.1-distribution.jar");
		//FileObject jfn=dfp.findLocalFile(yajswjar);
		//JarFileObject jfo=new JarFileObject(jfn.getName(),new ZipEntry("/"));
		//FileObject rfpfo=fsManager.createVirtualFileSystem("res:///");
		
		
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	
	}

	public static FileObject resolveFile(String base, String file) throws FileSystemException
	{
		init();
		FileObject basef = null, basefUp =null;
		if (base != null)
		{
			basef = fsManager.resolveFile(new File("."), base);
			basefUp=fsManager.resolveFile(new File(yajswBase),base);
		}
		
		FileObject result=resolveFile(new FileObject[]{basef,basefUp}, file);
		
		return result;

	}
	private static boolean isReal(FileObject fo) throws FileSystemException
	{
		return fo!=null && fo.getType()!=FileType.IMAGINARY;
	}
	private static FileObject realest(FileObject[] fo) throws FileSystemException
	{
		FileObject realest=null;
		for (FileObject cand: fo)
		{
			if (cand!=null)
			{
				if (realest==null)
				{
					realest=cand;;
				}
				else
				{
					if (cand.getType()!=FileType.IMAGINARY)
					{
						realest=cand;
						break;
					}
				}
			}
		}
		return realest;
	}
	public static FileObject resolveFile(FileObject basef, String file) throws FileSystemException
	{
		return resolveFile(new FileObject[]{basef},file);
	}
	public static FileObject resolveFile(FileObject[] baseflist, String file) throws FileSystemException
	{
		init();
		FileObject result=null;
		for (FileObject basef: baseflist)
		{
			FileObject resultTemp = resolveFileFinal(basef, file);
			if (isReal(resultTemp))
			{
				result=resultTemp;
				break;
			
			}
			else if (result==null)
			{
				result=resultTemp;
			}
		}
		
		if (!isReal(result) && resolveRes)
		{
			FileObject resultRes=res.resolveFile(file);
			if (result==null || isReal(resultRes)) result=resultRes;
		}

		return result;
	}

	private static FileObject resolveFileFinal(FileObject basef, String file)
			throws FileSystemException {
		FileObject result;
		if (basef != null)
		{
			result=fsManager.resolveFile(basef, file, opts);
		}
		else
		{
			result=fsManager.resolveFile(file, opts);
		}
		return result;
	}

}
