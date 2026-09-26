// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT


package klikr.util.log;

import java.io.*;

//**********************************************************
public interface Logger
//**********************************************************
{
	public static final String error ="❌";
	public static final String ok = "✅";
	public static final String warning ="⚠️";
	//blue diamond
	public static final String info ="\uD83D\uDD37";

	void log(boolean also_System_out_println, String s);

	//*******************************************************
	default void log(String s)
	//*******************************************************
	{
		log(true,s);
	}

	//*******************************************************
	default void log_with_stack_trace(String s)
	//*******************************************************
	{
		log(Stack_trace_getter.get_stack_trace(s));
	}



	//*******************************************************
	default void log_with_stack_trace_from_throwable(String header, Throwable e)
	//*******************************************************
	{
		log(header+Stack_trace_getter.get_stack_trace_for_throwable(e));
	}

}
