#pragma once

void push(void** variable) {
	ValueType* type = new ValueType(Lvalue);
	Value* value = new Value(type, variable);
	stack->push(value);
	type->release();
	value->release();
}

void pushVariableValue(void** variable) {
	Value* value = *(Value**)variable;
	stack->push(value);
	value->release();
}

void assign(void** variable) {
	Value** var = (Value**)variable;
	Value* rvalue = stack->pop();
	
	// Transform the rvalue to the default value when using `null`
	if (rvalue->isNull()) {
		rvalue->release();
		ValueType* rType = nullptr;
		switch ((*var)->getType()->getType()) {
			case NullType:
			case IdentityType:
				printf("Cannot determine type");
				exit(1);
				break;
			case IntegerType:
				rvalue = new Value(0);
				break;
			case StandardOut:
				rType = new ValueType(StandardOut);
				rvalue = new Value(rType, nullptr);
				rType->release();
				break;
			case StandardIn:
				rType = new ValueType(StandardIn);
				rvalue = new Value(rType, nullptr);
				rType->release();
				break;
			case Lvalue:
				printf("Cannot lvalue");
				exit(1);
				break;
		}
	}
	// Transform the rvalue to the default value when using `identity`
	if (rvalue->isIdentity()) {
		rvalue->release();
		ValueType* rType = nullptr;
		switch ((*var)->getType()->getType()) {
			case NullType:
			case IdentityType:
				printf("Cannot determine type");
				exit(1);
				break;
			case IntegerType:
				rvalue = new Value(1);
				break;
			case StandardOut:
				rType = new ValueType(StandardOut);
				rvalue = new Value(rType, nullptr);
				rType->release();
				break;
			case StandardIn:
				rType = new ValueType(StandardIn);
				rvalue = new Value(rType, nullptr);
				rType->release();
				break;
			case Lvalue:
				printf("Cannot lvalue");
				exit(1);
				break;
		}
	}
	
	// TODO: PROMOTION
	
	if (*var != nullptr) { (*var)->release(); }
	if (rvalue->isLvalue()) {
		Value* newValue = rvalue->lvalue();
		newValue->retain();
		*var = newValue;
	} else {
		*var = rvalue;
	}
}
