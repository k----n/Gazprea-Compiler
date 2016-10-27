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
