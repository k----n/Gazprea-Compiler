#pragma once

#include "promotion.h"

#include "print.h"
#include "arrowOperators.h"
#include "addition.h"
#include "subtraction.h"
#include "multiplication.h"
#include "division.h"
#include "modulus.h"
#include "exponent.h"
#include "negation.h"
#include "lessthan.h"
#include "greaterthan.h"
#include "lessthanequal.h"
#include "greaterthanequal.h"
#include "equals.h"
#include "notequals.h"
#include "logicalor.h"
#include "logicalxor.h"
#include "logicaland.h"
#include "by.h"

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

bool toBool() {
	Value* value = stack->pop();
	bool* returnValuePtr = value->booleanValue();
	bool returnValue = *returnValuePtr;
	delete returnValuePtr;
	value->release();
	return returnValue;
}
