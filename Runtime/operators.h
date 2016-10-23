#pragma once

#include "print.h"
#include "arrowOperators.h"

// Unwraps Lvalues to their raw value for operations
void _unwrap() {
	Value* value = stack->pop();
	if (!value->isLvalue()) {
		stack->push(value);
	} else {
		Value* unwraped = value->lvalue();
		stack->push(unwraped);
		unwraped->release();
	}
	value->release();
}
