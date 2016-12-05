#include "nullptr.h"

#include "declarations.h"
#include "literals.h"
#include "builtins.h"
#include "operators.h"
#include "variables.h"
#include "vectors.h"
#include "noop.h"

#include "VectorImp.h"

#ifdef LLVM_BUILD
asm("REPLACE_ME-GLOBAL_VARIABLES");
asm("REPLACE_ME-FUNCTIONS");
asm("REPLACE_ME-STRUCTS");
#endif

Stack<Value>* stack;

int main(int argc, const char * argv[]) {
	stack = new Stack<Value>();
	
#ifdef LLVM_BUILD
	asm("REPLACE_ME-GLOBAL_INITS");
	asm("REPLACE_ME-CALL_DEFINED_MAIN");
#endif

#ifndef LLVM_BUILD
	
	void* t1 = nullptr;
	
	pushStartVector();
	pushStartVector();
	pushInteger(0);
	pushInteger(1);
	pushInteger(2);
	endVector();
	pushStartVector();
	pushInteger(3);
	pushInteger(4);
	pushInteger(5);
	endVector();
	endMatrix();
	
	assign(&t1);
	
	push(&t1);
	columns();
	std_output();
	rightArrowOperator();
	
	pushInteger(0); // Push return code
#endif
	
	// Pop return value from the stack
	Value* value = stack->pop();
	int* returnValue_ptr = value->integerValue();
	int returnValue = *returnValue_ptr;
	delete returnValue_ptr;
	value->release();
	
	// Release Globals
	stack->release();
	
	return returnValue;
}
