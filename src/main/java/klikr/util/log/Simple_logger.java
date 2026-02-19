// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.util.log;

//*******************************************************
public class Simple_logger implements Logger
//*******************************************************
{

	@Override
	public void log(boolean also_System_out_println, String s) {
		log(s);
	}

	//*******************************************************
	@Override
	public void log( String s)
	//*******************************************************
	{
		System.out.println(s);
	}

	@Override
	public void log_stack_trace(String s) {
		Logger.super.log_stack_trace(s);
	}

	@Override
	public void log_exception(String header, Exception e) {
		Logger.super.log_exception(header, e);
	}

}
