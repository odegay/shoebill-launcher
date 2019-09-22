package net.gtaun.shoebill;

import java.io.*;
/*
import java.io.BufferedReader;
import java.io.File;
import java.io.FilenameFilter;
import java.io.InputStreamReader;
import java.io.InputStream;*/
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
//import net.gtaun.shoebill.ShoebillImpl;
//import net.gtaun.shoebill.*;

public class ShoebillLauncher
{
	private static final String PROPERTY_JAR_FILES = "jarFiles";
	
	private static final String SHOEBILL_PATH = "shoebill/";
	private static final String BOOTSTRAP_FOLDER_NAME = "bootstrap/";

	private static final FilenameFilter JAR_FILENAME_FILTER = (dir, name) -> name.endsWith(".jar");

	
	public static void loadNativeLibrary() throws ClassNotFoundException, SecurityException, IllegalArgumentException
	{
		ClassLoader.getSystemClassLoader().loadClass("net.gtaun.shoebill.SampNativeFunction");
		SampNativeFunction.loadLibrary();
	}
	
	public static Object resolveDependencies() throws Throwable
	{
		File folder = new File(SHOEBILL_PATH, BOOTSTRAP_FOLDER_NAME);
		
		try(URLClassLoader classLoader = createUrlClassLoader(folder.listFiles(JAR_FILENAME_FILTER), null))
		{
			BufferedReader reader = new BufferedReader(new InputStreamReader(classLoader.getResourceAsStream("dependencyManagerImpl.txt")));
			String implClass = reader.readLine();
			reader.close();

			implClass = "net.gtaun.shoebill.dependency.ShoebillDependencyManager";
			Class<?> clz = classLoader.loadClass(implClass);
			Method method = clz.getMethod("resolveDependencies");

			try
			{
				return method.invoke(null);
			}
			catch (InvocationTargetException e)
			{
				throw e.getTargetException();
			}
		}
	}

	@SuppressWarnings("unchecked")
	public static Object createShoebill(Object context, int[] amxHandles) throws Throwable
	{
		Map<String, Object> properties = Map.class.cast(context);
		List<File> files = List.class.cast(properties.get(PROPERTY_JAR_FILES));
		assert files != null;
		URLClassLoader classLoader = createUrlClassLoader(files.toArray(new File[files.size()]), ClassLoader.getSystemClassLoader());
		
//		System.out.println(classLoader.toString());
		//DEBUG
		String implClass = "net.gtaun.shoebill.ShoebillImpl";
		//ShoebillImpl test = new ShoebillImpl(amxHandles);
		//DEBUG

/*		try
		{			
			InputStream inputStream = classLoader.getResourceAsStream("shoebillImpl.txt");	
			System.out.println("1 passed");
			InputStreamReader streamReader = new InputStreamReader(inputStream);
			System.out.println("2 passed");
			BufferedReader reader = new BufferedReader(streamReader);
			System.out.println("3 passed");
			implClass = reader.readLine();
			reader.close();
		}
		catch (Exception e)
		{
			System.out.println("Launcher Error: Some issued with shoebilliml.txt");
			throw e;
		}*/
				
		Class<?> clz = classLoader.loadClass(implClass);
		
		
		try
		{
			Constructor<?> constructor = clz.getConstructor(int[].class);
			return constructor.newInstance((Object) amxHandles);
		}
		catch (NoSuchMethodException e)
		{
			System.out.println("Launcher Error: Can't find shoebill constructor, maybe the Shoebill library is outdated.");
			throw e;
		}
		catch (InvocationTargetException e)
		{
			throw e.getTargetException();
		}
		
	}
	
	private static URLClassLoader createUrlClassLoader(File[] files, ClassLoader parent)
	{
		List<URL> urls = new ArrayList<>();
		
		for(File file : files)
		{
			try
			{
				URL url = file.toURI().toURL();
				urls.add(url);
			}
			catch (MalformedURLException e)
			{
				e.printStackTrace();
			}
		}
		
		URL[] urlArray = urls.toArray(new URL[urls.size()]);
		return URLClassLoader.newInstance(urlArray, parent);
	}
}
