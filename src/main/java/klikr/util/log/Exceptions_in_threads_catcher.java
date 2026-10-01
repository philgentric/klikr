// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.util.log;

//**********************************************************
public class Exceptions_in_threads_catcher
//**********************************************************
{
	public static volatile int oops = 0;

	//**********************************************************
	public static void set_exceptions_in_threads_catcher(Logger logger)
	//**********************************************************
	{

		Thread.setDefaultUncaughtExceptionHandler((thread, e) ->
		{
			oops++;
			String thread_name = (thread == null) ? "<null thread, should not happen>" : thread.getName();

			String trace = Logger.error+Logger.error+Logger.error+" THREAD PANIC:"+thread_name;

			logger.log_with_stack_trace_from_throwable(trace,e);
			if ( thread == null)
			{
				logger.log(" thread == null, Should not happen");
			}
			else
			{
				logger.log("PLEASE REPORT: "+Stack_trace_getter.get_stack_trace(thread.getName() + " occurred here"));
			}
        });

		//logger.log(Stack_trace_getter.get_stack_trace("Exceptions_in_threads_catcher initialized"));
	}

	/*
	 * unit test: crash with array end overrun
	 */

	//**********************************************************
	public static void main(String[] args)
	//**********************************************************
	{
		Logger l = new Simple_logger();
		set_exceptions_in_threads_catcher(l);
		
		try {
			Thread.sleep(10);
		} catch (InterruptedException e) {
			l.log_with_stack_trace_from_throwable("unit test",e);
		}
		
		overrun();
		overrun();
	}

	//**********************************************************
	static void overrun()
	//**********************************************************
	{
		int[] deb = new int[10];
		
		int a = deb[10]; // intended to crash
		
		System.out.println("a="+a);
		
	}
}
