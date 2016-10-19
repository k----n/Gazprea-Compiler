#pragma once

#ifdef LLVM_BUILD
asm("REPLACE_ME-GLOBAL_VARIABLES");

asm("REPLACE_ME-FUNCTIONS");
#endif

int main(int argc, const char * argv[]) {
	#ifdef LLVM_BUILD
	asm("REPLACE_ME-CALL_DEFINED_MAIN");
	#else
	
	pushNull();
	void* a = nullptr;
	varInitPushNullInteger();
	assign(&a);
	assign(&a);
	push(&a);
	std_output();
	rightArrowOperator();
	
	/*
	 call void @_Z8pushNullv()
	 %GazVar_x_3 = alloca i8*, align 8
	 store i8* null, i8** %GazVar_x_3, align 8
	 call void @_Z22varInitPushNullIntegerv()
	 call void @_Z6assignPPv(i8** %GazVar_x_3)
	 call void @_Z6assignPPv(i8** %GazVar_x_3)
	 call void @_Z4pushPPv(i8** %GazVar_x_3)
	 call void @_Z10std_outputv()
	 call void @_Z18rightArrowOperatorv()
	 call void @_Z11pushIntegeri(i32 0)
	 ret void
	 */
	
	
	// Boilerplate included in Gazprea
	pushInteger(0);
	#endif
	
	// Pop return value from the stack
	CalculatorValue* value = stack->pop();
	int returnValue = *value->integerValue();
	delete value;
	
	// Clear Globals
	delete stack;
	
	return returnValue;
}
