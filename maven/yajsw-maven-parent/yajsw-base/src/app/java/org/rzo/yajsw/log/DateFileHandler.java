/* This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * <p/>
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.  
 */
package org.rzo.yajsw.log;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import org.rzo.yajsw.log.MyFileHandler.FileChangeListner;

// TODO: Auto-generated Javadoc
/**
 * The Class DateFileHandler.
 */
public class DateFileHandler extends Handler
{

	/** The _handler. */
	volatile MyFileHandler			_handler;

	/** The _end date. */
	volatile long				_endDate;

	/** The _pattern. */
	volatile String				_pattern;

	/** The _limit. */
	volatile int					_limit;

	/** The _count. */
	volatile int					_count;

	/** The _append. */
	volatile boolean				_append;

	/** The format. */
	final SimpleDateFormat	format	= new SimpleDateFormat("yyyyMMdd");

	/** The _init. */
	volatile boolean				_init	= false;
	
	volatile boolean _rollDate = false;
	
	volatile long _startDate = System.currentTimeMillis();
	
	volatile LinkedList<File> _previousFiles = new LinkedList<File>();
	volatile LinkedList<File> _currentFiles = new LinkedList<File>();

	/**
	 * Instantiates a new date file handler.
	 * 
	 * @param pattern
	 *            the pattern
	 * @param limit
	 *            the limit
	 * @param count
	 *            the count
	 * @param append
	 *            the append
	 * 
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 * @throws SecurityException
	 *             the security exception
	 */
	public DateFileHandler(String pattern, int limit, int count, boolean append, boolean rollDate, PatternFormatter fileFormatter, Level logLevel, String encoding)
	{
		_pattern = pattern;
		_limit = limit;
		_count = count;
		_append = append;
		_rollDate = rollDate;
		_init = true;
		if (encoding != null)
			try
			{
				setEncoding(encoding);
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		setFormatter(fileFormatter);
		setLevel(logLevel);
		rotateDate();
		checkFileCount();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.util.logging.Handler#close()
	 */
	@Override
	public void close() throws SecurityException
	{
		_handler.close();

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.util.logging.Handler#flush()
	 */
	@Override
	public void flush()
	{
		_handler.flush();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.util.logging.Handler#publish(java.util.logging.LogRecord)
	 */
	@Override
	public void publish(LogRecord record)
	{
		if (_rollDate)
		{
		if (_endDate < record.getMillis())
			rotateDate();
		if (System.currentTimeMillis() - _startDate > 25*60*60*1000)
		{
			String msg = record.getMessage();
			record.setMessage("missed file rolling at: "+new Date(_endDate)+"\n"+msg);
		}
		}
		_handler.publish(record);

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.util.logging.Handler#setFormatter(java.util.logging.Formatter)
	 */
	@Override
	public void setFormatter(Formatter newFormatter)
	{
		super.setFormatter(newFormatter);
		if (_handler != null)
			_handler.setFormatter(newFormatter);
	}

	/**
	 * Rotate date.
	 */
	private void rotateDate()
	{
		_startDate = System.currentTimeMillis();
		if (_handler != null)
			_handler.close();
		String pattern = _pattern.replace("%d", format.format(new Date()));
		Calendar next = Calendar.getInstance(); // current date
		// begin of next date
		next.set(Calendar.HOUR_OF_DAY, 0);
		next.set(Calendar.MINUTE, 0);
		next.set(Calendar.SECOND, 0);
		next.set(Calendar.MILLISECOND, 0);
		next.add(Calendar.DATE, 1);
		_endDate = next.getTimeInMillis();

		try
		{
			_handler = new MyFileHandler(pattern, _limit, _count, _append);
			if (_init)
			{
				_handler.setEncoding(this.getEncoding());
				_handler.setErrorManager(this.getErrorManager());
				_handler.setFilter(this.getFilter());
				_handler.setFormatter(this.getFormatter());
				_handler.setLevel(this.getLevel());
				findFiles();
				_currentFiles.clear();
				addFiles(_handler.getCurrentFiles());
				_handler.setNewFileListner(new FileChangeListner()
				{

					public void fileChange(File file, boolean added)
					{
						System.out.println("file change: "+added+ " "+file.getName());
						if (added)
							addFile(file);
					}
					
				});
				
			}
		}
		catch (SecurityException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void checkFileCount()
	{
		while (_previousFiles.size() > 0 && _previousFiles.size()+_currentFiles.size()>_count)
		{
			File f = _previousFiles.removeLast();
			if (f.exists())
				f.delete();
		}
	}
	
	private void addFile(File f)
	{
		if (!_currentFiles.contains(f))
			_currentFiles.addFirst(f);
		checkFileCount();
	}
	
	private void addFiles(LinkedList<File> files)
	{
		while (!files.isEmpty())
			addFile(files.removeLast());			
	}
	
	private void findFiles() throws IOException
	{
		Calendar date = Calendar.getInstance();
		while (_previousFiles.size() < _count)
		{
			date.add(Calendar.DAY_OF_MONTH, -1); 
			String pattern = _pattern.replace("%d", format.format(date.getTime()));
			for (int unique=0; unique<_count; unique++)
			{
				for (int generation=0; generation<_count; generation++)
				{
				File f = MyFileHandler.generate(pattern, generation, unique, _count);
				if (f.exists() && !_previousFiles.contains(f))
				{
					_previousFiles.addLast(f);
				}
				else
				{
					break;
				}
				}
				File f = MyFileHandler.generate(pattern, 0, unique, _count);
				if (!f.exists() || _previousFiles.contains(f))
					break;
			}
			File f = MyFileHandler.generate(pattern, 0, 0, _count);
			if (!f.exists())
				break;
		}

	}

}
