#pragma once

void push(void** variable) {
	CalculatorValue value = CalculatorValue(new Type(Lvalue), variable);
	stack->push(&value);
}

void assign(void** variable) {
	CalculatorValue** var = (CalculatorValue**)variable;
	CalculatorValue* rvalue = stack->pop();
	
	// Transform the rvalue to the default value when using `null`
	if (rvalue->isNull()) {
		delete rvalue;
		switch ((*var)->getType()->getType()) {
			case NullType:
			case IdentityType:
				printf("Cannot determine type");
				exit(1);
				break;
			case IntegerType:
				rvalue = new CalculatorValue(0);
				break;
			case StandardOut:
				rvalue = new CalculatorValue(new Type(StandardOut), nullptr);
				break;
			case StandardIn:
				rvalue = new CalculatorValue(new Type(StandardIn), nullptr);
				break;
			case Lvalue:
				printf("Cannot lvalue");
				exit(1);
				break;
		}
	}
	if (rvalue->isIdentity()) {
		delete rvalue;
		switch ((*var)->getType()->getType()) {
			case NullType:
			case IdentityType:
				printf("Cannot determine type");
				exit(1);
				break;
			case IntegerType:
				rvalue = new CalculatorValue(1);
				break;
			case StandardOut:
				rvalue = new CalculatorValue(new Type(StandardOut), nullptr);
				break;
			case StandardIn:
				rvalue = new CalculatorValue(new Type(StandardIn), nullptr);
				break;
			case Lvalue:
				printf("Cannot lvalue");
				exit(1);
				break;
		}
	}
	
	// TODO: PROMOTION
	
	if (*var != nullptr) { delete *var; }
	*var = rvalue;
}
