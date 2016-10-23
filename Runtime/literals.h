#pragma once

#include "BuiltinType.h"

void pushEmptyValue(BuiltinType builtinType) {
	ValueType* type = new ValueType(builtinType);
	Value* value = new Value(type, nullptr);
	stack->push(value);
	type->release();
	value->release();
}

void pushNull()		{ pushEmptyValue(NullType);		}
void pushIdentity()	{ pushEmptyValue(IdentityType);	}

void pushInteger(int value) {
	Value* integerValue = new Value(value);
	stack->push(integerValue);
	integerValue->release();
}

void varInitPushNullInteger() {
	pushInteger(0);
}
