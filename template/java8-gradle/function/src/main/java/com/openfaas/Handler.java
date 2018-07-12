package com.openfaas;

public class Handler implements FunctionService {

	@Override
	public String handle(String request) {
		return "Hello world!";
	}
}
